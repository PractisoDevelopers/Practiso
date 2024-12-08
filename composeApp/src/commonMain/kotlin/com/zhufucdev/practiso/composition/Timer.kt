package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration

@Composable
fun rememberClosestTimerAhead(elapsed: Duration?, timers: List<Duration>): Duration? {
    if (elapsed == null) {
        return null
    }

    val sortedTimers by remember { derivedStateOf { timers.sorted() } }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(elapsed) {
        while (index < sortedTimers.size && sortedTimers[index] < elapsed) {
            index++
        }
    }

    return sortedTimers.getOrNull(index)
}

fun Duration.toTimerPresentation(): String = toComponents { hours, minutes, seconds, _ ->
    buildString {
        fun appendComponent(value: Number) {
            if (value.toLong() < 10) {
                append('0')
            }
            append(value)
        }

        if (hours > 0) {
            appendComponent(hours)
            append(':')
        }
        appendComponent(minutes)
        append(':')
        appendComponent(seconds)
    }
}