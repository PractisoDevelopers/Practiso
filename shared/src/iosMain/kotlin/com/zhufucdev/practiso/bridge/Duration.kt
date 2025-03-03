package com.zhufucdev.practiso.bridge

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds


fun DurationKt(seconds: Long, attoseconds: Long) = seconds.seconds + (attoseconds / 1e9).nanoseconds