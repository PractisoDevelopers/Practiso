@file:OptIn(ExperimentalTime::class)

package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.datamodel.Barcode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private class BarcodeDebouncer(val lastSeen: Instant, val barcode: Barcode)

private fun removeExpired(
    debouncers: MutableMap<String, BarcodeDebouncer>,
    threshold: Duration,
    latest: List<String>
) {
    val now = Clock.System.now()
    debouncers
        .mapNotNull { (key, barcode) ->
            key.takeIf { now - barcode.lastSeen > threshold && key !in latest }
        }
        .forEach { longGoner ->
            debouncers.remove(longGoner)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<List<Barcode>>.debounced(threshold: Duration = 0.5.seconds): Flow<List<Barcode>> {
    val debouncers = mutableMapOf<String, BarcodeDebouncer>()
    return transformLatest { barcodes ->
        val now = Clock.System.now()
        barcodes.forEach {
            debouncers[it.value] = BarcodeDebouncer(lastSeen = now, barcode = it)
        }
        removeExpired(debouncers, threshold, barcodes.map(Barcode::value))
        coroutineScope {
            launch {
                delay(threshold)
                println("debounce timeout, ${barcodes.map { it.value }}")
                removeExpired(debouncers, 0.seconds, barcodes.map(Barcode::value))
                emit(debouncers.values.map(BarcodeDebouncer::barcode))
            }

            emit(debouncers.values.map(BarcodeDebouncer::barcode))
        }
    }
}