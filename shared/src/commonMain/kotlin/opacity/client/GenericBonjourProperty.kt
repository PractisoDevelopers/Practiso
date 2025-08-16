package opacity.client

data class GenericBonjourProperty(override val key: String, override val value: String) :
    BonjourProperty<String>