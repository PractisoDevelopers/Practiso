package opacity.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveMetadata(
    val id: String,
    val name: String,
    val ownerName: String?,
    val uploadTime: Instant,
    val updateTime: Instant,
    val dimensions: List<DimensionMetadata>,
)
