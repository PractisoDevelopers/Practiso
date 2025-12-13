package com.zhufucdev.practiso

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import io.ktor.utils.io.core.readBytes
import kotlinx.io.Buffer
import kotlinx.io.readDouble
import kotlinx.io.readFloat
import kotlinx.io.writeDouble
import kotlinx.io.writeFloat
import kotlin.io.encoding.Base64

class EncryptedSettings(private val delegate: ObservableSettings, private val aead: Aead) :
    ObservableSettings {
    override val keys: Set<String>
        get() = delegate.keys
    override val size: Int
        get() = delegate.size

    override fun clear() = delegate.clear()

    override fun remove(key: String) = delegate.remove(key)

    override fun hasKey(key: String): Boolean = delegate.hasKey(key)

    override fun putInt(key: String, value: Int) {
        putByteArray(key, value.encoded())
    }

    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String): Int? = getByteArrayOrNull(key)?.decodeInt()

    override fun putLong(key: String, value: Long) {
        putByteArray(key, value.encoded())
    }

    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String): Long? = getByteArrayOrNull(key)?.decodeLong()

    override fun putString(key: String, value: String) {
        putByteArray(key, value.encodeToByteArray())
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? =
        getByteArrayOrNull(key)?.decodeToString()

    override fun putFloat(key: String, value: Float) {
        putByteArray(key, value.encoded())
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String): Float? =
        getByteArrayOrNull(key)?.decodeFloat()

    override fun putDouble(key: String, value: Double) {
        putByteArray(key, value.encoded())
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? =
        getByteArrayOrNull(key)?.decodeDouble()

    override fun putBoolean(key: String, value: Boolean) {
        putByteArray(key, value.encoded())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? =
        getByteArrayOrNull(key)?.decodeBoolean()

    override fun addIntListener(
        key: String,
        defaultValue: Int,
        callback: (Int) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeInt() ?: defaultValue)
    }

    override fun addLongListener(
        key: String,
        defaultValue: Long,
        callback: (Long) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeLong() ?: defaultValue)
    }

    override fun addStringListener(
        key: String,
        defaultValue: String,
        callback: (String) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeToString() ?: defaultValue)
    }

    override fun addFloatListener(
        key: String,
        defaultValue: Float,
        callback: (Float) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeFloat() ?: defaultValue)
    }

    override fun addDoubleListener(
        key: String,
        defaultValue: Double,
        callback: (Double) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeDouble() ?: defaultValue)
    }

    override fun addBooleanListener(
        key: String,
        defaultValue: Boolean,
        callback: (Boolean) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeBoolean() ?: defaultValue)
    }

    override fun addIntOrNullListener(
        key: String,
        callback: (Int?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeInt())
    }

    override fun addLongOrNullListener(
        key: String,
        callback: (Long?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeLong())
    }

    override fun addStringOrNullListener(
        key: String,
        callback: (String?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeToString())
    }

    override fun addFloatOrNullListener(
        key: String,
        callback: (Float?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeFloat())
    }

    override fun addDoubleOrNullListener(
        key: String,
        callback: (Double?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeDouble())
    }

    override fun addBooleanOrNullListener(
        key: String,
        callback: (Boolean?) -> Unit
    ): SettingsListener = addByteArrayOrNullListener(key) {
        callback(it?.decodeBoolean())
    }

    fun putByteArray(key: String, value: ByteArray) =
        delegate.putString(key, aead.encrypt(value).let(Base64::encode))

    fun getByteArrayOrNull(key: String): ByteArray? =
        delegate.getStringOrNull(key)?.let(Base64::decode)?.let(aead::decrypt)

    fun addByteArrayOrNullListener(
        key: String,
        callback: (ByteArray?) -> Unit
    ): SettingsListener =
        delegate.addStringOrNullListener(key) {
            callback(it?.let(Base64::decode)?.let(aead::decrypt))
        }

    private fun Int.encoded(): ByteArray =
        Buffer().apply { writeInt(this@encoded) }.readBytes()

    private fun ByteArray.decodeInt(): Int =
        Buffer().apply { write(this@decodeInt) }.readInt()

    private fun Long.encoded(): ByteArray =
        Buffer().apply { writeLong(this@encoded) }.readBytes()

    private fun ByteArray.decodeLong(): Long =
        Buffer().apply { write(this@decodeLong) }.readLong()

    private fun Double.encoded(): ByteArray =
        Buffer().apply { writeDouble(this@encoded) }.readBytes()

    private fun ByteArray.decodeDouble(): Double =
        Buffer().apply { write(this@decodeDouble) }.readDouble()

    private fun Float.encoded(): ByteArray =
        Buffer().apply { writeFloat(this@encoded) }.readBytes()

    private fun ByteArray.decodeFloat(): Float =
        Buffer().apply { write(this@decodeFloat) }.readFloat()

    private fun Boolean.encoded(): ByteArray =
        if (this@encoded) byteArrayOf(1) else byteArrayOf(1)

    private fun ByteArray.decodeBoolean(): Boolean {
        assert(size == 1) { "Boolean value must be 1 byte long" }
        return this[0] == 1.toByte()
    }
}
