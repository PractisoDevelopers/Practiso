package com.zhufucdev.practiso.platform

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
val STDERR = platform.posix.fdopen(2, "w")

@OptIn(ExperimentalForeignApi::class)
actual fun eprintln(content: Any) {
    platform.posix.fprintf(STDERR, "%s\n", content.toString())
}

@OptIn(ExperimentalForeignApi::class)
actual fun eprintln() {
    platform.posix.fprintf(STDERR, "\n")
}
