package opacity.client

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

sealed class ArchiveUploadState {
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Success(@JsonNames("jwt") val authToken: String? = null, val archiveId: String) :
        ArchiveUploadState()

    data class Failure(val statusCode: HttpStatusCode, val message: String?) : ArchiveUploadState()
    data class InProgress(val stats: StateFlow<TransferStats>) : ArchiveUploadState()
}