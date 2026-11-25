package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class CalcVibrator(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun vibrate(type: CalcSoundType, enabled: Boolean) {
        if (!enabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val effectId = when (type) {
                        CalcSoundType.DIGIT -> VibrationEffect.EFFECT_TICK
                        CalcSoundType.OPERATOR -> VibrationEffect.EFFECT_CLICK
                        CalcSoundType.UTILITY -> VibrationEffect.EFFECT_CLICK
                        CalcSoundType.EQUALS -> VibrationEffect.EFFECT_HEAVY_CLICK
                    }
                    vib.vibrate(VibrationEffect.createPredefined(effectId))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val durationMs = when (type) {
                        CalcSoundType.DIGIT -> 10L
                        CalcSoundType.OPERATOR -> 14L
                        CalcSoundType.UTILITY -> 14L
                        CalcSoundType.EQUALS -> 24L
                    }
                    val amplitude = when (type) {
                        CalcSoundType.DIGIT -> 75
                        CalcSoundType.OPERATOR -> 105
                        CalcSoundType.UTILITY -> 105
                        CalcSoundType.EQUALS -> 160
                    }
                    vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vib.vibrate(if (type == CalcSoundType.EQUALS) 22L else 12L)
                }
            }
        }
    }
}
