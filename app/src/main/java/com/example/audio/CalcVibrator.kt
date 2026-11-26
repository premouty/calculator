package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    STRONG
}

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

    fun vibrate(
        type: CalcSoundType,
        enabled: Boolean,
        intensity: HapticIntensity = HapticIntensity.MEDIUM,
        view: View? = null
    ) {
        if (!enabled) return

        val vib = vibrator
        val hardwareVibrated = if (vib != null && vib.hasVibrator()) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = createEffectForApi26Plus(vib, type, intensity)
                    vibrateEffect(vib, effect)
                } else {
                    val legacyMs = when (intensity) {
                        HapticIntensity.LIGHT -> if (type == CalcSoundType.EQUALS) 20L else 10L
                        HapticIntensity.MEDIUM -> if (type == CalcSoundType.EQUALS) 36L else 20L
                        HapticIntensity.STRONG -> if (type == CalcSoundType.EQUALS) 52L else 30L
                    }
                    @Suppress("DEPRECATION")
                    vib.vibrate(legacyMs)
                }
                true
            }.getOrDefault(false)
        } else {
            false
        }

        if (!hardwareVibrated && view != null) {
            performViewFallbackHaptic(view, type)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createEffectForApi26Plus(
        vib: Vibrator,
        type: CalcSoundType,
        intensity: HapticIntensity
    ): VibrationEffect {
        val durationMs = when (intensity) {
            HapticIntensity.LIGHT -> when (type) {
                CalcSoundType.DIGIT -> 10L
                CalcSoundType.OPERATOR -> 14L
                CalcSoundType.UTILITY -> 12L
                CalcSoundType.EQUALS -> 22L
            }
            HapticIntensity.MEDIUM -> when (type) {
                CalcSoundType.DIGIT -> 18L
                CalcSoundType.OPERATOR -> 24L
                CalcSoundType.UTILITY -> 22L
                CalcSoundType.EQUALS -> 38L
            }
            HapticIntensity.STRONG -> when (type) {
                CalcSoundType.DIGIT -> 28L
                CalcSoundType.OPERATOR -> 36L
                CalcSoundType.UTILITY -> 32L
                CalcSoundType.EQUALS -> 54L
            }
        }
        val amplitude = if (vib.hasAmplitudeControl()) {
            when (intensity) {
                HapticIntensity.LIGHT -> when (type) {
                    CalcSoundType.DIGIT -> 85
                    CalcSoundType.OPERATOR -> 115
                    CalcSoundType.UTILITY -> 100
                    CalcSoundType.EQUALS -> 145
                }
                HapticIntensity.MEDIUM -> when (type) {
                    CalcSoundType.DIGIT -> 175
                    CalcSoundType.OPERATOR -> 215
                    CalcSoundType.UTILITY -> 200
                    CalcSoundType.EQUALS -> 255
                }
                HapticIntensity.STRONG -> when (type) {
                    CalcSoundType.DIGIT -> 235
                    CalcSoundType.OPERATOR -> 255
                    CalcSoundType.UTILITY -> 245
                    CalcSoundType.EQUALS -> 255
                }
            }
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        return VibrationEffect.createOneShot(durationMs, amplitude)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateEffect(vib: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_MEDIA)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vib.vibrate(effect, audioAttrs)
        }
    }

    private fun performViewFallbackHaptic(view: View, type: CalcSoundType) {
        val constant = when (type) {
            CalcSoundType.DIGIT -> HapticFeedbackConstants.KEYBOARD_TAP
            CalcSoundType.OPERATOR,
            CalcSoundType.UTILITY -> HapticFeedbackConstants.VIRTUAL_KEY
            CalcSoundType.EQUALS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        }
        @Suppress("DEPRECATION")
        view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}
