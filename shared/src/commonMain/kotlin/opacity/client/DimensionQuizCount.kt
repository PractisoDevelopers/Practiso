package opacity.client

import kotlinx.serialization.Serializable

@Serializable
data class DimensionQuizCount(val name: String, val quizCount: Int, val emoji: String?)
