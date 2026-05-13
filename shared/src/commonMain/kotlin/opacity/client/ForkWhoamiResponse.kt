package opacity.client

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ForkWhoamiResponse(@JsonNames("jwt") val authToken: String) {
}