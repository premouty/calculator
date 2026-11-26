package com.example

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.CalculatorViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.theme.ExpressiveBackgroundDark
import com.example.ui.theme.ExpressiveBackgroundLight
import com.example.ui.theme.ExpressiveCalcTheme
import com.example.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private var displayManager: DisplayManager? = null
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = applyHighestSupportedRefreshRate()
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = applyHighestSupportedRefreshRate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)

        val initialPrefs = UserPreferencesRepository(applicationContext).readInitialPreferencesSync()
        val initialDark = initialPrefs.themeMode == ThemeMode.DARK

        window.setBackgroundDrawable(
            (if (initialDark) ExpressiveBackgroundDark else ExpressiveBackgroundLight)
                .toArgb()
                .toDrawable()
        )

        val barStyle = if (initialDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !initialDark
            isAppearanceLightNavigationBars = !initialDark
        }

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        setContent {
            val calculatorViewModel: CalculatorViewModel = viewModel()
            val uiState by calculatorViewModel.uiState.collectAsStateWithLifecycle()

            ExpressiveCalcTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.useDynamicColor,
                customAccentArgb = uiState.customAccentArgb
            ) {
                CalculatorScreen(
                    state = uiState,
                    viewModel = calculatorViewModel
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyHighestSupportedRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        applyHighestSupportedRefreshRate()
    }

    override fun onDestroy() {
        displayManager?.unregisterDisplayListener(displayListener)
        super.onDestroy()
    }

    private fun applyHighestSupportedRefreshRate() {
        try {
            val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { display }.getOrNull()
            } else {
                null
            } ?: displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
                ?: return

            val currentMode = activeDisplay.mode ?: return
            val supportedModes = activeDisplay.supportedModes
            if (supportedModes.isNullOrEmpty()) return

            val bestMode = supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: currentMode

            val params = window.attributes
            var updated = false
            if (params.preferredDisplayModeId != bestMode.modeId) {
                params.preferredDisplayModeId = bestMode.modeId
                updated = true
            }
            if (params.preferredRefreshRate != bestMode.refreshRate) {
                params.preferredRefreshRate = bestMode.refreshRate
                updated = true
            }
            if (updated) {
                window.attributes = params
            }
        } catch (_: Throwable) {
            // Gracefully ignore in headless/test environments without full DisplayManager modes
        }
    }
}

