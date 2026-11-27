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

enum class AngleUnit {
    DEG,
    RAD
}

enum class ButtonShapeStyle {
    PILL,
    SQUIRCLE,
    SHARP
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
    val ambientDisplayGlow: Boolean = false
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
            ambientDisplayGlow = syncPrefs.getBoolean(SyncKeys.AMBIENT_DISPLAY_GLOW, false)
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
            ambientDisplayGlow = ambientDisplayGlow
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
        }
    }
}
