package opacity.client

import kotlinx.datetime.LocalDate

data class BuildDate(
    override val value: LocalDate,
) : BonjourProperty<LocalDate> {
    override val key: String get() = KEY
    companion object {
        const val KEY = "build_date"
    }
}
