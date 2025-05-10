package com.zhufucdev.practiso.platform

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.zhufucdev.practiso.SharedContext

private fun Vibrator.simpleWobble() {
    vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(10, 50, 100, 100, 100),
            intArrayOf(200, 127, 50, 10, 0),
            -1
        )
    )
}

actual fun wobbleHapticFeedback() {
    val vibrator =
        ContextCompat.getSystemService(SharedContext, Vibrator::class.java) ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val composition = VibrationEffect.startComposition()
        val supported: (Int) -> Boolean = vibrator::areAllPrimitivesSupported
        val primitive =
            VibrationEffect.Composition.PRIMITIVE_SPIN.takeIf(supported)
                ?: VibrationEffect.Composition.PRIMITIVE_CLICK.takeIf(supported)
        if (primitive != null) {
            repeat(4) {
                composition.addPrimitive(primitive, 0.8f / (it + 1))
            }
            vibrator.vibrate(composition.compose())
        } else {
            vibrator.simpleWobble()
        }
    } else {
        vibrator.simpleWobble()
    }
}