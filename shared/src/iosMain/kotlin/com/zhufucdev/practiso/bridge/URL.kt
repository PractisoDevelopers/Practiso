package com.zhufucdev.practiso.bridge

import okio.Path.Companion.toPath
import platform.Foundation.NSURL

fun NSURL.toPath() = path!!.toPath()