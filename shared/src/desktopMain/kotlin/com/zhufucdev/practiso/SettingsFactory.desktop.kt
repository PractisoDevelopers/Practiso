package com.zhufucdev.practiso

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun getDefaultSettingsFactory(): Settings.Factory {
    return PreferencesSettings.Factory(Preferences.userRoot().node("/practiso"))
}

actual fun getSecureSettingsFactory(): Settings.Factory {
    // there's no acceptably secure way to do cryptography on JVM platforms
    return PreferencesSettings.Factory(Preferences.userRoot().node("/practiso/insecure"))
}