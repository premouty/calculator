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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.audio.HapticIntensity
import com.example.data.preferences.ButtonShapeStyle
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
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectCustomAccentArgb: (Long) -> Unit,
    onSelectButtonShape: (ButtonShapeStyle) -> Unit,
    onToggleLivePreview: (Boolean) -> Unit,
    onToggleAmbientDisplayGlow: (Boolean) -> Unit,
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

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
                        .padding(top = 18.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
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
                            color = MaterialTheme.colorScheme.onSurface
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (state.isSoundEnabled) 1f else 0.65f)
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
            }

            // Section 2: Appearance
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
            }

            // Section 3: Optional Extras (can be turned on/off)
            SettingsSectionCard(title = stringResource(R.string.settings_section_extras)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_ambient_glow_title),
                    subtitle = stringResource(R.string.settings_ambient_glow_subtitle),
                    checked = state.ambientDisplayGlow,
                    onToggle = { onToggleAmbientDisplayGlow(!state.ambientDisplayGlow) },
                    testTag = "settings_ambient_glow_switch"
                )

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
