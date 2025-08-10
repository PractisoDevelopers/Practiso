package opacity.client

import com.zhufucdev.practiso.datamodel.InstantIsoSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ArchiveMetadata(
    val id: String,
    val name: String,
    val downloads: Int,
    val likes: Int,
    val ownerName: String?,
    @Serializable(InstantIsoSerializer::class)
    val uploadTime: Instant,
    @Serializable(InstantIsoSerializer::class)
    val updateTime: Instant,
    val dimensions: List<DimensionMetadata>,
)
