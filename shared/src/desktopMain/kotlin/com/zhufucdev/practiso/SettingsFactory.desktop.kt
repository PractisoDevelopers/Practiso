package com.zhufucdev.practiso

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

actual fun getDefaultSettingsFactory(): ObservableSettingsFactory =
    object : ObservableSettingsFactory {
        override fun create(name: String?): ObservableSettings {
            return PreferencesSettings.Factory(Preferences.userRoot().node("/practiso"))
                .create(name)
        }
    }

actual fun getSecureSettingsFactory(): ObservableSettingsFactory =
    object : ObservableSettingsFactory {
        override fun create(name: String?): ObservableSettings {
            // there's no acceptably secure way to do cryptography on JVM platforms
            return PreferencesSettings.Factory(Preferences.userRoot().node("/practiso/insecure"))
                .create(name)
        }
    }