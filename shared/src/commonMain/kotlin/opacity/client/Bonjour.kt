package opacity.client

import kotlinx.datetime.LocalDate

object Bonjour {
    fun parseProperty(text: String): BonjourProperty<*> {
        val split = text.split(':', limit = 2)
        if (split.size != 2) {
            throw IllegalArgumentException("text doesn't represent a Bonjour property")
        }
        val (key, value) = split[0] to split[1]
        return when (key) {
            CompatibilityVersion.KEY -> CompatibilityVersion(value.toInt())
            BuildDate.KEY -> BuildDate(LocalDate.parse(value))
            MaxNameLength.KEY -> MaxNameLength(value.toInt())
            else -> GenericBonjourProperty(key, value)
        }
    }
}