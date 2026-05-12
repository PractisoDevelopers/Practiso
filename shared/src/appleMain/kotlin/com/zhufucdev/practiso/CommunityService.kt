package com.zhufucdev.practiso

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.zhufucdev.practiso.bridge.NamedSource
import com.zhufucdev.practiso.convert.NSData
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.datamodel.DownloadException
import com.zhufucdev.practiso.datamodel.Paginated
import com.zhufucdev.practiso.helper.simpleHandleQuestions
import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityIdentity
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.UploadArchive
import io.github.vinceglb.filekit.utils.toByteArray
import io.ktor.utils.io.CancellationException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.okio.asKotlinxIoRawSource
import opacity.client.ArchiveMetadata
import opacity.client.ArchivePreview
import opacity.client.AuthorizationException
import opacity.client.BonjourResponse
import opacity.client.DimensionMetadata
import opacity.client.HttpStatusAssertionException
import opacity.client.SetWhoami
import opacity.client.SortOptions
import opacity.client.Whoami
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.UTF8String
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessGroup
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrServer
import platform.Security.kSecClass
import platform.Security.kSecClassInternetPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val COMMUNITY_ACCOUNT = "community"

class KeychainError(val status: Int) : IllegalStateException("unexpected Keychain status: $status")

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainCommunityIdentity(
    private val endpoint: String,
    private val keychainGroup: String? = null
) : CommunityIdentity {
    override val authToken: MutableStateFlow<AuthorizationToken?> = MutableStateFlow(run {
        memScoped {
            withSecQuery { parameters ->
                CFDictionaryAddValue(parameters, kSecMatchLimit, kSecMatchLimitOne)
                CFDictionaryAddValue(parameters, kSecReturnData, kCFBooleanTrue)
                val item = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(parameters, item.ptr)
                if (status == errSecItemNotFound) {
                    null
                } else if (status != errSecSuccess) {
                    throw KeychainError(status)
                } else {
                    (CFBridgingRelease(item.value) as? NSData)
                        ?.let {
                            val nss = NSString.create(it, NSUTF8StringEncoding)
                            nss?.UTF8String?.toKString()
                        }
                        ?.let { AuthorizationToken(it) }
                }
            }
        }
    })

    @OptIn(ExperimentalContracts::class)
    private fun <T> withSecQuery(block: (CFMutableDictionaryRef) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val parameters = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(parameters, kSecClass, kSecClassInternetPassword)
        val accountNameRef =
            CFBridgingRetain(NSData(COMMUNITY_ACCOUNT))
        CFDictionaryAddValue(
            parameters,
            kSecAttrAccount,
            accountNameRef
        )
        val endpointRef = CFBridgingRetain(NSData(endpoint))
        CFDictionaryAddValue(
            parameters,
            kSecAttrServer,
            endpointRef
        )
        val keychainGroupRef = keychainGroup?.let { CFBridgingRetain(keychainGroup) }
        if (keychainGroupRef != null) {
            CFDictionaryAddValue(
                parameters,
                kSecAttrAccessGroup,
                keychainGroupRef
            )
        }
        val result = block(parameters!!)
        if (keychainGroupRef != null) {
            CFBridgingRelease(keychainGroupRef)
        }
        CFBridgingRelease(endpointRef)
        CFBridgingRelease(accountNameRef)
        CFBridgingRelease(parameters)
        return result
    }

    override fun setAuthToken(value: AuthorizationToken) {
        val status = memScoped {
            withSecQuery { parameters ->
                val valueRef = CFBridgingRetain(NSData(value.toString()))
                CFDictionaryAddValue(
                    parameters,
                    kSecValueData,
                    valueRef
                )
                val status = SecItemAdd(parameters, null)
                CFBridgingRelease(valueRef)
                status
            }
        }
        if (status != errSecSuccess) {
            throw KeychainError(status)
        }
        authToken.value = value
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear() {
        memScoped {
            withSecQuery(::SecItemDelete)
        }
        authToken.value = null
    }
}

private const val COMMUNITY_SERVER_URL_KEY = "CommunityServerURL"
val APP_KEYCHAIN_SHARING = "${NSBundle.mainBundle.objectForInfoDictionaryKey("AppIdentifierPrefix")}com.zhufucdev.practiso.keychain.shared"

@OptIn(ExperimentalSettingsApi::class, ExperimentalCoroutinesApi::class)
object AppCommunityService {
    private val settings = NSUserDefaultsSettings(NSUserDefaults())
    private val community =
        settings.getStringFlow(COMMUNITY_SERVER_URL_KEY, DEFAULT_COMMUNITY_SERVER_URL)
            .map {
                CommunityService(
                    it,
                    KeychainCommunityIdentity(it, keychainGroup = APP_KEYCHAIN_SHARING)
                )
            }
            .shareIn(
                MainScope(),
                started = SharingStarted.Lazily,
                replay = 1
            )
    private val download =
        settings.getStringFlow(COMMUNITY_SERVER_URL_KEY, DEFAULT_COMMUNITY_SERVER_URL)
            .distinctUntilChanged()
            .map { CoroutineScope(Dispatchers.UniqueIO) }
            .runningReduce { old, new ->
                old.cancel()
                new
            }
            .map(::DownloadManager)
            .shareIn(
                MainScope(),
                started = SharingStarted.Lazily,
                replay = 1
            )

    private val importService = ImportService()

    fun getArchivePreview(archiveId: String): Flow<List<ArchivePreview>> =
        community.flatMapLatest { it.getArchivePreview(archiveId) }

    fun getArchivePagination(sortOptions: SortOptions = SortOptions()): Flow<Paginated<ArchiveMetadata>> =
        community.flatMapLatest { it.getArchivePagination(sortOptions) }

    fun getDimensions(takeFirst: Int = 20): Flow<List<DimensionMetadata>> =
        community.flatMapLatest { it.getDimensions(takeFirst) }

    fun getWhoami(): Flow<Whoami?> = community.flatMapLatest { it.getWhoami() }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun setWhoami(info: SetWhoami) = community.first().setWhoami(info)

    @Throws(HttpStatusAssertionException::class, kotlinx.coroutines.CancellationException::class)
    suspend fun deleteWhoami() = community.first().deleteWhoami()

    fun getServerInfo(): Flow<BonjourResponse> = community.flatMapLatest { it.getServerInfo() }

    fun getArchiveMetadata(archiveId: String): Flow<ArchiveMetadata?> =
        community.flatMapLatest { it.getArchiveMetadata(archiveId) }

    @Throws(DownloadException::class, CancellationException::class)
    suspend fun download(archive: ArchiveMetadata) {
        val handle = with(community.first()) {
            archive.toHandle()
        }
        val downloadMgr = download.first()
        val pack = try {
            handle.getAsSource(downloadMgr)
        } catch (_: CancellationException) {
            // noop
            return
        }
        importService.import(pack).simpleHandleQuestions()
    }

    fun downloadState(of: ArchiveMetadata): Flow<DownloadCycle> =
        community.map { with(it) { of.toHandle() }.taskId }
            .flatMapLatest { taskId ->
                download.flatMapLatest { it[taskId] }
            }

    suspend fun cancelDownload(of: ArchiveMetadata) {
        val handle = with(community.first()) { of.toHandle() }
        val downloadMgr = download.first()
        downloadMgr.cancel(handle.taskId)
        (downloadMgr[handle.taskId].first() as? DownloadState.Completed)
            ?.let {
                getPlatform().filesystem.delete(it.destination)
            }
    }

    fun upload(contentsOf: NSURL, contentName: String? = null): Flow<UploadArchive> {
        val content = NamedSource(contentsOf).source.asKotlinxIoRawSource().buffered()
        return community.flatMapLatest { it.uploadArchive(content, contentName) }
    }

    fun upload(data: NSData, contentName: String? = null): Flow<UploadArchive> {
        val content = Buffer().apply { write(data.toByteArray()) }
        return community.flatMapLatest { it.uploadArchive(content, contentName) }
    }

    @Throws(AuthorizationException::class, IllegalStateException::class)
    suspend fun like(archiveId: String) = community.first().like(archiveId)

    @Throws(AuthorizationException::class, IllegalStateException::class)
    suspend fun removeLike(archiveId: String) = community.first().removeLike(archiveId)

    suspend fun isAuthenticated() = community.first().identity.authToken.firstOrNull() != null

    suspend fun clearIdentity() = community.first().identity.clear()
}