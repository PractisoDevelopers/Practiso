package com.zhufucdev.practiso

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings

expect fun getDefaultSettingsFactory(): ObservableSettingsFactory
expect fun getSecureSettingsFactory(): ObservableSettingsFactory

interface ObservableSettingsFactory : Settings.Factory {
    override fun create(name: String?): ObservableSettings
}