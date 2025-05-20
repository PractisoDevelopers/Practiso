package com.zhufucdev.practiso.convert

import io.ktor.http.Url
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringByExpandingTildeInPath

fun NSURL.toPath() = path!!.toPath()

@Suppress("CAST_NEVER_SUCCEEDS")
fun Path.toNSURL() = NSURL(fileURLWithPath = (normalized().toString() as NSString).stringByExpandingTildeInPath())

fun Url.toNSURL() =
    NSURL.URLWithString(URLString = this.toString(), encodingInvalidCharacters = false)
