package com.zhufucdev.practiso

import com.russhwolf.settings.Settings

expect fun getDefaultSettingsFactory(): Settings.Factory
expect fun getSecureSettingsFactory(): Settings.Factory
