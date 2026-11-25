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

    fun vibrate(type: CalcSoundType, enabled: Boolean, view: View? = null) {
        if (!enabled) return

        val vib = vibrator
        val hardwareVibrated = if (vib != null && vib.hasVibrator()) {
            runCatching {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        val effect = createEffectForApi26Plus(vib, type)
                        vibrateEffect(vib, effect)
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        vib.vibrate(if (type == CalcSoundType.EQUALS) 32L else 18L)
                    }
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

    private fun createEffectForApi26Plus(vib: Vibrator, type: CalcSoundType): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effectId = when (type) {
                CalcSoundType.DIGIT -> VibrationEffect.EFFECT_CLICK
                CalcSoundType.OPERATOR -> VibrationEffect.EFFECT_CLICK
                CalcSoundType.UTILITY -> VibrationEffect.EFFECT_CLICK
                CalcSoundType.EQUALS -> VibrationEffect.EFFECT_HEAVY_CLICK
            }
            if (vib.areAllEffectsSupported(effectId) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
                return VibrationEffect.createPredefined(effectId)
            }
        }

        val durationMs = when (type) {
            CalcSoundType.DIGIT -> 18L
            CalcSoundType.OPERATOR -> 24L
            CalcSoundType.UTILITY -> 24L
            CalcSoundType.EQUALS -> 36L
        }
        val amplitude = if (vib.hasAmplitudeControl()) {
            when (type) {
                CalcSoundType.DIGIT -> 140
                CalcSoundType.OPERATOR -> 185
                CalcSoundType.UTILITY -> 185
                CalcSoundType.EQUALS -> 240
            }
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        return VibrationEffect.createOneShot(durationMs, amplitude)
    }

    private fun vibrateEffect(vib: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_TOUCH)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
