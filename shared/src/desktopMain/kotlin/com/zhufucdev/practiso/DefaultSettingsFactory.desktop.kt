package com.zhufucdev.practiso

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun getDefaultSettingsFactory(): Settings.Factory {
    return PreferencesSettings.Factory(Preferences.userRoot().node("/practiso"))
}