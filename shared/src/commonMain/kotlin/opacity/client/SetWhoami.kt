package opacity.client

import opacity.SetField

data class SetWhoami(
    val name: SetField<String?>,
    val clientName: SetField<String>
)