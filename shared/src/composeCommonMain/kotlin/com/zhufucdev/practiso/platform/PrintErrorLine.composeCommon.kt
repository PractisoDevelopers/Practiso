package com.zhufucdev.practiso.platform

actual fun eprintln(content: Any) {
    System.err.println(content)
}

actual fun eprintln() {
    System.err.println()
}