package opacity.client

data class CompatibilityVersion(override val value: Int) : BonjourProperty<Int> {
    override val key: String get() = KEY
    companion object {
        const val KEY = "version"
    }
}
