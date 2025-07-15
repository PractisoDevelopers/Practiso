package opacity.client

import kotlinx.serialization.Serializable

@Serializable
data class GetArchivesResponse(val page: List<ArchiveMetadata>, val next: String? = null) {
    val lastPage: Boolean get() = next == null
}