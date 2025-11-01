package opacity.client

import kotlinx.serialization.Serializable

@Serializable
data class  Whoami(
    val clientName: String,
    val name: String? = null,
    val ownerId: Int,
    val mode: Int = 0b1011
)
