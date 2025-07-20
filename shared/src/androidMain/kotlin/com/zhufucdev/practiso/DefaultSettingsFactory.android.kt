package com.zhufucdev.practiso

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun getDefaultSettingsFactory(): Settings.Factory {
    return SharedPreferencesSettings.Factory(SharedContext)
}