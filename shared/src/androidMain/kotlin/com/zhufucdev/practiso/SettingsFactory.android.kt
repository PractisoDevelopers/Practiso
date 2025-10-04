package com.zhufucdev.practiso

import android.content.Context
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.ChaCha20Poly1305KeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun getDefaultSettingsFactory(): Settings.Factory {
    return SharedPreferencesSettings.Factory(SharedContext)
}

actual fun getSecureSettingsFactory(): Settings.Factory {
    return object : Settings.Factory {
        override fun create(name: String?): Settings {
            val keysetManager = AndroidKeysetManager.Builder()
                .withSharedPref(
                    SharedContext,
                    "secure_settings_keyset",
                    "${SharedContext.packageName}_preferences"
                )
                .withKeyTemplate(ChaCha20Poly1305KeyManager.chaCha20Poly1305Template())
                .withMasterKeyUri("android-keystore://master_key")
                .build()
            val aead = keysetManager.keysetHandle.getPrimitive(
                RegistryConfiguration.get(),
                com.google.crypto.tink.Aead::class.java
            )

            return EncryptedSettings(
                delegate = SharedPreferencesSettings(
                    delegate = SharedContext.getSharedPreferences(
                        "secure_prefs",
                        Context.MODE_PRIVATE
                    ),
                ),
                aead = object : Aead {
                    override fun encrypt(plainText: ByteArray): ByteArray = aead.encrypt(plainText, null)
                    override fun decrypt(cipherText: ByteArray): ByteArray = aead.decrypt(cipherText, null)
                }
            )
        }
    }
}
