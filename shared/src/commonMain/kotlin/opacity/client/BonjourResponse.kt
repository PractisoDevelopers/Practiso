package opacity.client

data class BonjourResponse(
    val version: CompatibilityVersion,
    val buildDate: BuildDate,
    val maxNameLength: MaxNameLength? = null,
    val others: List<GenericBonjourProperty>,
)