package opacity.client

import kotlinx.serialization.Serializable

@Serializable
data class ArchivePreview(val name: String, val body: String)