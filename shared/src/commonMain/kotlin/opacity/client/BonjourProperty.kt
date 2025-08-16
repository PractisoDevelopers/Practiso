package opacity.client

sealed interface BonjourProperty<T> {
    val key: String
    val value: T
}
