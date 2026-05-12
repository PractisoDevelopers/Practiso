package opacity

sealed interface SetField<T> {
    data class Update<T>(val value: T) : SetField<T>
    class Unchanged<T> : SetField<T>
}