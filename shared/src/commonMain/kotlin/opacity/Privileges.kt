package opacity

class Privileges(var matrix: Int = 0b1011) {
    val user = Primitive(0)
    val others = Primitive(2)

    inner class Primitive(val offset: Int) {
        var read: Boolean
            get() = 0b10 and (matrix shr offset) == 0b10
            set(value) = set(value, 0b10)

        var write: Boolean
            get() = 0b01 and (matrix shr offset) == 0b01
            set(value) = set(value, 0b01)

        private fun set(enabled: Boolean, template: Int) {
            matrix = if (enabled) {
                matrix or (template shl offset)
            } else {
                matrix and (template.inv() shl offset)
            }
        }
    }
}