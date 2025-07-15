package opacity.client

import kotlinx.serialization.Serializable

@Serializable
data class DimensionMetadata(val name: String, val emoji: String?, val quizCount: Int)
