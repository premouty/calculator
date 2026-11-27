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
import kotlin.math.roundToInt

enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    STRONG
}

enum class HapticStyle {
    ADAPTIVE,
    SYSTEM,
    PULSE
}

const val DEFAULT_HAPTIC_STRENGTH: Float = 0.70f
const val DEFAULT_HAPTIC_DURATION_MS: Int = 12

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

    private val hasHardwareVibrator: Boolean = runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)
    private val hasAmplitudeControl: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasHardwareVibrator) {
        runCatching { vibrator?.hasAmplitudeControl() == true }.getOrDefault(false)
    } else {
        false
    }
    private val supportsPrimitiveClick: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasHardwareVibrator) {
        runCatching {
            vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK) == true
        }.getOrDefault(false)
    } else {
        false
    }
    private val supportsPrimitiveTick: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasHardwareVibrator) {
        runCatching {
            vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK) == true
        }.getOrDefault(false)
    } else {
        false
    }

    fun vibrate(
        type: CalcSoundType,
        enabled: Boolean,
        intensity: HapticIntensity = HapticIntensity.MEDIUM,
        style: HapticStyle = HapticStyle.ADAPTIVE,
        strength: Float = DEFAULT_HAPTIC_STRENGTH,
        durationMs: Int = DEFAULT_HAPTIC_DURATION_MS,
        accentEquals: Boolean = true,
        view: View? = null,
        forcePreview: Boolean = false
    ) {
        if (!enabled && !forcePreview) return

        val clampedStrength = strength.coerceIn(0.15f, 1.0f)
        val clampedDuration = durationMs.coerceIn(4, 40)

        if (style == HapticStyle.SYSTEM && view != null) {
            if (performSystemHaptic(view, type, intensity, accentEquals)) {
                return
            }
        }

        val vib = vibrator
        val hardwareVibrated = if (vib != null && hasHardwareVibrator) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (style) {
                        HapticStyle.PULSE -> createOneShotEffect(type, clampedStrength, clampedDuration, accentEquals)
                        HapticStyle.ADAPTIVE,
                        HapticStyle.SYSTEM -> createAdaptiveEffect(type, intensity, clampedStrength, clampedDuration, accentEquals)
                    }
                    vibrateEffect(vib, effect)
                } else {
                    val factor = when {
                        !accentEquals -> 1.0f
                        type == CalcSoundType.EQUALS -> 1.35f
                        type == CalcSoundType.OPERATOR || type == CalcSoundType.UTILITY -> 1.15f
                        else -> 1.0f
                    }
                    val legacyMs = (clampedDuration * factor).roundToInt().toLong().coerceIn(5L, 55L)
                    @Suppress("DEPRECATION")
                    vib.vibrate(legacyMs)
                }
                true
            }.getOrDefault(false)
        } else {
            false
        }

        if (!hardwareVibrated && view != null) {
            performSystemHaptic(view, type, intensity, accentEquals)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAdaptiveEffect(
        type: CalcSoundType,
        intensity: HapticIntensity,
        strength: Float,
        fallbackDurationMs: Int,
        accentEquals: Boolean
    ): VibrationEffect {
        // API 30+: hardware LRA primitives produce crisp mechanical clicks without Xiaomi/OEM buzz
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (supportsPrimitiveTick || supportsPrimitiveClick)) {
            val scale = when {
                !accentEquals -> strength
                type == CalcSoundType.EQUALS -> (strength * 1.18f).coerceAtMost(1.0f)
                type == CalcSoundType.OPERATOR -> (strength * 0.96f).coerceIn(0.18f, 1.0f)
                type == CalcSoundType.UTILITY -> (strength * 0.92f).coerceIn(0.18f, 1.0f)
                else -> (strength * 0.85f).coerceIn(0.15f, 1.0f)
            }
            val composition = VibrationEffect.startComposition()
            if (accentEquals && type == CalcSoundType.EQUALS && supportsPrimitiveClick && supportsPrimitiveTick) {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (scale * 0.72f).coerceIn(0.15f, 1.0f))
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale, 22)
            } else {
                val primitive = when {
                    type == CalcSoundType.DIGIT && supportsPrimitiveTick -> VibrationEffect.Composition.PRIMITIVE_TICK
                    supportsPrimitiveClick -> VibrationEffect.Composition.PRIMITIVE_CLICK
                    else -> VibrationEffect.Composition.PRIMITIVE_TICK
                }
                composition.addPrimitive(primitive, scale)
            }
            return composition.compose()
        }

        // API 29+: predefined hardware effects avoid generic continuous buzz on non-Pixel vibrators
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = when {
                accentEquals && type == CalcSoundType.EQUALS && intensity == HapticIntensity.STRONG ->
                    VibrationEffect.EFFECT_HEAVY_CLICK
                accentEquals && type == CalcSoundType.EQUALS ->
                    VibrationEffect.EFFECT_CLICK
                intensity == HapticIntensity.LIGHT ->
                    VibrationEffect.EFFECT_TICK
                intensity == HapticIntensity.STRONG && (type == CalcSoundType.OPERATOR || type == CalcSoundType.UTILITY) ->
                    VibrationEffect.EFFECT_CLICK
                else ->
                    VibrationEffect.EFFECT_TICK
            }
            return VibrationEffect.createPredefined(effectId)
        }

        return createOneShotEffect(type, strength, fallbackDurationMs, accentEquals)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOneShotEffect(
        type: CalcSoundType,
        strength: Float,
        durationMs: Int,
        accentEquals: Boolean
    ): VibrationEffect {
        val durationFactor = when {
            !accentEquals -> 1.0f
            type == CalcSoundType.EQUALS -> 1.40f
            type == CalcSoundType.OPERATOR || type == CalcSoundType.UTILITY -> 1.15f
            else -> 1.0f
        }
        val ampFactor = when {
            !accentEquals -> 1.0f
            type == CalcSoundType.EQUALS -> 1.18f
            type == CalcSoundType.OPERATOR || type == CalcSoundType.UTILITY -> 1.05f
            else -> 0.92f
        }
        val resolvedMs = (durationMs * durationFactor).roundToInt().toLong().coerceIn(4L, 55L)
        val amplitude = if (hasAmplitudeControl) {
            (strength * ampFactor * 255f).roundToInt().coerceIn(28, 255)
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        return VibrationEffect.createOneShot(resolvedMs, amplitude)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateEffect(vib: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // USAGE_TOUCH routes through tactile click HAL rather than media/ringtone rumble
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

    private fun performSystemHaptic(
        view: View,
        type: CalcSoundType,
        intensity: HapticIntensity,
        accentEquals: Boolean
    ): Boolean {
        val constant = when (intensity) {
            HapticIntensity.LIGHT -> when {
                accentEquals && type == CalcSoundType.EQUALS -> HapticFeedbackConstants.KEYBOARD_TAP
                else -> HapticFeedbackConstants.CLOCK_TICK
            }
            HapticIntensity.MEDIUM -> when {
                accentEquals && type == CalcSoundType.EQUALS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    HapticFeedbackConstants.CONFIRM
                accentEquals && type == CalcSoundType.EQUALS -> HapticFeedbackConstants.VIRTUAL_KEY
                else -> HapticFeedbackConstants.KEYBOARD_TAP
            }
            HapticIntensity.STRONG -> when {
                accentEquals && type == CalcSoundType.EQUALS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    HapticFeedbackConstants.CONFIRM
                accentEquals && type == CalcSoundType.EQUALS -> HapticFeedbackConstants.LONG_PRESS
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
        }
        @Suppress("DEPRECATION")
        return view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}
