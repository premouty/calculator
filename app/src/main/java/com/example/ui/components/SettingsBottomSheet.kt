package com.example.ui.components

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.audio.CalcSoundType
import com.example.audio.HapticIntensity
import com.example.audio.HapticStyle
import com.example.data.preferences.ButtonShapeStyle
import com.example.data.preferences.DisplayGlowStyle
import com.example.data.preferences.GlowColorPreset
import com.example.data.preferences.GlowTarget
import com.example.data.preferences.KeyPressEffect
import com.example.data.preferences.ResultAnimStyle
import com.example.data.preferences.ThemeAnimStyle
import com.example.ui.CalculatorUiState
import com.example.ui.theme.DEFAULT_CUSTOM_ACCENT_ARGB
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ACCENT_PRESETS: List<Long> = listOf(
    DEFAULT_CUSTOM_ACCENT_ARGB, // Indigo (default)
    0xFF0D9488L, // Teal / Emerald
    0xFFD946EFL, // Orchid / Violet
    0xFFE11D48L, // Rose
    0xFFD97706L, // Amber
    0xFF0284C7L  // Ocean Blue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    state: CalculatorUiState,
    onDismiss: () -> Unit,
    onToggleSound: () -> Unit,
    onChangeSoundVolume: (Float) -> Unit,
    onPreviewSoundVolume: () -> Unit,
    onToggleVibration: () -> Unit,
    onSelectHapticIntensity: (HapticIntensity) -> Unit,
    onSelectHapticStyle: (HapticStyle) -> Unit,
    onChangeHapticStrength: (Float) -> Unit,
    onChangeHapticDurationMs: (Int) -> Unit,
    onToggleHapticAccentEquals: (Boolean) -> Unit,
    onTestHaptic: (CalcSoundType) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectCustomAccentArgb: (Long) -> Unit,
    onSelectButtonShape: (ButtonShapeStyle) -> Unit,
    onChangeKeypadScale: (Float) -> Unit,
    onChangeFxScale: (Float) -> Unit,
    onToggleFxUnified: (Boolean) -> Unit,
    onSelectGlowTarget: (GlowTarget) -> Unit,
    onChangeGlowIntensity: (Float) -> Unit,
    onSelectGlowColorPreset: (GlowColorPreset) -> Unit,
    onSelectKeyPressEffect: (KeyPressEffect) -> Unit,
    onSelectResultAnimStyle: (ResultAnimStyle) -> Unit,
    onTogglePressSparks: (Boolean) -> Unit,
    onToggleGlowBreathing: (Boolean) -> Unit,
    onSelectThemeAnimStyle: (ThemeAnimStyle) -> Unit,
    onToggleLivePreview: (Boolean) -> Unit,
    onToggleAmbientDisplayGlow: (Boolean) -> Unit,
    onSelectDisplayGlowStyle: (DisplayGlowStyle) -> Unit,
    onChangeDisplayGlowIntensity: (Float) -> Unit,
    onSelectDisplayGlowColorPreset: (GlowColorPreset) -> Unit,
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var vibrationExpanded by remember { mutableStateOf(false) }

    val resetSpin = remember { Animatable(0f) }
    val resetCardPulse = remember { Animatable(1f) }
    var justReset by remember { mutableStateOf(false) }

    val isScrolled by remember {
        derivedStateOf { scrollState.value > 6 }
    }
    val headerDividerAlphaState = animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "settings_header_divider_alpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("settings_modal_sheet")
    ) {
        val sheetView = LocalView.current
        SideEffect {
            val dialogWindow = (sheetView.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                runCatching {
                    val dm = sheetView.context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                    val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching { sheetView.display }.getOrNull()
                    } else {
                        null
                    } ?: dm?.getDisplay(Display.DEFAULT_DISPLAY)
                    val currentMode = activeDisplay?.mode
                    val supportedModes = activeDisplay?.supportedModes
                    if (currentMode != null && !supportedModes.isNullOrEmpty()) {
                        val bestMode = supportedModes
                            .filter {
                                it.physicalWidth == currentMode.physicalWidth &&
                                    it.physicalHeight == currentMode.physicalHeight
                            }
                            .maxByOrNull { it.refreshRate }
                            ?: currentMode
                        val params = dialogWindow.attributes
                        if (params.preferredDisplayModeId != bestMode.modeId ||
                            params.preferredRefreshRate != bestMode.refreshRate
                        ) {
                            params.preferredDisplayModeId = bestMode.modeId
                            params.preferredRefreshRate = bestMode.refreshRate
                            dialogWindow.attributes = params
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Sticky Header: stays pinned when user swipes/scrolls down through settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("settings_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                    modifier = Modifier.graphicsLayer {
                        alpha = headerDividerAlphaState.value
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 18.dp)
                    .graphicsLayer {
                        val s = resetCardPulse.value
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Sound & Haptic Feedback
                SettingsSectionCard(title = stringResource(R.string.settings_section_feedback)) {
                    SettingsSwitchRow(
                        icon = if (state.isSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        title = stringResource(R.string.feedback_sound_label),
                        checked = state.isSoundEnabled,
                        onToggle = onToggleSound,
                        testTag = "settings_sound_switch"
                    )

                    AnimatedVisibility(
                        visible = state.isSoundEnabled,
                        enter = fadeIn(tween(180)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut(tween(140)) + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_sound_volume_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${(state.soundVolume * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = state.soundVolume,
                                onValueChange = onChangeSoundVolume,
                                onValueChangeFinished = onPreviewSoundVolume,
                                valueRange = 0.05f..1.0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_sound_volume_slider")
                            )
                        }
                    }

                    SettingsCardDivider()

                SettingsSwitchRow(
                    icon = null,
                    isVibrationSwitch = true,
                    title = stringResource(R.string.feedback_vibration_label),
                    checked = state.isVibrationEnabled,
                    onToggle = onToggleVibration,
                    testTag = "settings_vibration_switch"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (state.isVibrationEnabled) 1f else 0.72f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_haptic_intensity_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hapticOptions = listOf(
                            HapticIntensity.LIGHT to stringResource(R.string.settings_haptic_light),
                            HapticIntensity.MEDIUM to stringResource(R.string.settings_haptic_medium),
                            HapticIntensity.STRONG to stringResource(R.string.settings_haptic_strong)
                        )
                        hapticOptions.forEach { (intensity, label) ->
                            SettingsChoicePill(
                                label = label,
                                selected = state.hapticIntensity == intensity,
                                onClick = { onSelectHapticIntensity(intensity) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_haptic_${intensity.name.lowercase()}"
                            )
                        }
                    }
                }

                // Expandable detailed vibration controls (helpful for Xiaomi / non-Pixel motors)
                val chevronRotationState = animateFloatAsState(
                    targetValue = if (vibrationExpanded) 180f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "vibration_expand_chevron"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { vibrationExpanded = !vibrationExpanded }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("settings_haptic_advanced_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_haptic_advanced_toggle),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronRotationState.value }
                    )
                }

                AnimatedVisibility(
                    visible = vibrationExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (state.isVibrationEnabled) 1f else 0.72f)
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Haptic engine style
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.settings_haptic_style_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val styleOptions = listOf(
                                    HapticStyle.ADAPTIVE to stringResource(R.string.settings_haptic_style_adaptive),
                                    HapticStyle.SYSTEM to stringResource(R.string.settings_haptic_style_system),
                                    HapticStyle.PULSE to stringResource(R.string.settings_haptic_style_pulse)
                                )
                                styleOptions.forEach { (style, label) ->
                                    SettingsChoicePill(
                                        label = label,
                                        selected = state.hapticStyle == style,
                                        onClick = { onSelectHapticStyle(style) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "settings_haptic_style_${style.name.lowercase()}"
                                    )
                                }
                            }
                            Text(
                                text = when (state.hapticStyle) {
                                    HapticStyle.ADAPTIVE -> stringResource(R.string.settings_haptic_style_adaptive_hint)
                                    HapticStyle.SYSTEM -> stringResource(R.string.settings_haptic_style_system_hint)
                                    HapticStyle.PULSE -> stringResource(R.string.settings_haptic_style_pulse_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fine strength slider (active for ADAPTIVE and PULSE)
                        val strengthEnabled = state.hapticStyle != HapticStyle.SYSTEM
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (strengthEnabled) 1f else 0.45f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_haptic_strength_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${(state.hapticStrength * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = state.hapticStrength,
                                onValueChange = onChangeHapticStrength,
                                onValueChangeFinished = { onTestHaptic(CalcSoundType.DIGIT) },
                                enabled = strengthEnabled,
                                valueRange = 0.15f..1.0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_haptic_strength_slider")
                            )
                        }

                        // Fine pulse duration slider (4..40 ms for PULSE mode)
                        val durationEnabled = state.hapticStyle == HapticStyle.PULSE
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (durationEnabled) 1f else 0.45f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_haptic_duration_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_haptic_duration_value, state.hapticDurationMs),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = state.hapticDurationMs.toFloat(),
                                onValueChange = { onChangeHapticDurationMs(it.roundToInt()) },
                                onValueChangeFinished = { onTestHaptic(CalcSoundType.DIGIT) },
                                enabled = durationEnabled,
                                valueRange = 4f..40f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_haptic_duration_slider")
                            )
                        }

                        // Toggle distinct accent for '=' and operators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onToggleHapticAccentEquals(!state.hapticAccentEquals) }
                                .padding(vertical = 4.dp)
                                .testTag("settings_haptic_accent_equals_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_haptic_accent_equals_title),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_haptic_accent_equals_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.hapticAccentEquals,
                                onCheckedChange = onToggleHapticAccentEquals
                            )
                        }

                        // Instant haptic test buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsChoicePill(
                                label = stringResource(R.string.settings_haptic_test_digit),
                                icon = Icons.Rounded.TouchApp,
                                selected = false,
                                onClick = { onTestHaptic(CalcSoundType.DIGIT) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_haptic_test_digit_button"
                            )
                            SettingsChoicePill(
                                label = stringResource(R.string.settings_haptic_test_equals),
                                icon = Icons.Rounded.TouchApp,
                                selected = false,
                                onClick = { onTestHaptic(CalcSoundType.EQUALS) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_haptic_test_equals_button"
                            )
                        }
                    }
                }
            }

            // Section 2: Appearance & Core Preferences
            SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.theme_dark),
                            icon = Icons.Rounded.DarkMode,
                            selected = state.themeMode == ThemeMode.DARK,
                            onClick = { onSelectThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_dark"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.theme_light),
                            icon = Icons.Rounded.LightMode,
                            selected = state.themeMode == ThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_light"
                        )
                    }
                }

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.ColorLens,
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                    checked = state.useDynamicColor,
                    onToggle = { onToggleDynamicColor(!state.useDynamicColor) },
                    testTag = "settings_dynamic_color_switch"
                )

                AnimatedVisibility(
                    visible = !state.useDynamicColor,
                    enter = fadeIn(tween(200)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    CustomAccentColorEditor(
                        selectedAccentArgb = state.customAccentArgb,
                        onSelectAccentArgb = onSelectCustomAccentArgb
                    )
                }

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Visibility,
                    title = stringResource(R.string.settings_live_preview_title),
                    subtitle = stringResource(R.string.settings_live_preview_subtitle),
                    checked = state.showLivePreview,
                    onToggle = { onToggleLivePreview(!state.showLivePreview) },
                    testTag = "settings_live_preview_switch"
                )
            }

            // Dedicated Section: Customization & Animations Hub
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_section_customization_hub),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                )
            }

            // Hub Subsection A: Layout, Shapes & Sizes
            SettingsSectionCard(title = stringResource(R.string.settings_subsection_layout_shape)) {
                PanelLayoutPreviewCard(state = state)

                SettingsCardDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RoundedCorner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_button_shape_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val shapeOptions = listOf(
                            ButtonShapeStyle.PILL to stringResource(R.string.settings_shape_pill),
                            ButtonShapeStyle.SQUIRCLE to stringResource(R.string.settings_shape_squircle),
                            ButtonShapeStyle.SHARP to stringResource(R.string.settings_shape_sharp)
                        )
                        shapeOptions.forEach { (style, label) ->
                            SettingsChoicePill(
                                label = label,
                                selected = state.buttonShapeStyle == style,
                                onClick = { onSelectButtonShape(style) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_button_shape_${style.name.lowercase()}"
                            )
                        }
                    }
                }

                SettingsCardDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ViewAgenda,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_fx_mode_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_fx_mode_detached),
                            selected = !state.fxUnifiedWithKeypad,
                            onClick = { onToggleFxUnified(false) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_fx_mode_detached"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_fx_mode_unified),
                            selected = state.fxUnifiedWithKeypad,
                            onClick = { onToggleFxUnified(true) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_fx_mode_unified"
                        )
                    }
                    Text(
                        text = stringResource(
                            if (state.fxUnifiedWithKeypad) {
                                R.string.settings_fx_mode_unified_hint
                            } else {
                                R.string.settings_fx_mode_detached_hint
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SettingsCardDivider()

                SettingsScaleSliderRow(
                    icon = Icons.Rounded.ViewModule,
                    title = stringResource(R.string.settings_keypad_size_title),
                    value = state.keypadScale,
                    valueRange = 0.80f..1.18f,
                    onValueChange = onChangeKeypadScale,
                    testTag = "settings_keypad_scale_slider"
                )

                SettingsCardDivider()

                SettingsScaleSliderRow(
                    icon = Icons.Rounded.Functions,
                    title = stringResource(R.string.settings_fx_size_title),
                    value = state.fxScale,
                    valueRange = 0.78f..1.25f,
                    onValueChange = onChangeFxScale,
                    testTag = "settings_fx_scale_slider"
                )
            }

            // Hub Subsection B: Key Backlight & Display Glow Variations
            SettingsSectionCard(title = stringResource(R.string.settings_subsection_glow_display)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_glow_target_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val glowTargetOptions = listOf(
                            GlowTarget.ACCENT_KEYS to stringResource(R.string.settings_glow_target_accents),
                            GlowTarget.ALL_KEYS to stringResource(R.string.settings_glow_target_all),
                            GlowTarget.OFF to stringResource(R.string.settings_glow_target_off)
                        )
                        glowTargetOptions.forEach { (target, label) ->
                            SettingsChoicePill(
                                label = label,
                                selected = state.glowTarget == target,
                                onClick = { onSelectGlowTarget(target) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_glow_target_${target.name.lowercase()}"
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.glowTarget != GlowTarget.OFF,
                    enter = fadeIn(tween(180)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsCardDivider()

                        SettingsScaleSliderRow(
                            icon = Icons.Rounded.Height,
                            title = stringResource(R.string.settings_glow_intensity_title),
                            value = state.glowIntensity,
                            valueRange = 0.35f..1.65f,
                            onValueChange = onChangeGlowIntensity,
                            testTag = "settings_glow_intensity_slider"
                        )

                        SettingsCardDivider()

                        GlowColorPresetSelector(
                            title = stringResource(R.string.settings_glow_color_title),
                            selectedPreset = state.glowColorPreset,
                            onSelectPreset = onSelectGlowColorPreset,
                            testTagPrefix = "settings_glow_color"
                        )
                    }
                }

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_ambient_glow_title),
                    subtitle = stringResource(R.string.settings_ambient_glow_subtitle),
                    checked = state.ambientDisplayGlow,
                    onToggle = { onToggleAmbientDisplayGlow(!state.ambientDisplayGlow) },
                    testTag = "settings_ambient_glow_switch"
                )

                AnimatedVisibility(
                    visible = state.ambientDisplayGlow,
                    enter = fadeIn(tween(180)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsCardDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_display_glow_style_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_aurora),
                                    selected = state.displayGlowStyle == DisplayGlowStyle.AURORA,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.AURORA) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_aurora"
                                )
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_neon_frame),
                                    selected = state.displayGlowStyle == DisplayGlowStyle.NEON_FRAME,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.NEON_FRAME) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_neon_frame"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_prism),
                                    selected = state.displayGlowStyle == DisplayGlowStyle.PRISM_WAVE,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.PRISM_WAVE) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_prism_wave"
                                )
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_horizon),
                                    selected = state.displayGlowStyle == DisplayGlowStyle.BOTTOM_HORIZON,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.BOTTOM_HORIZON) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_bottom_horizon"
                                )
                            }
                        }

                        SettingsCardDivider()

                        SettingsScaleSliderRow(
                            icon = Icons.Rounded.Flare,
                            title = stringResource(R.string.settings_display_glow_intensity_title),
                            value = state.displayGlowIntensity,
                            valueRange = 0.40f..2.0f,
                            onValueChange = onChangeDisplayGlowIntensity,
                            testTag = "settings_display_glow_intensity_slider"
                        )

                        SettingsCardDivider()

                        GlowColorPresetSelector(
                            title = stringResource(R.string.settings_display_glow_color_title),
                            selectedPreset = state.displayGlowColorPreset,
                            onSelectPreset = onSelectDisplayGlowColorPreset,
                            testTagPrefix = "settings_display_glow_color"
                        )
                    }
                }
            }

            // Hub Subsection C: Animations & Effects (including Theme transition animation)
            SettingsSectionCard(title = stringResource(R.string.settings_subsection_animations)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_theme_anim_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_theme_anim_circle),
                            selected = state.themeAnimStyle == ThemeAnimStyle.CIRCLE_REVEAL,
                            onClick = { onSelectThemeAnimStyle(ThemeAnimStyle.CIRCLE_REVEAL) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_anim_circle"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_theme_anim_crossfade),
                            selected = state.themeAnimStyle == ThemeAnimStyle.CROSSFADE,
                            onClick = { onSelectThemeAnimStyle(ThemeAnimStyle.CROSSFADE) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_anim_crossfade"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_theme_anim_diagonal),
                            selected = state.themeAnimStyle == ThemeAnimStyle.DIAGONAL_WIPE,
                            onClick = { onSelectThemeAnimStyle(ThemeAnimStyle.DIAGONAL_WIPE) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_anim_diagonal"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_theme_anim_curtain),
                            selected = state.themeAnimStyle == ThemeAnimStyle.VERTICAL_CURTAIN,
                            onClick = { onSelectThemeAnimStyle(ThemeAnimStyle.VERTICAL_CURTAIN) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_anim_curtain"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_theme_anim_instant),
                            selected = state.themeAnimStyle == ThemeAnimStyle.INSTANT,
                            onClick = { onSelectThemeAnimStyle(ThemeAnimStyle.INSTANT) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_theme_anim_instant"
                        )
                    }
                }

                SettingsCardDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Animation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_key_effect_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_key_effect_spring),
                            selected = state.keyPressEffect == KeyPressEffect.SPRING,
                            onClick = { onSelectKeyPressEffect(KeyPressEffect.SPRING) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_key_effect_spring"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_key_effect_shockwave),
                            selected = state.keyPressEffect == KeyPressEffect.SHOCKWAVE,
                            onClick = { onSelectKeyPressEffect(KeyPressEffect.SHOCKWAVE) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_key_effect_shockwave"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_key_effect_neon),
                            selected = state.keyPressEffect == KeyPressEffect.NEON_BURST,
                            onClick = { onSelectKeyPressEffect(KeyPressEffect.NEON_BURST) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_key_effect_neon_burst"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_key_effect_tilt),
                            selected = state.keyPressEffect == KeyPressEffect.SOFT_TILT,
                            onClick = { onSelectKeyPressEffect(KeyPressEffect.SOFT_TILT) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_key_effect_soft_tilt"
                        )
                    }
                }

                SettingsCardDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_result_anim_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val resultAnimOptions = listOf(
                            ResultAnimStyle.SLIDE_UP to stringResource(R.string.settings_result_anim_slide),
                            ResultAnimStyle.ELASTIC_POP to stringResource(R.string.settings_result_anim_pop),
                            ResultAnimStyle.SHIMMER_SWEEP to stringResource(R.string.settings_result_anim_shimmer)
                        )
                        resultAnimOptions.forEach { (style, label) ->
                            SettingsChoicePill(
                                label = label,
                                selected = state.resultAnimStyle == style,
                                onClick = { onSelectResultAnimStyle(style) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_result_anim_${style.name.lowercase()}"
                            )
                        }
                    }
                }

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_press_sparks_title),
                    subtitle = stringResource(R.string.settings_press_sparks_subtitle),
                    checked = state.pressSparksEnabled,
                    onToggle = { onTogglePressSparks(!state.pressSparksEnabled) },
                    testTag = "settings_press_sparks_switch"
                )

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Flare,
                    title = stringResource(R.string.settings_glow_breathing_title),
                    subtitle = stringResource(R.string.settings_glow_breathing_subtitle),
                    checked = state.glowBreathingEnabled,
                    onToggle = { onToggleGlowBreathing(!state.glowBreathingEnabled) },
                    testTag = "settings_glow_breathing_switch"
                )
            }

            // Animated Reset Pill
            val resetBg by animateColorAsState(
                targetValue = if (justReset) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(240),
                label = "reset_pill_bg"
            )
            val resetFg by animateColorAsState(
                targetValue = if (justReset) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                animationSpec = tween(240),
                label = "reset_pill_fg"
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = resetBg,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (justReset) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable {
                            onResetDefaults()
                            scope.launch {
                                justReset = true
                                launch {
                                    val start = resetSpin.value
                                    resetSpin.animateTo(
                                        targetValue = start - 360f,
                                        animationSpec = spring(
                                            dampingRatio = 0.65f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                launch {
                                    resetCardPulse.animateTo(
                                        0.985f,
                                        tween(95, easing = CubicBezierEasing(0.2f, 0f, 0.4f, 1f))
                                    )
                                    resetCardPulse.animateTo(
                                        1f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                delay(950L)
                                justReset = false
                            }
                        }
                        .testTag("settings_reset_defaults_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = justReset,
                            transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(initialScale = 0.6f)) togetherWith
                                    (fadeOut(tween(120)) + scaleOut(targetScale = 0.6f))
                            },
                            label = "reset_icon_state"
                        ) { resetConfirmed ->
                            if (resetConfirmed) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = resetFg,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    tint = resetFg,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = resetSpin.value }
                                )
                            }
                        }
                        AnimatedContent(
                            targetState = justReset,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                            },
                            label = "reset_text_state"
                        ) { resetConfirmed ->
                            Text(
                                text = stringResource(
                                    if (resetConfirmed) R.string.settings_reset_done else R.string.settings_reset_defaults
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = resetFg
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun CustomAccentColorEditor(
    selectedAccentArgb: Long,
    onSelectAccentArgb: (Long) -> Unit
) {
    val activeColor = Color(selectedAccentArgb.toInt())
    var lastEmittedArgb by remember { mutableStateOf<Long?>(null) }
    var hue by remember {
        mutableFloatStateOf(
            FloatArray(3).also { android.graphics.Color.colorToHSV(selectedAccentArgb.toInt(), it) }[0]
        )
    }
    var saturation by remember {
        mutableFloatStateOf(
            FloatArray(3).also { android.graphics.Color.colorToHSV(selectedAccentArgb.toInt(), it) }[1]
                .coerceIn(0.25f, 0.96f)
        )
    }

    androidx.compose.runtime.LaunchedEffect(selectedAccentArgb) {
        if (selectedAccentArgb != lastEmittedArgb) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(selectedAccentArgb.toInt(), hsv)
            hue = hsv[0]
            saturation = hsv[1].coerceIn(0.25f, 0.96f)
            lastEmittedArgb = selectedAccentArgb
        }
    }

    val hexLabel = remember(selectedAccentArgb) {
        String.format("#%06X", selectedAccentArgb and 0xFFFFFFL)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_custom_color_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                    Text(
                        text = hexLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Quick preset swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ACCENT_PRESETS.forEach { presetArgb ->
                val swatchColor = Color(presetArgb.toInt())
                val isSelected = presetArgb == selectedAccentArgb
                val swatchScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "swatch_scale"
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = swatchScale
                            scaleY = swatchScale
                        }
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            },
                            shape = CircleShape
                        )
                        .clickable {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(presetArgb.toInt(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1].coerceIn(0.25f, 0.96f)
                            lastEmittedArgb = presetArgb
                            onSelectAccentArgb(presetArgb)
                        }
                        .testTag("settings_color_preset_${presetArgb.toString(16)}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (swatchColor.luminance() > 0.55f) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Custom Hue rainbow slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.settings_custom_color_hue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val hueBrush = remember {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(hueBrush)
                )
                Slider(
                    value = hue,
                    onValueChange = { newHue ->
                        hue = newHue
                        val packed = android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturation, 0.85f))
                        val argb = packed.toLong() and 0xFFFFFFFFL
                        lastEmittedArgb = argb
                        onSelectAccentArgb(argb)
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = activeColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_custom_hue_slider")
                )
            }
        }

        // Custom Saturation / Richness slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.settings_custom_color_saturation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(contentAlignment = Alignment.Center) {
                val lowSatColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.25f, 0.85f)))
                val highSatColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.96f, 0.85f)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(lowSatColor, highSatColor)))
                )
                Slider(
                    value = saturation,
                    onValueChange = { newSat ->
                        saturation = newSat
                        val packed = android.graphics.Color.HSVToColor(floatArrayOf(hue, newSat, 0.85f))
                        val argb = packed.toLong() and 0xFFFFFFFFL
                        lastEmittedArgb = argb
                        onSelectAccentArgb(argb)
                    },
                    valueRange = 0.25f..0.96f,
                    colors = SliderDefaults.colors(
                        thumbColor = activeColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_custom_sat_slider")
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsCardDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector?,
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    isVibrationSwitch: Boolean = false,
    subtitle: String? = null
) {
    val badgeBg by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "switch_badge_bg"
    )
    val badgeTint by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "switch_badge_tint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            if (isVibrationSwitch) {
                VibrationStatusIcon(
                    enabled = checked,
                    tint = badgeTint,
                    gapColor = badgeBg,
                    modifier = Modifier.size(19.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun SettingsChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "choice_pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "choice_pill_fg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "choice_pill_scale"
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
        },
        modifier = modifier
            .height(40.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsScaleSliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "${(value * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun GlowColorPresetSelector(
    title: String,
    selectedPreset: GlowColorPreset,
    onSelectPreset: (GlowColorPreset) -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowColorPreset.entries.forEach { preset ->
                val isSelected = selectedPreset == preset
                val swatchColor = preset.argb?.let { Color(it.toInt()) }
                    ?: MaterialTheme.colorScheme.primary
                val swatchScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "${testTagPrefix}_scale"
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = swatchScale
                            scaleY = swatchScale
                        }
                        .clip(CircleShape)
                        .background(
                            if (preset == GlowColorPreset.AUTO) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(swatchColor, swatchColor))
                            }
                        )
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            },
                            shape = CircleShape
                        )
                        .clickable { onSelectPreset(preset) }
                        .testTag("${testTagPrefix}_${preset.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (swatchColor.luminance() > 0.58f) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (preset == GlowColorPreset.AUTO) {
                        Text(
                            text = stringResource(R.string.settings_glow_color_auto).take(1),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelLayoutPreviewCard(state: CalculatorUiState) {
    var previewFxOpen by remember { mutableStateOf(true) }
    val fxOpenMixState = animateFloatAsState(
        targetValue = if (previewFxOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = 580f
        ),
        label = "preview_fx_open_mix"
    )
    val unifiedMixState = animateFloatAsState(
        targetValue = if (state.fxUnifiedWithKeypad) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = 520f
        ),
        label = "preview_unified_mix"
    )

    val bgScreenColor = MaterialTheme.colorScheme.background
    val displayCardColor = MaterialTheme.colorScheme.surfaceContainer
    val unifiedDeckColor = MaterialTheme.colorScheme.surfaceContainerLow
    val digitKeyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val operatorKeyColor = MaterialTheme.colorScheme.secondaryContainer
    val utilityKeyColor = MaterialTheme.colorScheme.tertiaryContainer
    val equalsKeyColor = MaterialTheme.colorScheme.primary
    val fxModeKeyColor = MaterialTheme.colorScheme.secondaryContainer
    val fxNormalKeyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fxChipColor = if (previewFxOpen) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val voiceChipColor = MaterialTheme.colorScheme.tertiaryContainer
    val displayTextLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    val displaySubLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val outlineTint = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val resolvedKeyGlowColor = state.glowColorPreset.argb?.let { Color(it.toInt()) }
    val resolvedDisplayGlowColor = state.displayGlowColorPreset.argb?.let { Color(it.toInt()) }

    val cornerFactor = when (state.buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 0.50f
        ButtonShapeStyle.SQUIRCLE -> 0.28f
        ButtonShapeStyle.SHARP -> 0.14f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_layout_preview_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // True portrait phone-proportioned single-Canvas interactive mockup (144x256 dp)
            Box(
                modifier = Modifier
                    .width(144.dp)
                    .height(256.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgScreenColor)
                    .border(
                        width = 1.5.dp,
                        color = outlineTint.copy(alpha = 0.48f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable { previewFxOpen = !previewFxOpen }
                    .testTag("settings_layout_preview_canvas")
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pFx = fxOpenMixState.value.coerceIn(0f, 1f)
                    val uMix = unifiedMixState.value.coerceIn(0f, 1f)
                    val padX = 9.dp.toPx()
                    val padTop = 7.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val innerW = (size.width - padX * 2f).coerceAtLeast(10f)

                    val unit = size.height / 256f
                    val kScale = state.keypadScale.coerceIn(0.80f, 1.18f)
                    val fScale = state.fxScale.coerceIn(0.78f, 1.25f)

                    val baseRowH = 16.5f * unit * kScale
                    val baseRowGap = 3.2f * unit * (0.85f + 0.15f * kScale)
                    val compressedRatio = 1f - 0.22f * uMix * pFx
                    val rowH = (baseRowH * compressedRatio).coerceAtLeast(11.5f * unit)
                    val rowGap = (baseRowGap * (1f - 0.16f * uMix * pFx)).coerceAtLeast(2.0f * unit)

                    val keypadH = rowH * 5f + rowGap * 4f
                    val fxH = 39f * unit * fScale
                    val fxGap = (3.5f + 1.5f * uMix) * unit
                    val stripH = 11.5f * unit
                    val fxVisibleH = (fxH + fxGap) * pFx

                    val keypadTop = size.height - padBottom - keypadH
                    val deckTop = keypadTop - fxVisibleH
                    val stripTop = (deckTop - stripH - 3.dp.toPx()).coerceAtLeast(padTop + 42f * unit)
                    val displayBottom = (stripTop - 2.dp.toPx()).coerceAtLeast(padTop + 36f * unit)

                    // 1. Top Display Card + Live Ambient Glow preview
                    val displayCorner = 13.dp.toPx()
                    drawRoundRect(
                        color = displayCardColor,
                        topLeft = Offset(0f, -displayCorner),
                        size = Size(size.width, displayBottom + displayCorner),
                        cornerRadius = CornerRadius(displayCorner, displayCorner)
                    )

                    if (state.ambientDisplayGlow) {
                        val dGlowMul = state.displayGlowIntensity.coerceIn(0.40f, 2.0f)
                        val ambPrimary = resolvedDisplayGlowColor ?: primaryColor
                        val ambTertiary = resolvedDisplayGlowColor?.let {
                            lerp(it, tertiaryColor, 0.42f)
                        } ?: tertiaryColor

                        when (state.displayGlowStyle) {
                            DisplayGlowStyle.AURORA -> {
                                val c1 = Offset(size.width * 0.25f, displayBottom * 0.36f)
                                val r1 = size.width * 0.56f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to ambPrimary.copy(alpha = (0.34f * dGlowMul).coerceIn(0f, 0.70f)),
                                        1f to Color.Transparent,
                                        center = c1,
                                        radius = r1
                                    ),
                                    center = c1,
                                    radius = r1
                                )
                                val c2 = Offset(size.width * 0.78f, displayBottom * 0.72f)
                                val r2 = size.width * 0.48f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to ambTertiary.copy(alpha = (0.28f * dGlowMul).coerceIn(0f, 0.60f)),
                                        1f to Color.Transparent,
                                        center = c2,
                                        radius = r2
                                    ),
                                    center = c2,
                                    radius = r2
                                )
                            }
                            DisplayGlowStyle.NEON_FRAME -> {
                                drawRoundRect(
                                    color = ambPrimary.copy(alpha = (0.65f * dGlowMul).coerceIn(0f, 0.92f)),
                                    topLeft = Offset(1.dp.toPx(), -displayCorner),
                                    size = Size(size.width - 2.dp.toPx(), displayBottom + displayCorner - 1.dp.toPx()),
                                    cornerRadius = CornerRadius(displayCorner, displayCorner),
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
                            }
                            DisplayGlowStyle.PRISM_WAVE -> {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        0f to ambPrimary.copy(alpha = (0.30f * dGlowMul).coerceIn(0f, 0.58f)),
                                        0.5f to ambTertiary.copy(alpha = (0.22f * dGlowMul).coerceIn(0f, 0.46f)),
                                        1f to Color.Transparent
                                    ),
                                    size = Size(size.width, displayBottom)
                                )
                            }
                            DisplayGlowStyle.BOTTOM_HORIZON -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.38f to Color.Transparent,
                                        1f to ambPrimary.copy(alpha = (0.44f * dGlowMul).coerceIn(0f, 0.75f))
                                    ),
                                    size = Size(size.width, displayBottom)
                                )
                            }
                        }
                    }

                    // Mini top utility pills inside display
                    val dotR = 2.6.dp.toPx()
                    drawCircle(
                        color = outlineTint.copy(alpha = 0.55f),
                        radius = dotR,
                        center = Offset(padX + dotR, padTop + 5.dp.toPx())
                    )
                    drawCircle(
                        color = outlineTint.copy(alpha = 0.45f),
                        radius = dotR,
                        center = Offset(size.width - padX - dotR, padTop + 5.dp.toPx())
                    )
                    drawCircle(
                        color = outlineTint.copy(alpha = 0.45f),
                        radius = dotR,
                        center = Offset(size.width - padX - dotR * 3.8f, padTop + 5.dp.toPx())
                    )

                    // Schematic expression & result lines inside mini display
                    val lineRight = size.width - padX - 2.dp.toPx()
                    val resLineH = (8.5f * unit).coerceAtMost((displayBottom - padTop) * 0.24f).coerceAtLeast(4f)
                    val exprLineH = (3.8f * unit).coerceAtMost((displayBottom - padTop) * 0.12f).coerceAtLeast(2.2f)
                    val resW = innerW * 0.48f
                    val exprW = innerW * 0.30f
                    val resY = (displayBottom - 8.dp.toPx() - resLineH).coerceAtLeast(padTop + 14.dp.toPx())
                    if (resY - exprLineH - 3.dp.toPx() >= padTop + 10.dp.toPx()) {
                        drawRoundRect(
                            color = displaySubLineColor,
                            topLeft = Offset(lineRight - exprW, resY - exprLineH - 3.dp.toPx()),
                            size = Size(exprW, exprLineH),
                            cornerRadius = CornerRadius(exprLineH / 2f, exprLineH / 2f)
                        )
                    }
                    drawRoundRect(
                        color = displayTextLineColor,
                        topLeft = Offset(lineRight - resW, resY),
                        size = Size(resW, resLineH),
                        cornerRadius = CornerRadius(resLineH / 2f, resLineH / 2f)
                    )

                    // 2. Mini Toggle Strip (fx + Voice)
                    val chipW = 24.dp.toPx()
                    val chipH = 9.dp.toPx()
                    val chipY = stripTop + (stripH - chipH) * 0.5f
                    drawRoundRect(
                        color = fxChipColor,
                        topLeft = Offset(padX, chipY),
                        size = Size(chipW, chipH),
                        cornerRadius = CornerRadius(chipH / 2f, chipH / 2f)
                    )
                    drawRoundRect(
                        color = voiceChipColor,
                        topLeft = Offset(size.width - padX - chipW * 1.25f, chipY),
                        size = Size(chipW * 1.25f, chipH),
                        cornerRadius = CornerRadius(chipH / 2f, chipH / 2f)
                    )

                    // 3. Unified deck backing card drawn BEHIND both fx & keypad when unified
                    val deckAlpha = uMix * pFx
                    if (deckAlpha > 0.02f) {
                        val deckPadX = 4.dp.toPx()
                        val deckPadY = 4.dp.toPx()
                        val deckCorner = 12.dp.toPx()
                        drawRoundRect(
                            color = unifiedDeckColor.copy(alpha = 0.94f * deckAlpha),
                            topLeft = Offset(padX - deckPadX, deckTop - deckPadY),
                            size = Size(
                                innerW + deckPadX * 2f,
                                (size.height - padBottom - deckTop) + deckPadY * 2f
                            ),
                            cornerRadius = CornerRadius(deckCorner, deckCorner)
                        )
                        drawRoundRect(
                            color = outlineTint.copy(alpha = 0.30f * deckAlpha),
                            topLeft = Offset(padX - deckPadX, deckTop - deckPadY),
                            size = Size(
                                innerW + deckPadX * 2f,
                                (size.height - padBottom - deckTop) + deckPadY * 2f
                            ),
                            cornerRadius = CornerRadius(deckCorner, deckCorner),
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                        val sepY = keypadTop - fxGap * 0.5f
                        drawLine(
                            color = outlineTint.copy(alpha = 0.28f * deckAlpha),
                            start = Offset(padX + 2.dp.toPx(), sepY),
                            end = Offset(size.width - padX - 2.dp.toPx(), sepY),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    // 4. Mini 5x3 fx Grid
                    val glowIntensity = state.glowIntensity.coerceIn(0.35f, 1.65f)
                    if (pFx > 0.01f) {
                        clipRect(
                            left = 0f,
                            top = deckTop.coerceAtLeast(0f),
                            right = size.width,
                            bottom = keypadTop
                        ) {
                            val fxCols = 5
                            val fxRows = 3
                            val fxCellGap = 2.6f * unit
                            val fxCellW = (innerW - fxCellGap * (fxCols - 1)) / fxCols
                            val fxCellH = (fxH - fxCellGap * (fxRows - 1)) / fxRows
                            val fxCorner = (minOf(fxCellW, fxCellH) * cornerFactor).coerceAtMost(fxCellH * 0.5f)
                            val fxTop = keypadTop - fxGap - fxH + (1f - pFx) * (fxH * 0.25f)

                            for (r in 0 until fxRows) {
                                for (c in 0 until fxCols) {
                                    val x = padX + c * (fxCellW + fxCellGap)
                                    val y = fxTop + r * (fxCellH + fxCellGap)
                                    val tileColor = when {
                                        r == 0 && c == 1 -> fxModeKeyColor
                                        else -> fxNormalKeyColor
                                    }.copy(alpha = pFx)
                                    drawRoundRect(
                                        color = tileColor,
                                        topLeft = Offset(x, y),
                                        size = Size(fxCellW, fxCellH),
                                        cornerRadius = CornerRadius(fxCorner, fxCorner)
                                    )
                                    if (state.glowTarget == GlowTarget.ALL_KEYS) {
                                        val rimCol = (resolvedKeyGlowColor ?: primaryColor)
                                            .copy(alpha = (0.34f * glowIntensity * pFx).coerceIn(0f, 0.72f))
                                        drawRoundRect(
                                            color = rimCol,
                                            topLeft = Offset(x, y),
                                            size = Size(fxCellW, fxCellH),
                                            cornerRadius = CornerRadius(fxCorner, fxCorner),
                                            style = Stroke(width = 0.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Mini 4x5 Standard Keypad Grid
                    val kCols = 4
                    val kRows = 5
                    val colGap = 3.2f * unit
                    val cellW = (innerW - colGap * (kCols - 1)) / kCols
                    val keyCorner = (minOf(cellW, rowH) * cornerFactor).coerceAtMost(rowH * 0.5f)

                    for (r in 0 until kRows) {
                        for (c in 0 until kCols) {
                            val x = padX + c * (cellW + colGap)
                            val y = keypadTop + r * (rowH + rowGap)
                            val isEquals = r == 4 && c == 3
                            val isUtility = (r == 0 && c == 0) || (r == 4 && c == 2)
                            val isOperator = (c == 3 && !isEquals) || (r == 0 && c in 1..2)
                            val hasMiniGlow = when (state.glowTarget) {
                                GlowTarget.OFF -> false
                                GlowTarget.ACCENT_KEYS -> isEquals || isUtility
                                GlowTarget.ALL_KEYS -> true
                            }
                            if (hasMiniGlow) {
                                val rawGlow = resolvedKeyGlowColor ?: when {
                                    isUtility -> tertiaryColor
                                    else -> primaryColor
                                }
                                val spread = 2.2f.dp.toPx() * (0.7f + 0.3f * glowIntensity)
                                val roleAlpha = if (isEquals || isUtility) 0.40f else 0.22f
                                drawRoundRect(
                                    color = rawGlow.copy(alpha = (roleAlpha * glowIntensity).coerceIn(0f, 0.75f)),
                                    topLeft = Offset(x - spread, y - spread),
                                    size = Size(cellW + spread * 2f, rowH + spread * 2f),
                                    cornerRadius = CornerRadius(keyCorner + spread, keyCorner + spread)
                                )
                            }

                            val keyColor = when {
                                isEquals -> equalsKeyColor
                                isUtility -> utilityKeyColor
                                isOperator -> operatorKeyColor
                                else -> digitKeyColor
                            }
                            drawRoundRect(
                                color = keyColor,
                                topLeft = Offset(x, y),
                                size = Size(cellW, rowH),
                                cornerRadius = CornerRadius(keyCorner, keyCorner)
                            )
                        }
                    }

                    // Home gesture indicator pill
                    val navW = size.width * 0.30f
                    val navH = 2.2.dp.toPx()
                    drawRoundRect(
                        color = outlineTint.copy(alpha = 0.55f),
                        topLeft = Offset((size.width - navW) * 0.5f, size.height - 4.5.dp.toPx()),
                        size = Size(navW, navH),
                        cornerRadius = CornerRadius(navH / 2f, navH / 2f)
                    )
                }
            }

            // Right telemetry & quick fx-state toggle column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (previewFxOpen) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (previewFxOpen) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            outlineTint.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(CircleShape)
                        .clickable { previewFxOpen = !previewFxOpen }
                        .testTag("settings_layout_preview_toggle_fx")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                if (previewFxOpen) {
                                    R.string.settings_layout_preview_fx_open
                                } else {
                                    R.string.settings_layout_preview_fx_closed
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (previewFxOpen) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                PreviewMetricPill(
                    label = stringResource(R.string.settings_fx_mode_title),
                    value = stringResource(
                        if (state.fxUnifiedWithKeypad) {
                            R.string.settings_fx_mode_unified
                        } else {
                            R.string.settings_fx_mode_detached
                        }
                    )
                )

                PreviewMetricPill(
                    label = stringResource(R.string.settings_keypad_size_title),
                    value = "${(state.keypadScale * 100f).roundToInt()}%"
                )

                PreviewMetricPill(
                    label = stringResource(R.string.settings_fx_size_title),
                    value = "${(state.fxScale * 100f).roundToInt()}%"
                )

                Text(
                    text = stringResource(R.string.settings_layout_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewMetricPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
