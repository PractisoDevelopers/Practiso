package opacity.client

data class BonjourResponse(
    val version: CompatibilityVersion,
    val buildDate: BuildDate,
    val others: List<GenericBonjourProperty>,
)