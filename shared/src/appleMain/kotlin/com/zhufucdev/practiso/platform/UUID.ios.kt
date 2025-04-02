package com.zhufucdev.practiso.platform

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString