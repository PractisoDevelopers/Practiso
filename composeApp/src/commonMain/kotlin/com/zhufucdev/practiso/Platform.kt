package com.zhufucdev.practiso

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform