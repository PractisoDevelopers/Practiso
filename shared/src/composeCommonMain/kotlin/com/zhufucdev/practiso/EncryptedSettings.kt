package com.zhufucdev.practiso

import com.russhwolf.settings.Settings
import io.ktor.utils.io.core.readBytes
import kotlinx.io.Buffer
import kotlinx.io.readDouble
import kotlinx.io.readFloat
import kotlinx.io.writeDouble
import kotlinx.io.writeFloat
import kotlin.io.encoding.Base64

class EncryptedSettings(private val delegate: Settings, private val aead: Aead) : Settings {
    override val keys: Set<String>
        get() = delegate.keys
    override val size: Int
        get() = delegate.size

    override fun clear() = delegate.clear()

    override fun remove(key: String) = delegate.remove(key)

    override fun hasKey(key: String): Boolean = delegate.hasKey(key)

    private fun putAndEncrypt(key: String, data: ByteArray) {
        val cipherText = aead.encrypt(data)
        val base64 = Base64.encode(cipherText)
        delegate.putString(key, base64)
    }

    private inline fun <T> getAndDecrypt(key: String, crossinline block: (ByteArray) -> T): T? {
        val base64 = delegate.getStringOrNull(key) ?: return null
        return try {
            val cipherText = Base64.decode(base64)
            val plainText = aead.decrypt(cipherText)
            block(plainText)
        } catch (e: Exception) {
            println("Error decoding $key in encrypted settings")
            e.printStackTrace()
            null
        }
    }

    override fun putInt(key: String, value: Int) {
        putAndEncrypt(key, Buffer().apply { writeInt(value) }.readBytes())
    }

    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String): Int? = getAndDecrypt(key) { plainText ->
        Buffer().apply { write(plainText) }.readInt()
    }

    override fun putLong(key: String, value: Long) {
        putAndEncrypt(key, Buffer().apply { writeLong(value) }.readBytes())
    }

    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String): Long? = getAndDecrypt(key) { plainText ->
        Buffer().apply { write(plainText) }.readLong()
    }

    override fun putString(key: String, value: String) {
        putAndEncrypt(key, value.encodeToByteArray())
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? = getAndDecrypt(key) { plainText ->
        plainText.decodeToString()
    }

    override fun putFloat(key: String, value: Float) {
        putAndEncrypt(key, Buffer().apply { writeFloat(value) }.readBytes())
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String): Float? = getAndDecrypt(key) { plainText ->
        Buffer().apply { write(plainText) }.readFloat()
    }

    override fun putDouble(key: String, value: Double) {
        putAndEncrypt(key, Buffer().apply { writeDouble(value) }.readBytes())
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? = getAndDecrypt(key) { plainText ->
        Buffer().apply { write(plainText) }.readDouble()
    }

    override fun putBoolean(key: String, value: Boolean) {
        putAndEncrypt(key, byteArrayOf(if (value) 1 else 0))
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? = getAndDecrypt(key) { plainText ->
        if (plainText.isNotEmpty()) plainText[0] == 1.toByte() else null
    }
}



