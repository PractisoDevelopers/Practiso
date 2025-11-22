package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.DEFAULT_COMMUNITY_SERVER_URL
import com.zhufucdev.practiso.datamodel.ArchiveHandle
import com.zhufucdev.practiso.datamodel.NextPointerBasedPaginated
import com.zhufucdev.practiso.datamodel.Paginated
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.io.Source
import opacity.client.ArchiveMetadata
import opacity.client.ArchivePreview
import opacity.client.ArchiveUploadState
import opacity.client.AuthorizationException
import opacity.client.BonjourResponse
import opacity.client.DimensionMetadata
import opacity.client.OpacityClient
import opacity.client.SortOptions
import opacity.client.TransferStats
import opacity.client.Whoami

class CommunityService(
    endpoint: String = DEFAULT_COMMUNITY_SERVER_URL,
    val identity: CommunityIdentity,
) {
    private val clientScope = CoroutineScope(Dispatchers.Default)
    private val client = identity.authToken.map { authToken ->
        OpacityClient(
            endpoint,
            authToken,
            PlatformHttpClientFactory
        )
    }
        .shareIn(clientScope, started = SharingStarted.Eagerly, replay = 1)

    fun getArchivePagination(sortOptions: SortOptions = SortOptions()): Paginated<ArchiveMetadata> =
        object : NextPointerBasedPaginated<ArchiveMetadata, String>() {
            override suspend fun getFirstPage(): Pair<List<ArchiveMetadata>, String?> {
                return client.first().getArchiveList(sortOptions).let { it.page to it.next }
            }

            override suspend fun getFollowingPages(priorPointer: String): Pair<List<ArchiveMetadata>, String?> {
                return client.first().getArchiveList(sortOptions, priorPointer)
                    .let { it.page to it.next }
            }
        }

    fun getDimensionArchivePagination(
        dimensionName: String,
        sortOptions: SortOptions = SortOptions(),
    ): Paginated<ArchiveMetadata> =
        object : NextPointerBasedPaginated<ArchiveMetadata, String>() {
            override suspend fun getFirstPage(): Pair<List<ArchiveMetadata>, String?> {
                return client
                    .first()
                    .getDimensionArchiveList(dimensionName, sortOptions)
                    .let { it.page to it.next }
            }

            override suspend fun getFollowingPages(priorPointer: String): Pair<List<ArchiveMetadata>, String?> {
                return client.first()
                    .getDimensionArchiveList(dimensionName, sortOptions, priorPointer)
                    .let { it.page to it.next }
            }
        }


    fun ArchiveMetadata.toHandle(): ArchiveHandle = ArchiveHandle(this, client)

    suspend fun getDimensions(takeFirst: Int = 20): List<DimensionMetadata> =
        client.first().getDimensionList(takeFirst)

    suspend fun getArchivePreview(archiveId: String): List<ArchivePreview> =
        client.first().getArchivePreview(archiveId)

    suspend fun getArchiveMetadata(archiveId: String): ArchiveMetadata? =
        client.first().getArchiveMetadata(archiveId)

    suspend fun getServerInfo(): BonjourResponse = client.first().getBonjour()

    suspend fun getWhoami(): Whoami? = client.first().getWhoami()

    suspend fun deleteArchive(archiveId: String) =
        client.first().deleteArchive(archiveId)

    private fun uploadArchiveImpl(
        content: Source,
        contentName: String? = null,
        clientName: String? = null,
        ownerName: String? = null,
    ): Flow<UploadArchive> = flow {
        var archiveName = contentName
        if (archiveName == null) {
            val duplexSubmissionChannel = Channel<String>()
            emit(UploadArchive.ArchiveNameRequired(duplexSubmissionChannel))
            archiveName = duplexSubmissionChannel.receive()
        }
        emitAll(
            client.first().uploadArchive(
                content.peek(),
                archiveName,
                clientName,
                ownerName,
            )
                .transform {
                    when (it) {
                        is ArchiveUploadState.Success -> {
                            if (it.authToken != null) {
                                identity.authToken.tryEmit(it.authToken)
                            }
                            emit(UploadArchive.Success(it.archiveId))
                        }

                        is ArchiveUploadState.Failure -> when {
                            it.statusCode == HttpStatusCode.Unauthorized && identity.authToken.first() == null -> {
                                val duplexSubmissionChannel = Channel<CommunityRegistrationInfo>()
                                emit(UploadArchive.RegistrationRequired(duplexSubmissionChannel))
                                val info = duplexSubmissionChannel.receive()
                                emitAll(
                                    uploadArchiveImpl(
                                        content.peek(),
                                        archiveName,
                                        info.clientName,
                                        info.ownerName
                                    )
                                )
                            }

                            it.statusCode == HttpStatusCode.Forbidden && identity.authToken.first() != null -> {
                                val duplexProceedChannel = Channel<Unit>()
                                emit(
                                    UploadArchive.SignOffRequired(
                                        serverMessage = it.message,
                                        proceed = duplexProceedChannel
                                    )
                                )
                                duplexProceedChannel.receive()
                                identity.clear()
                                emitAll(uploadArchiveImpl(content.peek(), archiveName))
                            }

                            else -> emit(UploadArchive.Failure(it.statusCode, it.message))
                        }

                        is ArchiveUploadState.InProgress -> {
                            emit(UploadArchive.InProgress(it.stats))
                        }
                    }
                }
        )
    }

    fun uploadArchive(content: Source, contentName: String? = null) =
        uploadArchiveImpl(
            content = content,
            contentName = contentName,
        )

    @Throws(AuthorizationException::class, IllegalStateException::class)
    suspend fun like(archiveId: String) = client.first().like(archiveId)

    @Throws(AuthorizationException::class, IllegalStateException::class)
    suspend fun removeLike(archiveId: String) =
        client.first().removeLike(archiveId)
}

sealed class UploadArchive {
    data class Success(val archiveId: String) : UploadArchive()
    data class RegistrationRequired(val submission: SendChannel<CommunityRegistrationInfo>) :
        UploadArchive()

    data class ArchiveNameRequired(val submission: SendChannel<String>) : UploadArchive()
    data class SignOffRequired(val serverMessage: String?, val proceed: SendChannel<Unit>) :
        UploadArchive()

    data class InProgress(val stats: StateFlow<TransferStats>) : UploadArchive()
    data class Failure(val statusCode: HttpStatusCode, val message: String?) : UploadArchive()
}

data class CommunityRegistrationInfo(val clientName: String, val ownerName: String)

interface CommunityIdentity {
    val authToken: MutableStateFlow<String?>
    fun clear()
}