package com.zhufucdev.practiso.bridge

import io.ktor.http.Url
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSURL

fun NSURL.toPath() = path!!.toPath()

fun Path.toNSURL() = NSURL(fileURLWithPath = this.toString())

fun Url.toNSURL() = NSURL(string = this.toString(), encodingInvalidCharacters = false)
