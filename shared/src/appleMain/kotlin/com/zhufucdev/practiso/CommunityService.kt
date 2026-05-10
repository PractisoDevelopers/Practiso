package com.zhufucdev.practiso

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.coroutines.getStringFlow
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
import io.ktor.utils.io.CancellationException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import opacity.client.ArchiveMetadata
import opacity.client.ArchivePreview
import opacity.client.DimensionMetadata
import opacity.client.SortOptions
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrServer
import platform.Security.kSecClass
import platform.Security.kSecClassInternetPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecValueData
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val COMMUNITY_ACCOUNT = "community"

class KeychainError(val status: Int) : IllegalStateException("unexpected Keychain status: $status")

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
class KeychainCommunityIdentity(private val endpoint: String) : CommunityIdentity {
    override val authToken: StateFlow<AuthorizationToken?> = MutableStateFlow(run {
        memScoped {
            withSecQuery { parameters ->
                CFDictionaryAddValue(parameters, kSecMatchLimit, kSecMatchLimitOne)
                val item = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(parameters, item.ptr)
                if (status == errSecItemNotFound) {
                    null
                } else if (status != errSecSuccess) {
                    throw KeychainError(status)
                } else {
                    val result = item.ptr as CFDictionaryRef
                    val tokenPtr = CFDictionaryGetValue(result, kSecValueData) as CPointer<ByteVar>
                    AuthorizationToken(tokenPtr.toKString())
                }
            }
        }
    })

    @OptIn(ExperimentalContracts::class)
    private fun <T> MemScope.withSecQuery(block: (CFMutableDictionaryRef) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val parameters = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(parameters, kSecClass, kSecClassInternetPassword)
        CFDictionaryAddValue(
            parameters,
            kSecAttrAccount,
            CFBridgingRetain(NSData(COMMUNITY_ACCOUNT))
        )
        CFDictionaryAddValue(
            parameters,
            kSecAttrServer,
            CFBridgingRetain(NSData(endpoint))
        )
        val result = block(parameters!!)
        CFBridgingRelease(parameters)
        return result
    }

    override fun setAuthToken(value: AuthorizationToken) {
        val status = memScoped {
            withSecQuery { parameters ->
                CFDictionaryAddValue(
                    parameters,
                    kSecValueData,
                    CFBridgingRetain(NSData(value.toString()))
                )
                SecItemAdd(parameters, null)
            }
        }
        if (status != errSecSuccess) {
            throw KeychainError(status)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear() {
        memScoped {
            withSecQuery(::SecItemDelete)
        }
    }
}

private const val COMMUNITY_SERVER_URL_KEY = "CommunityServerURL"

@OptIn(ExperimentalSettingsApi::class, ExperimentalCoroutinesApi::class)
object AppCommunityService {
    private val settings = NSUserDefaultsSettings(NSUserDefaults())
    private val community =
        settings.getStringFlow(COMMUNITY_SERVER_URL_KEY, DEFAULT_COMMUNITY_SERVER_URL)
            .map { CommunityService(it, KeychainCommunityIdentity(it)) }
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
}