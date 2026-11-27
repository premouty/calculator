package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.DEFAULT_HAPTIC_DURATION_MS
import com.example.audio.DEFAULT_HAPTIC_STRENGTH
import com.example.audio.HapticIntensity
import com.example.audio.HapticStyle
import com.example.ui.theme.DEFAULT_CUSTOM_ACCENT_ARGB
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calc_prefs")

const val DEFAULT_SOUND_VOLUME: Float = 0.75f
const val DEFAULT_KEYPAD_SCALE: Float = 1.0f
const val DEFAULT_FX_SCALE: Float = 1.0f
const val DEFAULT_GLOW_INTENSITY: Float = 1.0f

enum class AngleUnit {
    DEG,
    RAD
}

enum class ButtonShapeStyle {
    PILL,
    SQUIRCLE,
    SHARP
}

enum class GlowTarget {
    OFF,
    ACCENT_KEYS,
    ALL_KEYS
}

enum class GlowColorPreset(val argb: Long?) {
    AUTO(null),
    CYAN(0xFF06B6D4L),
    VIOLET(0xFFA855F7L),
    ROSE(0xFFF43F5EL),
    AMBER(0xFFF59E0BL),
    EMERALD(0xFF10B981L)
}

enum class KeyPressEffect {
    SPRING,
    SHOCKWAVE,
    NEON_BURST,
    SOFT_TILT
}

enum class ResultAnimStyle {
    SLIDE_UP,
    ELASTIC_POP,
    SHIMMER_SWEEP
}

enum class ThemeAnimStyle {
    CIRCLE_REVEAL,
    CROSSFADE,
    DIAGONAL_WIPE,
    VERTICAL_CURTAIN,
    INSTANT
}

enum class DisplayGlowStyle {
    AURORA,
    NEON_FRAME,
    PRISM_WAVE,
    BOTTOM_HORIZON
}

