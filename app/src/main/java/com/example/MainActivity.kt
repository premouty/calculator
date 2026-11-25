package com.example

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.CalculatorViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.theme.ExpressiveCalcTheme

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
        enableEdgeToEdge()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        setContent {
            val calculatorViewModel: CalculatorViewModel = viewModel()
            val uiState by calculatorViewModel.uiState.collectAsStateWithLifecycle()

            ExpressiveCalcTheme(
                themeMode = uiState.themeMode,
                dynamicColor = true
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

