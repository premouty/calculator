package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calc_prefs")

enum class AngleUnit {
    DEG,
    RAD
}

data class UserCalcPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val isScientificExpanded: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val SCIENTIFIC_EXPANDED = booleanPreferencesKey("scientific_expanded")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val preferencesFlow: Flow<UserCalcPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        val angle = prefs[Keys.ANGLE_UNIT]?.let {
            runCatching { AngleUnit.valueOf(it) }.getOrNull()
        } ?: AngleUnit.DEG

        val scientific = prefs[Keys.SCIENTIFIC_EXPANDED] ?: false
        val soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
        val vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true

        UserCalcPreferences(
            themeMode = theme,
            angleUnit = angle,
            isScientificExpanded = scientific,
            isSoundEnabled = soundEnabled,
            isVibrationEnabled = vibrationEnabled
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setScientificExpanded(expanded: Boolean) {
        context.dataStore.edit { it[Keys.SCIENTIFIC_EXPANDED] = expanded }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }
}