data class UserCalcPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val isScientificExpanded: Boolean = false,
    val isSoundEnabled: Boolean = false,
    val isVibrationEnabled: Boolean = false,
    val isFeedbackPermissionGranted: Boolean = false,
    val soundVolume: Float = DEFAULT_SOUND_VOLUME,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val hapticStyle: HapticStyle = HapticStyle.ADAPTIVE,
    val hapticStrength: Float = DEFAULT_HAPTIC_STRENGTH,
    val hapticDurationMs: Int = DEFAULT_HAPTIC_DURATION_MS,
    val hapticAccentEquals: Boolean = true,
    val buttonShapeStyle: ButtonShapeStyle = ButtonShapeStyle.PILL,
    val useDynamicColor: Boolean = true,
    val customAccentArgb: Long = DEFAULT_CUSTOM_ACCENT_ARGB,
    val showLivePreview: Boolean = true,
    val ambientDisplayGlow: Boolean = false,
    val keypadScale: Float = DEFAULT_KEYPAD_SCALE,
    val fxScale: Float = DEFAULT_FX_SCALE,
    val fxUnifiedWithKeypad: Boolean = false,
    val glowTarget: GlowTarget = GlowTarget.ACCENT_KEYS,
    val glowIntensity: Float = DEFAULT_GLOW_INTENSITY,
    val glowColorPreset: GlowColorPreset = GlowColorPreset.AUTO,
    val keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    val resultAnimStyle: ResultAnimStyle = ResultAnimStyle.SLIDE_UP,
    val pressSparksEnabled: Boolean = false,
    val glowBreathingEnabled: Boolean = false,
    val themeAnimStyle: ThemeAnimStyle = ThemeAnimStyle.CIRCLE_REVEAL,
    val displayGlowStyle: DisplayGlowStyle = DisplayGlowStyle.AURORA,
    val displayGlowIntensity: Float = DEFAULT_GLOW_INTENSITY,
    val displayGlowColorPreset: GlowColorPreset = GlowColorPreset.AUTO
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val SCIENTIFIC_EXPANDED = booleanPreferencesKey("scientific_expanded")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FEEDBACK_PERMISSION_GRANTED = booleanPreferencesKey("feedback_permission_granted")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val HAPTIC_STYLE = stringPreferencesKey("haptic_style")
        val HAPTIC_STRENGTH = floatPreferencesKey("haptic_strength")
        val HAPTIC_DURATION_MS = intPreferencesKey("haptic_duration_ms")
        val HAPTIC_ACCENT_EQUALS = booleanPreferencesKey("haptic_accent_equals")
        val BUTTON_SHAPE_STYLE = stringPreferencesKey("button_shape_style")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val CUSTOM_ACCENT_ARGB = longPreferencesKey("custom_accent_argb")
        val SHOW_LIVE_PREVIEW = booleanPreferencesKey("show_live_preview")
        val AMBIENT_DISPLAY_GLOW = booleanPreferencesKey("ambient_display_glow")
        val KEYPAD_SCALE = floatPreferencesKey("keypad_scale")
        val FX_SCALE = floatPreferencesKey("fx_scale")
        val FX_UNIFIED_WITH_KEYPAD = booleanPreferencesKey("fx_unified_with_keypad")
        val GLOW_TARGET = stringPreferencesKey("glow_target")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val GLOW_COLOR_PRESET = stringPreferencesKey("glow_color_preset")
        val KEY_PRESS_EFFECT = stringPreferencesKey("key_press_effect")
        val RESULT_ANIM_STYLE = stringPreferencesKey("result_anim_style")
        val PRESS_SPARKS_ENABLED = booleanPreferencesKey("press_sparks_enabled")
        val GLOW_BREATHING_ENABLED = booleanPreferencesKey("glow_breathing_enabled")
        val THEME_ANIM_STYLE = stringPreferencesKey("theme_anim_style")
        val DISPLAY_GLOW_STYLE = stringPreferencesKey("display_glow_style")
        val DISPLAY_GLOW_INTENSITY = floatPreferencesKey("display_glow_intensity")
        val DISPLAY_GLOW_COLOR_PRESET = stringPreferencesKey("display_glow_color_preset")
    }

    private object SyncKeys {
        const val PREFS_NAME = "calc_prefs_sync"
        const val THEME_MODE = "theme_mode"
        const val ANGLE_UNIT = "angle_unit"
        const val SCIENTIFIC_EXPANDED = "scientific_expanded"
        const val SOUND_ENABLED = "sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
        const val FEEDBACK_PERMISSION_GRANTED = "feedback_permission_granted"
        const val SOUND_VOLUME = "sound_volume"
        const val HAPTIC_INTENSITY = "haptic_intensity"
        const val HAPTIC_STYLE = "haptic_style"
        const val HAPTIC_STRENGTH = "haptic_strength"
        const val HAPTIC_DURATION_MS = "haptic_duration_ms"
        const val HAPTIC_ACCENT_EQUALS = "haptic_accent_equals"
        const val BUTTON_SHAPE_STYLE = "button_shape_style"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val CUSTOM_ACCENT_ARGB = "custom_accent_argb"
        const val SHOW_LIVE_PREVIEW = "show_live_preview"
        const val AMBIENT_DISPLAY_GLOW = "ambient_display_glow"
        const val KEYPAD_SCALE = "keypad_scale"
        const val FX_SCALE = "fx_scale"
        const val FX_UNIFIED_WITH_KEYPAD = "fx_unified_with_keypad"
        const val GLOW_TARGET = "glow_target"
        const val GLOW_INTENSITY = "glow_intensity"
        const val GLOW_COLOR_PRESET = "glow_color_preset"
        const val KEY_PRESS_EFFECT = "key_press_effect"
        const val RESULT_ANIM_STYLE = "result_anim_style"
        const val PRESS_SPARKS_ENABLED = "press_sparks_enabled"
        const val GLOW_BREATHING_ENABLED = "glow_breathing_enabled"
        const val THEME_ANIM_STYLE = "theme_anim_style"
        const val DISPLAY_GLOW_STYLE = "display_glow_style"
        const val DISPLAY_GLOW_INTENSITY = "display_glow_intensity"
        const val DISPLAY_GLOW_COLOR_PRESET = "display_glow_color_preset"
    }

    private val syncPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(SyncKeys.PREFS_NAME, Context.MODE_PRIVATE)

    fun readInitialPreferencesSync(): UserCalcPreferences {
        val theme = syncPrefs.getString(SyncKeys.THEME_MODE, null)?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.DARK

        val angle = syncPrefs.getString(SyncKeys.ANGLE_UNIT, null)?.let {
            runCatching { AngleUnit.valueOf(it) }.getOrNull()
        } ?: AngleUnit.DEG

        val hapticIntensity = syncPrefs.getString(SyncKeys.HAPTIC_INTENSITY, null)?.let {
            runCatching { HapticIntensity.valueOf(it) }.getOrNull()
        } ?: HapticIntensity.MEDIUM

        val hapticStyle = syncPrefs.getString(SyncKeys.HAPTIC_STYLE, null)?.let {
            runCatching { HapticStyle.valueOf(it) }.getOrNull()
        } ?: HapticStyle.ADAPTIVE

        val hapticStrength = syncPrefs.getFloat(SyncKeys.HAPTIC_STRENGTH, DEFAULT_HAPTIC_STRENGTH)
            .coerceIn(0.15f, 1.0f)

        val hapticDurationMs = syncPrefs.getInt(SyncKeys.HAPTIC_DURATION_MS, DEFAULT_HAPTIC_DURATION_MS)
            .coerceIn(4, 40)

        val hapticAccentEquals = syncPrefs.getBoolean(SyncKeys.HAPTIC_ACCENT_EQUALS, true)

        val buttonShapeStyle = syncPrefs.getString(SyncKeys.BUTTON_SHAPE_STYLE, null)?.let {
            runCatching { ButtonShapeStyle.valueOf(it) }.getOrNull()
        } ?: ButtonShapeStyle.PILL

        val soundVolume = syncPrefs.getFloat(SyncKeys.SOUND_VOLUME, DEFAULT_SOUND_VOLUME)
            .coerceIn(0.05f, 1.0f)

        val keypadScale = syncPrefs.getFloat(SyncKeys.KEYPAD_SCALE, DEFAULT_KEYPAD_SCALE)
            .coerceIn(0.80f, 1.18f)

        val fxScale = syncPrefs.getFloat(SyncKeys.FX_SCALE, DEFAULT_FX_SCALE)
            .coerceIn(0.78f, 1.25f)

        val glowTarget = syncPrefs.getString(SyncKeys.GLOW_TARGET, null)?.let {
            runCatching { GlowTarget.valueOf(it) }.getOrNull()
        } ?: GlowTarget.ACCENT_KEYS

        val glowIntensity = syncPrefs.getFloat(SyncKeys.GLOW_INTENSITY, DEFAULT_GLOW_INTENSITY)
            .coerceIn(0.35f, 1.65f)

        val glowColorPreset = syncPrefs.getString(SyncKeys.GLOW_COLOR_PRESET, null)?.let {
            runCatching { GlowColorPreset.valueOf(it) }.getOrNull()
        } ?: GlowColorPreset.AUTO

        val keyPressEffect = syncPrefs.getString(SyncKeys.KEY_PRESS_EFFECT, null)?.let {
            runCatching { KeyPressEffect.valueOf(it) }.getOrNull()
        } ?: KeyPressEffect.SPRING

        val resultAnimStyle = syncPrefs.getString(SyncKeys.RESULT_ANIM_STYLE, null)?.let {
            runCatching { ResultAnimStyle.valueOf(it) }.getOrNull()
        } ?: ResultAnimStyle.SLIDE_UP

        val themeAnimStyle = syncPrefs.getString(SyncKeys.THEME_ANIM_STYLE, null)?.let {
            runCatching { ThemeAnimStyle.valueOf(it) }.getOrNull()
        } ?: ThemeAnimStyle.CIRCLE_REVEAL

        val displayGlowStyle = syncPrefs.getString(SyncKeys.DISPLAY_GLOW_STYLE, null)?.let {
            runCatching { DisplayGlowStyle.valueOf(it) }.getOrNull()
        } ?: DisplayGlowStyle.AURORA

        val displayGlowIntensity = syncPrefs.getFloat(SyncKeys.DISPLAY_GLOW_INTENSITY, DEFAULT_GLOW_INTENSITY)
            .coerceIn(0.40f, 2.0f)

        val displayGlowColorPreset = syncPrefs.getString(SyncKeys.DISPLAY_GLOW_COLOR_PRESET, null)?.let {
            runCatching { GlowColorPreset.valueOf(it) }.getOrNull()
        } ?: GlowColorPreset.AUTO

        return UserCalcPreferences(
            themeMode = theme,
            angleUnit = angle,
            isScientificExpanded = syncPrefs.getBoolean(SyncKeys.SCIENTIFIC_EXPANDED, false),
            isSoundEnabled = syncPrefs.getBoolean(SyncKeys.SOUND_ENABLED, false),
            isVibrationEnabled = syncPrefs.getBoolean(SyncKeys.VIBRATION_ENABLED, false),
            isFeedbackPermissionGranted = syncPrefs.getBoolean(SyncKeys.FEEDBACK_PERMISSION_GRANTED, false),
            soundVolume = soundVolume,
            hapticIntensity = hapticIntensity,
            hapticStyle = hapticStyle,
            hapticStrength = hapticStrength,
            hapticDurationMs = hapticDurationMs,
            hapticAccentEquals = hapticAccentEquals,
            buttonShapeStyle = buttonShapeStyle,
            useDynamicColor = syncPrefs.getBoolean(SyncKeys.USE_DYNAMIC_COLOR, true),
            customAccentArgb = syncPrefs.getLong(SyncKeys.CUSTOM_ACCENT_ARGB, DEFAULT_CUSTOM_ACCENT_ARGB),
            showLivePreview = syncPrefs.getBoolean(SyncKeys.SHOW_LIVE_PREVIEW, true),
            ambientDisplayGlow = syncPrefs.getBoolean(SyncKeys.AMBIENT_DISPLAY_GLOW, false),
            keypadScale = keypadScale,
            fxScale = fxScale,
            fxUnifiedWithKeypad = syncPrefs.getBoolean(SyncKeys.FX_UNIFIED_WITH_KEYPAD, false),
            glowTarget = glowTarget,
            glowIntensity = glowIntensity,
            glowColorPreset = glowColorPreset,
            keyPressEffect = keyPressEffect,
            resultAnimStyle = resultAnimStyle,
            pressSparksEnabled = syncPrefs.getBoolean(SyncKeys.PRESS_SPARKS_ENABLED, false),
            glowBreathingEnabled = syncPrefs.getBoolean(SyncKeys.GLOW_BREATHING_ENABLED, false),
            themeAnimStyle = themeAnimStyle,
            displayGlowStyle = displayGlowStyle,
            displayGlowIntensity = displayGlowIntensity,
            displayGlowColorPreset = displayGlowColorPreset
        )
    }

    val preferencesFlow: Flow<UserCalcPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.DARK

        val angle = prefs[Keys.ANGLE_UNIT]?.let {
            runCatching { AngleUnit.valueOf(it) }.getOrNull()
        } ?: AngleUnit.DEG

        val scientific = prefs[Keys.SCIENTIFIC_EXPANDED] ?: false
        val soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false
        val vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: false
        val permissionGranted = prefs[Keys.FEEDBACK_PERMISSION_GRANTED] ?: false
        val soundVolume = (prefs[Keys.SOUND_VOLUME] ?: DEFAULT_SOUND_VOLUME).coerceIn(0.05f, 1.0f)

        val hapticIntensity = prefs[Keys.HAPTIC_INTENSITY]?.let {
            runCatching { HapticIntensity.valueOf(it) }.getOrNull()
        } ?: HapticIntensity.MEDIUM

        val hapticStyle = prefs[Keys.HAPTIC_STYLE]?.let {
            runCatching { HapticStyle.valueOf(it) }.getOrNull()
        } ?: HapticStyle.ADAPTIVE

        val hapticStrength = (prefs[Keys.HAPTIC_STRENGTH] ?: DEFAULT_HAPTIC_STRENGTH).coerceIn(0.15f, 1.0f)
        val hapticDurationMs = (prefs[Keys.HAPTIC_DURATION_MS] ?: DEFAULT_HAPTIC_DURATION_MS).coerceIn(4, 40)
        val hapticAccentEquals = prefs[Keys.HAPTIC_ACCENT_EQUALS] ?: true

        val buttonShapeStyle = prefs[Keys.BUTTON_SHAPE_STYLE]?.let {
            runCatching { ButtonShapeStyle.valueOf(it) }.getOrNull()
        } ?: ButtonShapeStyle.PILL

        val useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true
        val customAccentArgb = prefs[Keys.CUSTOM_ACCENT_ARGB] ?: DEFAULT_CUSTOM_ACCENT_ARGB
        val showLivePreview = prefs[Keys.SHOW_LIVE_PREVIEW] ?: true
        val ambientDisplayGlow = prefs[Keys.AMBIENT_DISPLAY_GLOW] ?: false
        val keypadScale = (prefs[Keys.KEYPAD_SCALE] ?: DEFAULT_KEYPAD_SCALE).coerceIn(0.80f, 1.18f)
        val fxScale = (prefs[Keys.FX_SCALE] ?: DEFAULT_FX_SCALE).coerceIn(0.78f, 1.25f)
        val fxUnifiedWithKeypad = prefs[Keys.FX_UNIFIED_WITH_KEYPAD] ?: false
        val glowTarget = prefs[Keys.GLOW_TARGET]?.let {
            runCatching { GlowTarget.valueOf(it) }.getOrNull()
        } ?: GlowTarget.ACCENT_KEYS
        val glowIntensity = (prefs[Keys.GLOW_INTENSITY] ?: DEFAULT_GLOW_INTENSITY).coerceIn(0.35f, 1.65f)
        val glowColorPreset = prefs[Keys.GLOW_COLOR_PRESET]?.let {
            runCatching { GlowColorPreset.valueOf(it) }.getOrNull()
        } ?: GlowColorPreset.AUTO
        val keyPressEffect = prefs[Keys.KEY_PRESS_EFFECT]?.let {
            runCatching { KeyPressEffect.valueOf(it) }.getOrNull()
        } ?: KeyPressEffect.SPRING
        val resultAnimStyle = prefs[Keys.RESULT_ANIM_STYLE]?.let {
            runCatching { ResultAnimStyle.valueOf(it) }.getOrNull()
        } ?: ResultAnimStyle.SLIDE_UP
        val pressSparksEnabled = prefs[Keys.PRESS_SPARKS_ENABLED] ?: false
        val glowBreathingEnabled = prefs[Keys.GLOW_BREATHING_ENABLED] ?: false
        val themeAnimStyle = prefs[Keys.THEME_ANIM_STYLE]?.let {
            runCatching { ThemeAnimStyle.valueOf(it) }.getOrNull()
        } ?: ThemeAnimStyle.CIRCLE_REVEAL
        val displayGlowStyle = prefs[Keys.DISPLAY_GLOW_STYLE]?.let {
            runCatching { DisplayGlowStyle.valueOf(it) }.getOrNull()
        } ?: DisplayGlowStyle.AURORA
        val displayGlowIntensity = (prefs[Keys.DISPLAY_GLOW_INTENSITY] ?: DEFAULT_GLOW_INTENSITY)
            .coerceIn(0.40f, 2.0f)
        val displayGlowColorPreset = prefs[Keys.DISPLAY_GLOW_COLOR_PRESET]?.let {
            runCatching { GlowColorPreset.valueOf(it) }.getOrNull()
        } ?: GlowColorPreset.AUTO

        // Backfill synchronous mirror once for installations migrating from DataStore-only
        if (!syncPrefs.contains(SyncKeys.THEME_MODE) && prefs.contains(Keys.THEME_MODE)) {
            syncPrefs.edit().putString(SyncKeys.THEME_MODE, theme.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.ANGLE_UNIT) && prefs.contains(Keys.ANGLE_UNIT)) {
            syncPrefs.edit().putString(SyncKeys.ANGLE_UNIT, angle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.SCIENTIFIC_EXPANDED) && prefs.contains(Keys.SCIENTIFIC_EXPANDED)) {
            syncPrefs.edit().putBoolean(SyncKeys.SCIENTIFIC_EXPANDED, scientific).apply()
        }
        if (!syncPrefs.contains(SyncKeys.SOUND_ENABLED) && prefs.contains(Keys.SOUND_ENABLED)) {
            syncPrefs.edit().putBoolean(SyncKeys.SOUND_ENABLED, soundEnabled).apply()
        }
        if (!syncPrefs.contains(SyncKeys.VIBRATION_ENABLED) && prefs.contains(Keys.VIBRATION_ENABLED)) {
            syncPrefs.edit().putBoolean(SyncKeys.VIBRATION_ENABLED, vibrationEnabled).apply()
        }
        if (!syncPrefs.contains(SyncKeys.FEEDBACK_PERMISSION_GRANTED) && prefs.contains(Keys.FEEDBACK_PERMISSION_GRANTED)) {
            syncPrefs.edit().putBoolean(SyncKeys.FEEDBACK_PERMISSION_GRANTED, permissionGranted).apply()
        }
        if (!syncPrefs.contains(SyncKeys.SOUND_VOLUME) && prefs.contains(Keys.SOUND_VOLUME)) {
            syncPrefs.edit().putFloat(SyncKeys.SOUND_VOLUME, soundVolume).apply()
        }
        if (!syncPrefs.contains(SyncKeys.HAPTIC_INTENSITY) && prefs.contains(Keys.HAPTIC_INTENSITY)) {
            syncPrefs.edit().putString(SyncKeys.HAPTIC_INTENSITY, hapticIntensity.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.HAPTIC_STYLE) && prefs.contains(Keys.HAPTIC_STYLE)) {
            syncPrefs.edit().putString(SyncKeys.HAPTIC_STYLE, hapticStyle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.HAPTIC_STRENGTH) && prefs.contains(Keys.HAPTIC_STRENGTH)) {
            syncPrefs.edit().putFloat(SyncKeys.HAPTIC_STRENGTH, hapticStrength).apply()
        }
        if (!syncPrefs.contains(SyncKeys.HAPTIC_DURATION_MS) && prefs.contains(Keys.HAPTIC_DURATION_MS)) {
            syncPrefs.edit().putInt(SyncKeys.HAPTIC_DURATION_MS, hapticDurationMs).apply()
        }
        if (!syncPrefs.contains(SyncKeys.HAPTIC_ACCENT_EQUALS) && prefs.contains(Keys.HAPTIC_ACCENT_EQUALS)) {
            syncPrefs.edit().putBoolean(SyncKeys.HAPTIC_ACCENT_EQUALS, hapticAccentEquals).apply()
        }
        if (!syncPrefs.contains(SyncKeys.BUTTON_SHAPE_STYLE) && prefs.contains(Keys.BUTTON_SHAPE_STYLE)) {
            syncPrefs.edit().putString(SyncKeys.BUTTON_SHAPE_STYLE, buttonShapeStyle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.USE_DYNAMIC_COLOR) && prefs.contains(Keys.USE_DYNAMIC_COLOR)) {
            syncPrefs.edit().putBoolean(SyncKeys.USE_DYNAMIC_COLOR, useDynamicColor).apply()
        }
        if (!syncPrefs.contains(SyncKeys.CUSTOM_ACCENT_ARGB) && prefs.contains(Keys.CUSTOM_ACCENT_ARGB)) {
            syncPrefs.edit().putLong(SyncKeys.CUSTOM_ACCENT_ARGB, customAccentArgb).apply()
        }
        if (!syncPrefs.contains(SyncKeys.SHOW_LIVE_PREVIEW) && prefs.contains(Keys.SHOW_LIVE_PREVIEW)) {
            syncPrefs.edit().putBoolean(SyncKeys.SHOW_LIVE_PREVIEW, showLivePreview).apply()
        }
        if (!syncPrefs.contains(SyncKeys.AMBIENT_DISPLAY_GLOW) && prefs.contains(Keys.AMBIENT_DISPLAY_GLOW)) {
            syncPrefs.edit().putBoolean(SyncKeys.AMBIENT_DISPLAY_GLOW, ambientDisplayGlow).apply()
        }
        if (!syncPrefs.contains(SyncKeys.KEYPAD_SCALE) && prefs.contains(Keys.KEYPAD_SCALE)) {
            syncPrefs.edit().putFloat(SyncKeys.KEYPAD_SCALE, keypadScale).apply()
        }
        if (!syncPrefs.contains(SyncKeys.FX_SCALE) && prefs.contains(Keys.FX_SCALE)) {
            syncPrefs.edit().putFloat(SyncKeys.FX_SCALE, fxScale).apply()
        }
        if (!syncPrefs.contains(SyncKeys.FX_UNIFIED_WITH_KEYPAD) && prefs.contains(Keys.FX_UNIFIED_WITH_KEYPAD)) {
            syncPrefs.edit().putBoolean(SyncKeys.FX_UNIFIED_WITH_KEYPAD, fxUnifiedWithKeypad).apply()
        }
        if (!syncPrefs.contains(SyncKeys.GLOW_TARGET) && prefs.contains(Keys.GLOW_TARGET)) {
            syncPrefs.edit().putString(SyncKeys.GLOW_TARGET, glowTarget.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.GLOW_INTENSITY) && prefs.contains(Keys.GLOW_INTENSITY)) {
            syncPrefs.edit().putFloat(SyncKeys.GLOW_INTENSITY, glowIntensity).apply()
        }
        if (!syncPrefs.contains(SyncKeys.GLOW_COLOR_PRESET) && prefs.contains(Keys.GLOW_COLOR_PRESET)) {
            syncPrefs.edit().putString(SyncKeys.GLOW_COLOR_PRESET, glowColorPreset.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.KEY_PRESS_EFFECT) && prefs.contains(Keys.KEY_PRESS_EFFECT)) {
            syncPrefs.edit().putString(SyncKeys.KEY_PRESS_EFFECT, keyPressEffect.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.RESULT_ANIM_STYLE) && prefs.contains(Keys.RESULT_ANIM_STYLE)) {
            syncPrefs.edit().putString(SyncKeys.RESULT_ANIM_STYLE, resultAnimStyle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.PRESS_SPARKS_ENABLED) && prefs.contains(Keys.PRESS_SPARKS_ENABLED)) {
            syncPrefs.edit().putBoolean(SyncKeys.PRESS_SPARKS_ENABLED, pressSparksEnabled).apply()
        }
        if (!syncPrefs.contains(SyncKeys.GLOW_BREATHING_ENABLED) && prefs.contains(Keys.GLOW_BREATHING_ENABLED)) {
            syncPrefs.edit().putBoolean(SyncKeys.GLOW_BREATHING_ENABLED, glowBreathingEnabled).apply()
        }
        if (!syncPrefs.contains(SyncKeys.THEME_ANIM_STYLE) && prefs.contains(Keys.THEME_ANIM_STYLE)) {
            syncPrefs.edit().putString(SyncKeys.THEME_ANIM_STYLE, themeAnimStyle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.DISPLAY_GLOW_STYLE) && prefs.contains(Keys.DISPLAY_GLOW_STYLE)) {
            syncPrefs.edit().putString(SyncKeys.DISPLAY_GLOW_STYLE, displayGlowStyle.name).apply()
        }
        if (!syncPrefs.contains(SyncKeys.DISPLAY_GLOW_INTENSITY) && prefs.contains(Keys.DISPLAY_GLOW_INTENSITY)) {
            syncPrefs.edit().putFloat(SyncKeys.DISPLAY_GLOW_INTENSITY, displayGlowIntensity).apply()
        }
        if (!syncPrefs.contains(SyncKeys.DISPLAY_GLOW_COLOR_PRESET) && prefs.contains(Keys.DISPLAY_GLOW_COLOR_PRESET)) {
            syncPrefs.edit().putString(SyncKeys.DISPLAY_GLOW_COLOR_PRESET, displayGlowColorPreset.name).apply()
        }

        UserCalcPreferences(
            themeMode = theme,
            angleUnit = angle,
            isScientificExpanded = scientific,
            isSoundEnabled = soundEnabled,
            isVibrationEnabled = vibrationEnabled,
            isFeedbackPermissionGranted = permissionGranted,
            soundVolume = soundVolume,
            hapticIntensity = hapticIntensity,
            hapticStyle = hapticStyle,
            hapticStrength = hapticStrength,
            hapticDurationMs = hapticDurationMs,
            hapticAccentEquals = hapticAccentEquals,
            buttonShapeStyle = buttonShapeStyle,
            useDynamicColor = useDynamicColor,
            customAccentArgb = customAccentArgb,
            showLivePreview = showLivePreview,
            ambientDisplayGlow = ambientDisplayGlow,
            keypadScale = keypadScale,
            fxScale = fxScale,
            fxUnifiedWithKeypad = fxUnifiedWithKeypad,
            glowTarget = glowTarget,
            glowIntensity = glowIntensity,
            glowColorPreset = glowColorPreset,
            keyPressEffect = keyPressEffect,
            resultAnimStyle = resultAnimStyle,
            pressSparksEnabled = pressSparksEnabled,
            glowBreathingEnabled = glowBreathingEnabled,
            themeAnimStyle = themeAnimStyle,
            displayGlowStyle = displayGlowStyle,
            displayGlowIntensity = displayGlowIntensity,
            displayGlowColorPreset = displayGlowColorPreset
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        syncPrefs.edit().putString(SyncKeys.THEME_MODE, mode.name).apply()
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        syncPrefs.edit().putString(SyncKeys.ANGLE_UNIT, unit.name).apply()
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setScientificExpanded(expanded: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.SCIENTIFIC_EXPANDED, expanded).apply()
        context.dataStore.edit { it[Keys.SCIENTIFIC_EXPANDED] = expanded }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.SOUND_ENABLED, enabled).apply()
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.VIBRATION_ENABLED, enabled).apply()
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setFeedbackEnabled(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        syncPrefs.edit()
            .putBoolean(SyncKeys.SOUND_ENABLED, soundEnabled)
            .putBoolean(SyncKeys.VIBRATION_ENABLED, vibrationEnabled)
            .apply()
        context.dataStore.edit {
            it[Keys.SOUND_ENABLED] = soundEnabled
            it[Keys.VIBRATION_ENABLED] = vibrationEnabled
        }
    }

    suspend fun grantFeedbackPermissionAndEnable(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        syncPrefs.edit()
            .putBoolean(SyncKeys.FEEDBACK_PERMISSION_GRANTED, true)
            .putBoolean(SyncKeys.SOUND_ENABLED, soundEnabled)
            .putBoolean(SyncKeys.VIBRATION_ENABLED, vibrationEnabled)
            .apply()
        context.dataStore.edit {
            it[Keys.FEEDBACK_PERMISSION_GRANTED] = true
            it[Keys.SOUND_ENABLED] = soundEnabled
            it[Keys.VIBRATION_ENABLED] = vibrationEnabled
        }
    }

    suspend fun setSoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0.05f, 1.0f)
        syncPrefs.edit().putFloat(SyncKeys.SOUND_VOLUME, clamped).apply()
        context.dataStore.edit { it[Keys.SOUND_VOLUME] = clamped }
    }

    suspend fun setHapticIntensity(intensity: HapticIntensity) {
        syncPrefs.edit().putString(SyncKeys.HAPTIC_INTENSITY, intensity.name).apply()
        context.dataStore.edit { it[Keys.HAPTIC_INTENSITY] = intensity.name }
    }

    suspend fun setHapticPreset(intensity: HapticIntensity, strength: Float, durationMs: Int) {
        val clampedStrength = strength.coerceIn(0.15f, 1.0f)
        val clampedDuration = durationMs.coerceIn(4, 40)
        syncPrefs.edit()
            .putString(SyncKeys.HAPTIC_INTENSITY, intensity.name)
            .putFloat(SyncKeys.HAPTIC_STRENGTH, clampedStrength)
            .putInt(SyncKeys.HAPTIC_DURATION_MS, clampedDuration)
            .apply()
        context.dataStore.edit {
            it[Keys.HAPTIC_INTENSITY] = intensity.name
            it[Keys.HAPTIC_STRENGTH] = clampedStrength
            it[Keys.HAPTIC_DURATION_MS] = clampedDuration
        }
    }

    suspend fun setHapticStyle(style: HapticStyle) {
        syncPrefs.edit().putString(SyncKeys.HAPTIC_STYLE, style.name).apply()
        context.dataStore.edit { it[Keys.HAPTIC_STYLE] = style.name }
    }

    suspend fun setHapticStrength(strength: Float) {
        val clamped = strength.coerceIn(0.15f, 1.0f)
        syncPrefs.edit().putFloat(SyncKeys.HAPTIC_STRENGTH, clamped).apply()
        context.dataStore.edit { it[Keys.HAPTIC_STRENGTH] = clamped }
    }

    suspend fun setHapticDurationMs(durationMs: Int) {
        val clamped = durationMs.coerceIn(4, 40)
        syncPrefs.edit().putInt(SyncKeys.HAPTIC_DURATION_MS, clamped).apply()
        context.dataStore.edit { it[Keys.HAPTIC_DURATION_MS] = clamped }
    }

    suspend fun setHapticAccentEquals(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.HAPTIC_ACCENT_EQUALS, enabled).apply()
        context.dataStore.edit { it[Keys.HAPTIC_ACCENT_EQUALS] = enabled }
    }

    suspend fun setButtonShapeStyle(style: ButtonShapeStyle) {
        syncPrefs.edit().putString(SyncKeys.BUTTON_SHAPE_STYLE, style.name).apply()
        context.dataStore.edit { it[Keys.BUTTON_SHAPE_STYLE] = style.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.USE_DYNAMIC_COLOR, enabled).apply()
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCustomAccentArgb(argb: Long) {
        syncPrefs.edit().putLong(SyncKeys.CUSTOM_ACCENT_ARGB, argb).apply()
        context.dataStore.edit { it[Keys.CUSTOM_ACCENT_ARGB] = argb }
    }

    suspend fun setShowLivePreview(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.SHOW_LIVE_PREVIEW, enabled).apply()
        context.dataStore.edit { it[Keys.SHOW_LIVE_PREVIEW] = enabled }
    }

    suspend fun setAmbientDisplayGlow(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.AMBIENT_DISPLAY_GLOW, enabled).apply()
        context.dataStore.edit { it[Keys.AMBIENT_DISPLAY_GLOW] = enabled }
    }

    suspend fun setKeypadScale(scale: Float) {
        val clamped = scale.coerceIn(0.80f, 1.18f)
        syncPrefs.edit().putFloat(SyncKeys.KEYPAD_SCALE, clamped).apply()
        context.dataStore.edit { it[Keys.KEYPAD_SCALE] = clamped }
    }

    suspend fun setFxScale(scale: Float) {
        val clamped = scale.coerceIn(0.78f, 1.25f)
        syncPrefs.edit().putFloat(SyncKeys.FX_SCALE, clamped).apply()
        context.dataStore.edit { it[Keys.FX_SCALE] = clamped }
    }

    suspend fun setGlowTarget(target: GlowTarget) {
        syncPrefs.edit().putString(SyncKeys.GLOW_TARGET, target.name).apply()
        context.dataStore.edit { it[Keys.GLOW_TARGET] = target.name }
    }

    suspend fun setGlowIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.35f, 1.65f)
        syncPrefs.edit().putFloat(SyncKeys.GLOW_INTENSITY, clamped).apply()
        context.dataStore.edit { it[Keys.GLOW_INTENSITY] = clamped }
    }

    suspend fun setGlowColorPreset(preset: GlowColorPreset) {
        syncPrefs.edit().putString(SyncKeys.GLOW_COLOR_PRESET, preset.name).apply()
        context.dataStore.edit { it[Keys.GLOW_COLOR_PRESET] = preset.name }
    }

    suspend fun setFxUnifiedWithKeypad(unified: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.FX_UNIFIED_WITH_KEYPAD, unified).apply()
        context.dataStore.edit { it[Keys.FX_UNIFIED_WITH_KEYPAD] = unified }
    }

    suspend fun setKeyPressEffect(effect: KeyPressEffect) {
        syncPrefs.edit().putString(SyncKeys.KEY_PRESS_EFFECT, effect.name).apply()
        context.dataStore.edit { it[Keys.KEY_PRESS_EFFECT] = effect.name }
    }

    suspend fun setResultAnimStyle(style: ResultAnimStyle) {
        syncPrefs.edit().putString(SyncKeys.RESULT_ANIM_STYLE, style.name).apply()
        context.dataStore.edit { it[Keys.RESULT_ANIM_STYLE] = style.name }
    }

    suspend fun setPressSparksEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.PRESS_SPARKS_ENABLED, enabled).apply()
        context.dataStore.edit { it[Keys.PRESS_SPARKS_ENABLED] = enabled }
    }

    suspend fun setGlowBreathingEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean(SyncKeys.GLOW_BREATHING_ENABLED, enabled).apply()
        context.dataStore.edit { it[Keys.GLOW_BREATHING_ENABLED] = enabled }
    }

    suspend fun setThemeAnimStyle(style: ThemeAnimStyle) {
        syncPrefs.edit().putString(SyncKeys.THEME_ANIM_STYLE, style.name).apply()
        context.dataStore.edit { it[Keys.THEME_ANIM_STYLE] = style.name }
    }

    suspend fun setDisplayGlowStyle(style: DisplayGlowStyle) {
        syncPrefs.edit().putString(SyncKeys.DISPLAY_GLOW_STYLE, style.name).apply()
        context.dataStore.edit { it[Keys.DISPLAY_GLOW_STYLE] = style.name }
    }

    suspend fun setDisplayGlowIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.40f, 2.0f)
        syncPrefs.edit().putFloat(SyncKeys.DISPLAY_GLOW_INTENSITY, clamped).apply()
        context.dataStore.edit { it[Keys.DISPLAY_GLOW_INTENSITY] = clamped }
    }

    suspend fun setDisplayGlowColorPreset(preset: GlowColorPreset) {
        syncPrefs.edit().putString(SyncKeys.DISPLAY_GLOW_COLOR_PRESET, preset.name).apply()
        context.dataStore.edit { it[Keys.DISPLAY_GLOW_COLOR_PRESET] = preset.name }
    }

    suspend fun resetCustomizationsToDefaults() {
        syncPrefs.edit()
            .putFloat(SyncKeys.SOUND_VOLUME, DEFAULT_SOUND_VOLUME)
            .putString(SyncKeys.HAPTIC_INTENSITY, HapticIntensity.MEDIUM.name)
            .putString(SyncKeys.HAPTIC_STYLE, HapticStyle.ADAPTIVE.name)
            .putFloat(SyncKeys.HAPTIC_STRENGTH, DEFAULT_HAPTIC_STRENGTH)
            .putInt(SyncKeys.HAPTIC_DURATION_MS, DEFAULT_HAPTIC_DURATION_MS)
            .putBoolean(SyncKeys.HAPTIC_ACCENT_EQUALS, true)
            .putString(SyncKeys.BUTTON_SHAPE_STYLE, ButtonShapeStyle.PILL.name)
            .putBoolean(SyncKeys.USE_DYNAMIC_COLOR, true)
            .putLong(SyncKeys.CUSTOM_ACCENT_ARGB, DEFAULT_CUSTOM_ACCENT_ARGB)
            .putBoolean(SyncKeys.SHOW_LIVE_PREVIEW, true)
            .putBoolean(SyncKeys.AMBIENT_DISPLAY_GLOW, false)
            .putFloat(SyncKeys.KEYPAD_SCALE, DEFAULT_KEYPAD_SCALE)
            .putFloat(SyncKeys.FX_SCALE, DEFAULT_FX_SCALE)
            .putBoolean(SyncKeys.FX_UNIFIED_WITH_KEYPAD, false)
            .putString(SyncKeys.GLOW_TARGET, GlowTarget.ACCENT_KEYS.name)
            .putFloat(SyncKeys.GLOW_INTENSITY, DEFAULT_GLOW_INTENSITY)
            .putString(SyncKeys.GLOW_COLOR_PRESET, GlowColorPreset.AUTO.name)
            .putString(SyncKeys.KEY_PRESS_EFFECT, KeyPressEffect.SPRING.name)
            .putString(SyncKeys.RESULT_ANIM_STYLE, ResultAnimStyle.SLIDE_UP.name)
            .putBoolean(SyncKeys.PRESS_SPARKS_ENABLED, false)
            .putBoolean(SyncKeys.GLOW_BREATHING_ENABLED, false)
            .putString(SyncKeys.THEME_ANIM_STYLE, ThemeAnimStyle.CIRCLE_REVEAL.name)
            .putString(SyncKeys.DISPLAY_GLOW_STYLE, DisplayGlowStyle.AURORA.name)
            .putFloat(SyncKeys.DISPLAY_GLOW_INTENSITY, DEFAULT_GLOW_INTENSITY)
            .putString(SyncKeys.DISPLAY_GLOW_COLOR_PRESET, GlowColorPreset.AUTO.name)
            .apply()
        context.dataStore.edit {
            it[Keys.SOUND_VOLUME] = DEFAULT_SOUND_VOLUME
            it[Keys.HAPTIC_INTENSITY] = HapticIntensity.MEDIUM.name
            it[Keys.HAPTIC_STYLE] = HapticStyle.ADAPTIVE.name
            it[Keys.HAPTIC_STRENGTH] = DEFAULT_HAPTIC_STRENGTH
            it[Keys.HAPTIC_DURATION_MS] = DEFAULT_HAPTIC_DURATION_MS
            it[Keys.HAPTIC_ACCENT_EQUALS] = true
            it[Keys.BUTTON_SHAPE_STYLE] = ButtonShapeStyle.PILL.name
            it[Keys.USE_DYNAMIC_COLOR] = true
            it[Keys.CUSTOM_ACCENT_ARGB] = DEFAULT_CUSTOM_ACCENT_ARGB
            it[Keys.SHOW_LIVE_PREVIEW] = true
            it[Keys.AMBIENT_DISPLAY_GLOW] = false
            it[Keys.KEYPAD_SCALE] = DEFAULT_KEYPAD_SCALE
            it[Keys.FX_SCALE] = DEFAULT_FX_SCALE
            it[Keys.FX_UNIFIED_WITH_KEYPAD] = false
            it[Keys.GLOW_TARGET] = GlowTarget.ACCENT_KEYS.name
            it[Keys.GLOW_INTENSITY] = DEFAULT_GLOW_INTENSITY
            it[Keys.GLOW_COLOR_PRESET] = GlowColorPreset.AUTO.name
            it[Keys.KEY_PRESS_EFFECT] = KeyPressEffect.SPRING.name
            it[Keys.RESULT_ANIM_STYLE] = ResultAnimStyle.SLIDE_UP.name
            it[Keys.PRESS_SPARKS_ENABLED] = false
            it[Keys.GLOW_BREATHING_ENABLED] = false
            it[Keys.THEME_ANIM_STYLE] = ThemeAnimStyle.CIRCLE_REVEAL.name
            it[Keys.DISPLAY_GLOW_STYLE] = DisplayGlowStyle.AURORA.name
            it[Keys.DISPLAY_GLOW_INTENSITY] = DEFAULT_GLOW_INTENSITY
            it[Keys.DISPLAY_GLOW_COLOR_PRESET] = GlowColorPreset.AUTO.name
        }
    }
}
