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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.audio.CalcSoundType
import com.example.audio.HapticIntensity
import com.example.data.preferences.ButtonShapeStyle
import com.example.data.preferences.DisplayGlowStyle
import com.example.data.preferences.DynamicColorSource
import com.example.data.preferences.DynamicColorVibrancy
import com.example.data.preferences.ExpressiveTouchStyle
import com.example.data.preferences.GlowColorPreset
import com.example.data.preferences.GlowTarget
import com.example.data.preferences.ResultAnimStyle
import com.example.ui.CalculatorUiState
import com.example.ui.theme.DEFAULT_CUSTOM_ACCENT_ARGB
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onDismiss: () -> Unit,
    onSaveScrollPosition: (Int, Int) -> Unit,
    onToggleVibrationExpanded: (Boolean) -> Unit,
    onToggleDynamicColorExpanded: (Boolean) -> Unit,
    onSelectDesignTab: (Int) -> Unit,
    onToggleSound: () -> Unit,
    onChangeSoundVolume: (Float) -> Unit,
    onPreviewSoundVolume: () -> Unit,
    onToggleVibration: () -> Unit,
    onSelectHapticIntensity: (HapticIntensity) -> Unit,
    onToggleHapticAccentEquals: (Boolean) -> Unit,
    onTestHaptic: (CalcSoundType) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectDynamicColorSource: (DynamicColorSource) -> Unit,
    onSelectDynamicColorVibrancy: (DynamicColorVibrancy) -> Unit,
    onToggleColorTintOperators: (Boolean) -> Unit,
    onToggleColorTintUtilities: (Boolean) -> Unit,
    onToggleColorTintDigits: (Boolean) -> Unit,
    onToggleColorTintFx: (Boolean) -> Unit,
    onSelectCustomAccentArgb: (Long) -> Unit,
    onSelectButtonShape: (ButtonShapeStyle) -> Unit,
    onChangeKeypadScale: (Float) -> Unit,
    onChangeFxScale: (Float) -> Unit,
    onSelectGlowTarget: (GlowTarget) -> Unit,
    onChangeGlowIntensity: (Float) -> Unit,
    onSelectResultAnimStyle: (ResultAnimStyle) -> Unit,
    onTogglePressSparks: (Boolean) -> Unit,
    onSelectExpressiveTouchStyle: (ExpressiveTouchStyle) -> Unit,
    onToggleGlowBreathing: (Boolean) -> Unit,
    onToggleLivePreview: (Boolean) -> Unit,
    onToggleAmbientDisplayGlow: (Boolean) -> Unit,
    onSelectDisplayGlowStyle: (DisplayGlowStyle) -> Unit,
    onChangeDisplayGlowIntensity: (Float) -> Unit,
    onSelectDisplayGlowColorPreset: (GlowColorPreset) -> Unit,
    onSelectDisplayGlowCustomArgb: (Long) -> Unit,
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )
    val scope = rememberCoroutineScope()
    val vibrationExpanded = state.settingsVibrationExpanded
    val dynamicColorExpanded = state.settingsDynamicColorExpanded

    // Persist scroll only when idle or closing — zero StateFlow recompositions while swiping at 120Hz
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    onSaveScrollPosition(
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset
                    )
                }
            }
    }
    DisposableEffect(listState) {
        onDispose {
            onSaveScrollPosition(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    // Unconditionally consume leftover scroll/fling inside the scrollable list so swiping
    // bottom-to-top in settings never spills into ModalBottomSheet swipe-to-dismiss.
    val blockSheetSwipeOverscroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = if (available.y != 0f) Offset(0f, available.y) else Offset.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = if (available.y != 0f) Velocity(0f, available.y) else Velocity.Zero
        }
    }

    val resetSpin = remember { Animatable(0f) }
    val resetCardPulse = remember { Animatable(1f) }
    var justReset by remember { mutableStateOf(false) }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 6
        }
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
        LaunchedEffect(sheetView) {
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
            modifier = Modifier.fillMaxWidth()
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

                    val closeInteraction = remember { MutableInteractionSource() }
                    val closePressed by closeInteraction.collectIsPressedAsState()
                    val closeCorner by animateDpAsState(
                        targetValue = if (closePressed) 12.dp else 20.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "close_btn_corner"
                    )
                    val closeScale by animateFloatAsState(
                        targetValue = if (closePressed) 0.90f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "close_btn_scale"
                    )
                    val closeShape = RoundedCornerShape(closeCorner)
                    Surface(
                        shape = closeShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                scaleX = closeScale
                                scaleY = closeScale
                            }
                            .clip(closeShape)
                            .clickable(
                                interactionSource = closeInteraction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onDismiss
                            )
                            .testTag("settings_close_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .nestedScroll(blockSheetSwipeOverscroll)
                    .graphicsLayer {
                        val s = resetCardPulse.value
                        scaleX = s
                        scaleY = s
                    }
            ) {
                // Section 1: Sound & Haptic Feedback
                item(key = "section_feedback") {
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

                SettingsVibrationSection(
                    isVibrationEnabled = state.isVibrationEnabled,
                    vibrationExpanded = vibrationExpanded,
                    hapticIntensity = state.hapticIntensity,
                    hapticAccentEquals = state.hapticAccentEquals,
                    onToggleVibration = onToggleVibration,
                    onToggleVibrationExpanded = { onToggleVibrationExpanded(!vibrationExpanded) },
                    onSelectHapticIntensity = onSelectHapticIntensity,
                    onToggleHapticAccentEquals = onToggleHapticAccentEquals,
                    onTestHaptic = onTestHaptic
                )
                }
                }

                // Unified Section 2: Appearance, Shape & Live Mockup (economical tabbed studio)
                item(key = "section_design_layout") {
                SettingsSectionCard(title = stringResource(R.string.settings_subsection_design_layout)) {
                PanelLayoutPreviewCard(state = state)

                SettingsCardDivider()

                // Economical 2-tab Material 3 Expressive switcher directly below the live mockup
                val designTab = state.settingsDesignTab.coerceIn(0, 1)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_design_tab_appearance),
                            icon = Icons.Rounded.Palette,
                            selected = designTab == 0,
                            onClick = { onSelectDesignTab(0) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_design_tab_appearance"
                        )
                        SettingsChoicePill(
                            label = stringResource(R.string.settings_design_tab_shape),
                            icon = Icons.Rounded.RoundedCorner,
                            selected = designTab == 1,
                            onClick = { onSelectDesignTab(1) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_design_tab_shape"
                        )
                    }
                }

                AnimatedContent(
                    targetState = designTab,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(130))
                    },
                    label = "design_layout_tab_content"
                ) { activeTab ->
                    if (activeTab == 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
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

                            SettingsDynamicColorSection(
                                useDynamicColor = state.useDynamicColor,
                                dynamicColorExpanded = dynamicColorExpanded,
                                dynamicColorSource = state.dynamicColorSource,
                                dynamicColorVibrancy = state.dynamicColorVibrancy,
                                onToggleDynamicColor = { onToggleDynamicColor(!state.useDynamicColor) },
                                onToggleDynamicColorExpanded = { onToggleDynamicColorExpanded(!dynamicColorExpanded) },
                                onSelectDynamicColorSource = onSelectDynamicColorSource,
                                onSelectDynamicColorVibrancy = onSelectDynamicColorVibrancy
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
                                Text(
                                    text = stringResource(R.string.settings_color_buttons_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsChoicePill(
                                        label = stringResource(R.string.settings_color_buttons_digits),
                                        icon = if (state.colorTintDigits) Icons.Rounded.CheckCircle else Icons.Rounded.Dialpad,
                                        selected = state.colorTintDigits,
                                        onClick = { onToggleColorTintDigits(!state.colorTintDigits) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "settings_color_tint_digits"
                                    )
                                    SettingsChoicePill(
                                        label = stringResource(R.string.settings_color_buttons_operators),
                                        icon = if (state.colorTintOperators) Icons.Rounded.CheckCircle else Icons.Rounded.Calculate,
                                        selected = state.colorTintOperators,
                                        onClick = { onToggleColorTintOperators(!state.colorTintOperators) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "settings_color_tint_operators"
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsChoicePill(
                                        label = stringResource(R.string.settings_color_buttons_utilities),
                                        icon = if (state.colorTintUtilities) Icons.Rounded.CheckCircle else Icons.Rounded.Backspace,
                                        selected = state.colorTintUtilities,
                                        onClick = { onToggleColorTintUtilities(!state.colorTintUtilities) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "settings_color_tint_utilities"
                                    )
                                    SettingsChoicePill(
                                        label = stringResource(R.string.settings_color_buttons_fx),
                                        icon = if (state.colorTintFx) Icons.Rounded.CheckCircle else Icons.Rounded.Functions,
                                        selected = state.colorTintFx,
                                        onClick = { onToggleColorTintFx(!state.colorTintFx) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "settings_color_tint_fx"
                                    )
                                }
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
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                        Triple(ButtonShapeStyle.PILL, stringResource(R.string.settings_shape_pill), Icons.Rounded.Circle),
                                        Triple(ButtonShapeStyle.SQUIRCLE, stringResource(R.string.settings_shape_squircle), Icons.Rounded.RoundedCorner),
                                        Triple(ButtonShapeStyle.SHARP, stringResource(R.string.settings_shape_sharp), Icons.Rounded.CropSquare)
                                    )
                                    shapeOptions.forEach { (style, label, icon) ->
                                        SettingsChoicePill(
                                            label = label,
                                            icon = icon,
                                            selected = state.buttonShapeStyle == style,
                                            onClick = { onSelectButtonShape(style) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "settings_button_shape_${style.name.lowercase()}"
                                        )
                                    }
                                }
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
                    }
                }
                }
                }

                // Hub Subsection B: Key Backlight & Display Glow Variations
                item(key = "section_glow_display") {
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
                            Triple(GlowTarget.ACCENT_KEYS, stringResource(R.string.settings_glow_target_accents), Icons.Rounded.Flare),
                            Triple(GlowTarget.ALL_KEYS, stringResource(R.string.settings_glow_target_all), Icons.Rounded.AutoAwesome),
                            Triple(GlowTarget.OFF, stringResource(R.string.settings_glow_target_off), Icons.Rounded.VisibilityOff)
                        )
                        glowTargetOptions.forEach { (target, label, icon) ->
                            SettingsChoicePill(
                                label = label,
                                icon = icon,
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
                                    icon = Icons.Rounded.AutoAwesome,
                                    selected = state.displayGlowStyle == DisplayGlowStyle.AURORA,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.AURORA) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_aurora"
                                )
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_neon_frame),
                                    icon = Icons.Rounded.CropFree,
                                    selected = state.displayGlowStyle == DisplayGlowStyle.NEON_FRAME,
                                    onClick = { onSelectDisplayGlowStyle(DisplayGlowStyle.NEON_FRAME) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_display_glow_neon_frame"
                                )
                                SettingsChoicePill(
                                    label = stringResource(R.string.settings_display_glow_horizon),
                                    icon = Icons.Rounded.Gradient,
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

                        DisplayGlowColorEditor(
                            title = stringResource(R.string.settings_display_glow_color_title),
                            selectedPreset = state.displayGlowColorPreset,
                            customArgb = state.displayGlowCustomArgb,
                            onSelectPreset = onSelectDisplayGlowColorPreset,
                            onSelectCustomArgb = onSelectDisplayGlowCustomArgb,
                            testTagPrefix = "settings_display_glow_color"
                        )
                    }
                }
                }
                }

                // Hub Subsection C: Animations & Effects
                item(key = "section_animations") {
                SettingsSectionCard(title = stringResource(R.string.settings_subsection_animations)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_result_anim_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    val resultAnimRow1 = listOf(
                        ResultAnimStyle.SLIDE_UP to stringResource(R.string.settings_result_anim_slide),
                        ResultAnimStyle.ELASTIC_POP to stringResource(R.string.settings_result_anim_pop),
                        ResultAnimStyle.SHIMMER_SWEEP to stringResource(R.string.settings_result_anim_shimmer)
                    )
                    val resultAnimRow2 = listOf(
                        ResultAnimStyle.ZOOM_FOCUS to stringResource(R.string.settings_result_anim_focus),
                        ResultAnimStyle.GLIDE_RIGHT to stringResource(R.string.settings_result_anim_glide),
                        ResultAnimStyle.FLIP_REVEAL to stringResource(R.string.settings_result_anim_flip)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        resultAnimRow1.forEach { (style, label) ->
                            ResultAnimOptionTile(
                                style = style,
                                label = label,
                                selected = state.resultAnimStyle == style,
                                onClick = { onSelectResultAnimStyle(style) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_result_anim_${style.name.lowercase()}"
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        resultAnimRow2.forEach { (style, label) ->
                            ResultAnimOptionTile(
                                style = style,
                                label = label,
                                selected = state.resultAnimStyle == style,
                                onClick = { onSelectResultAnimStyle(style) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_result_anim_${style.name.lowercase()}"
                            )
                        }
                    }

                    ResultAnimPreviewStage(style = state.resultAnimStyle)
                }

                SettingsCardDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.settings_press_sparks_title),
                    subtitle = stringResource(R.string.settings_press_sparks_subtitle),
                    checked = state.pressSparksEnabled,
                    onToggle = { onTogglePressSparks(!state.pressSparksEnabled) },
                    testTag = "settings_press_sparks_switch"
                )

                AnimatedVisibility(
                    visible = state.pressSparksEnabled,
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
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_press_sparks_style_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val row1 = listOf(
                            ExpressiveTouchStyle.TONAL_HALO to stringResource(R.string.settings_press_sparks_halo),
                            ExpressiveTouchStyle.FLOWER_MORPH to stringResource(R.string.settings_press_sparks_petal),
                            ExpressiveTouchStyle.SPRING_WAVE to stringResource(R.string.settings_press_sparks_wave)
                        )
                        val row2 = listOf(
                            ExpressiveTouchStyle.CAPSULE_BURST to stringResource(R.string.settings_press_sparks_capsules),
                            ExpressiveTouchStyle.CLOVER_BLOOM to stringResource(R.string.settings_press_sparks_clover),
                            ExpressiveTouchStyle.PRISM_SHIMMER to stringResource(R.string.settings_press_sparks_prism)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row1.forEach { (style, label) ->
                                ExpressiveEffectOptionTile(
                                    style = style,
                                    label = label,
                                    selected = state.expressiveTouchStyle == style,
                                    onClick = { onSelectExpressiveTouchStyle(style) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_press_spark_${style.name.lowercase()}"
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row2.forEach { (style, label) ->
                                ExpressiveEffectOptionTile(
                                    style = style,
                                    label = label,
                                    selected = state.expressiveTouchStyle == style,
                                    onClick = { onSelectExpressiveTouchStyle(style) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_press_spark_${style.name.lowercase()}"
                                )
                            }
                        }

                        ExpressiveTouchPreviewStage(state = state)
                    }
                }

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
                }

                // Animated Reset Pill
                item(key = "section_reset") {
                val resetInteraction = remember { MutableInteractionSource() }
                val resetPressed by resetInteraction.collectIsPressedAsState()
                val resetCorner by animateDpAsState(
                    targetValue = if (resetPressed) 13.dp else 23.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "reset_pill_corner"
                )
                val resetScale by animateFloatAsState(
                    targetValue = if (resetPressed) 0.95f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "reset_pill_press_scale"
                )
                val resetBg by animateColorAsState(
                    targetValue = if (justReset) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    animationSpec = tween(220),
                    label = "reset_pill_bg"
                )
                val confirmedResetFg = if (MaterialTheme.colorScheme.primary.luminance() > 0.46f) {
                    Color(0xFF14161D)
                } else {
                    Color.White
                }
                val resetFg by animateColorAsState(
                    targetValue = if (justReset) {
                        confirmedResetFg
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    animationSpec = tween(220),
                    label = "reset_pill_fg"
                )
                val resetShape = RoundedCornerShape(resetCorner)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = resetShape,
                        color = resetBg,
                        border = BorderStroke(
                            width = if (justReset) 1.5.dp else 1.2.dp,
                            color = if (justReset) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                            }
                        ),
                        modifier = Modifier
                            .height(46.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                scaleX = resetScale
                                scaleY = resetScale
                            }
                            .clip(resetShape)
                            .clickable(
                                interactionSource = resetInteraction,
                                indication = androidx.compose.material3.ripple()
                            ) {
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
                            modifier = Modifier.padding(horizontal = 20.dp),
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
}

@Composable
private fun CustomAccentColorEditor(
    selectedAccentArgb: Long,
    onSelectAccentArgb: (Long) -> Unit
) {
    val isPresetColor = remember(selectedAccentArgb) {
        ACCENT_PRESETS.any { it == selectedAccentArgb }
    }
    var customExpanded by remember { mutableStateOf(!isPresetColor) }
    androidx.compose.runtime.LaunchedEffect(isPresetColor) {
        if (!isPresetColor) {
            customExpanded = true
        }
    }

    val activeColor = Color(selectedAccentArgb.toInt())
    val hexLabel = remember(selectedAccentArgb) {
        String.format("#%06X", selectedAccentArgb and 0xFFFFFFL)
    }

    ExpressiveColorPickerBlock(
        title = stringResource(R.string.settings_custom_color_title),
        badgeText = hexLabel,
        badgePrimaryColor = activeColor,
        badgeSecondaryColor = activeColor,
        recommendedEntries = ACCENT_PRESETS.map { presetArgb ->
            ColorSwatchItem(
                key = presetArgb.toString(16),
                color = Color(presetArgb.toInt()),
                selected = !customExpanded && presetArgb == selectedAccentArgb,
                testTag = "settings_color_preset_${presetArgb.toString(16)}",
                onClick = {
                    customExpanded = false
                    onSelectAccentArgb(presetArgb)
                }
            )
        },
        customSelected = customExpanded,
        onSelectCustom = { customExpanded = true },
        customTestTag = "settings_color_custom_pill",
        slidersExpanded = customExpanded,
        sliderColorArgb = selectedAccentArgb,
        sliderValueBrightness = 0.85f,
        maxSaturation = 0.96f,
        onSliderColorChanged = { argb ->
            customExpanded = true
            onSelectAccentArgb(argb)
        },
        hueSliderTestTag = "settings_custom_hue_slider",
        satSliderTestTag = "settings_custom_sat_slider"
    )
}

@Composable
private fun DisplayGlowColorEditor(
    title: String,
    selectedPreset: GlowColorPreset,
    customArgb: Long,
    onSelectPreset: (GlowColorPreset) -> Unit,
    onSelectCustomArgb: (Long) -> Unit,
    testTagPrefix: String
) {
    val isCustom = selectedPreset == GlowColorPreset.CUSTOM
    val activeColor = when (selectedPreset) {
        GlowColorPreset.AUTO -> MaterialTheme.colorScheme.primary
        GlowColorPreset.CUSTOM -> Color(customArgb.toInt())
        else -> selectedPreset.argb?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
    }
    val secondaryBadgeColor = if (selectedPreset == GlowColorPreset.AUTO) {
        MaterialTheme.colorScheme.tertiary
    } else {
        activeColor
    }
    val badgeText = when (selectedPreset) {
        GlowColorPreset.AUTO -> stringResource(R.string.settings_glow_color_auto)
        GlowColorPreset.CUSTOM -> String.format("#%06X", customArgb and 0xFFFFFFL)
        else -> String.format("#%06X", (selectedPreset.argb ?: 0L) and 0xFFFFFFL)
    }
    val recommendedPresets = remember {
        GlowColorPreset.entries.filter { it != GlowColorPreset.CUSTOM }
    }

    ExpressiveColorPickerBlock(
        title = title,
        badgeText = badgeText,
        badgePrimaryColor = activeColor,
        badgeSecondaryColor = secondaryBadgeColor,
        recommendedEntries = recommendedPresets.map { preset ->
            val swatchColor = preset.argb?.let { Color(it.toInt()) }
                ?: MaterialTheme.colorScheme.primary
            ColorSwatchItem(
                key = preset.name,
                color = swatchColor,
                secondaryGradientColor = if (preset == GlowColorPreset.AUTO) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    null
                },
                centerGlyph = if (preset == GlowColorPreset.AUTO) {
                    stringResource(R.string.settings_glow_color_auto).take(1)
                } else {
                    null
                },
                selected = selectedPreset == preset,
                testTag = "${testTagPrefix}_${preset.name.lowercase()}",
                onClick = { onSelectPreset(preset) }
            )
        },
        customSelected = isCustom,
        onSelectCustom = { onSelectPreset(GlowColorPreset.CUSTOM) },
        customTestTag = "${testTagPrefix}_custom_pill",
        slidersExpanded = isCustom,
        sliderColorArgb = customArgb,
        sliderValueBrightness = 0.92f,
        maxSaturation = 1.0f,
        onSliderColorChanged = onSelectCustomArgb,
        hueSliderTestTag = "${testTagPrefix}_hue_slider",
        satSliderTestTag = "${testTagPrefix}_sat_slider"
    )
}

private data class ColorSwatchItem(
    val key: String,
    val color: Color,
    val secondaryGradientColor: Color? = null,
    val centerGlyph: String? = null,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun ExpressiveColorPickerBlock(
    title: String,
    badgeText: String,
    badgePrimaryColor: Color,
    badgeSecondaryColor: Color,
    recommendedEntries: List<ColorSwatchItem>,
    customSelected: Boolean,
    onSelectCustom: () -> Unit,
    customTestTag: String,
    slidersExpanded: Boolean,
    sliderColorArgb: Long,
    sliderValueBrightness: Float,
    maxSaturation: Float,
    onSliderColorChanged: (Long) -> Unit,
    hueSliderTestTag: String,
    satSliderTestTag: String
) {
    val activeSliderColor = Color(sliderColorArgb.toInt())
    var lastEmittedArgb by remember { mutableStateOf<Long?>(null) }
    var hue by remember {
        mutableFloatStateOf(
            FloatArray(3).also { android.graphics.Color.colorToHSV(sliderColorArgb.toInt(), it) }[0]
        )
    }
    var saturation by remember {
        mutableFloatStateOf(
            FloatArray(3).also { android.graphics.Color.colorToHSV(sliderColorArgb.toInt(), it) }[1]
                .coerceIn(0.25f, maxSaturation)
        )
    }

    androidx.compose.runtime.LaunchedEffect(sliderColorArgb) {
        if (sliderColorArgb != lastEmittedArgb) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(sliderColorArgb.toInt(), hsv)
            hue = hsv[0]
            saturation = hsv[1].coerceIn(0.25f, maxSaturation)
            lastEmittedArgb = sliderColorArgb
        }
    }

    val customInteraction = remember { MutableInteractionSource() }
    val customPressed by customInteraction.collectIsPressedAsState()
    val customPillCorner by animateDpAsState(
        targetValue = when {
            customPressed -> 9.dp
            customSelected -> 20.dp
            else -> 13.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "custom_color_pill_corner"
    )
    val customPillScale by animateFloatAsState(
        targetValue = when {
            customPressed -> 0.94f
            customSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "custom_color_pill_scale"
    )
    val customPillShape = RoundedCornerShape(customPillCorner)
    val selectedCustomBg = MaterialTheme.colorScheme.primary
    val selectedCustomFg = if (selectedCustomBg.luminance() > 0.46f) {
        Color(0xFF14161D)
    } else {
        Color.White
    }
    val customPillBg by animateColorAsState(
        targetValue = if (customSelected) {
            selectedCustomBg
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "custom_color_pill_bg"
    )
    val customPillFg by animateColorAsState(
        targetValue = if (customSelected) {
            selectedCustomFg
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(180),
        label = "custom_color_pill_fg"
    )

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
                text = title,
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
                            .background(
                                Brush.linearGradient(listOf(badgePrimaryColor, badgeSecondaryColor))
                            )
                    )
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Sub-header: Recommended label + Custom expressive toggle pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_color_recommended),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = customPillShape,
                color = customPillBg,
                border = if (customSelected) {
                    BorderStroke(1.3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
                },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = customPillScale
                        scaleY = customPillScale
                    }
                    .clip(customPillShape)
                    .clickable(
                        interactionSource = customInteraction,
                        indication = androidx.compose.material3.ripple(),
                        onClick = onSelectCustom
                    )
                    .testTag(customTestTag)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (customSelected) {
                                    Brush.linearGradient(listOf(activeSliderColor, activeSliderColor))
                                } else {
                                    Brush.sweepGradient(
                                        listOf(
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
                            )
                            .border(
                                width = 1.dp,
                                color = if (customSelected) customPillFg.copy(alpha = 0.45f) else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = stringResource(R.string.settings_color_custom),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = customPillFg
                    )
                }
            }
        }

        // Recommended swatches row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            recommendedEntries.forEach { item ->
                val swatchScale by animateFloatAsState(
                    targetValue = if (item.selected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "swatch_scale_${item.key}"
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
                            if (item.secondaryGradientColor != null) {
                                Brush.linearGradient(listOf(item.color, item.secondaryGradientColor))
                            } else {
                                Brush.linearGradient(listOf(item.color, item.color))
                            }
                        )
                        .border(
                            width = if (item.selected) 2.5.dp else 1.dp,
                            color = if (item.selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            },
                            shape = CircleShape
                        )
                        .clickable(onClick = item.onClick)
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (item.secondaryGradientColor == null && item.color.luminance() > 0.56f) {
                                Color.Black
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (item.centerGlyph != null) {
                        Text(
                            text = item.centerGlyph,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = slidersExpanded,
            enter = fadeIn(tween(180)) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(140)) + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val packed = android.graphics.Color.HSVToColor(
                                    floatArrayOf(newHue, saturation, sliderValueBrightness)
                                )
                                val argb = packed.toLong() and 0xFFFFFFFFL
                                lastEmittedArgb = argb
                                onSliderColorChanged(argb)
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = activeSliderColor,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(hueSliderTestTag)
                        )
                    }
                }

                // Custom Saturation slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.settings_custom_color_saturation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(contentAlignment = Alignment.Center) {
                        val satBrush = remember(hue, sliderValueBrightness, maxSaturation) {
                            val lowSatColor = Color(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(hue, 0.25f, sliderValueBrightness)
                                )
                            )
                            val highSatColor = Color(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(hue, maxSaturation, sliderValueBrightness)
                                )
                            )
                            Brush.horizontalGradient(listOf(lowSatColor, highSatColor))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(satBrush)
                        )
                        Slider(
                            value = saturation,
                            onValueChange = { newSat ->
                                saturation = newSat
                                val packed = android.graphics.Color.HSVToColor(
                                    floatArrayOf(hue, newSat, sliderValueBrightness)
                                )
                                val argb = packed.toLong() and 0xFFFFFFFFL
                                lastEmittedArgb = argb
                                onSliderColorChanged(argb)
                            },
                            valueRange = 0.25f..maxSaturation,
                            colors = SliderDefaults.colors(
                                thumbColor = activeSliderColor,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(satSliderTestTag)
                        )
                    }
                }
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
private fun SettingsVibrationSection(
    isVibrationEnabled: Boolean,
    vibrationExpanded: Boolean,
    hapticIntensity: HapticIntensity,
    hapticAccentEquals: Boolean,
    onToggleVibration: () -> Unit,
    onToggleVibrationExpanded: () -> Unit,
    onSelectHapticIntensity: (HapticIntensity) -> Unit,
    onToggleHapticAccentEquals: (Boolean) -> Unit,
    onTestHaptic: (CalcSoundType) -> Unit
) {
    val badgeBg by animateColorAsState(
        targetValue = if (isVibrationEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "vibration_badge_bg"
    )
    val badgeTint by animateColorAsState(
        targetValue = if (isVibrationEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "vibration_badge_tint"
    )
    val intensityLabel = when (hapticIntensity) {
        HapticIntensity.LIGHT -> stringResource(R.string.settings_haptic_light)
        HapticIntensity.MEDIUM -> stringResource(R.string.settings_haptic_medium)
        HapticIntensity.STRONG -> stringResource(R.string.settings_haptic_strong)
    }
    val summaryLabel = if (hapticAccentEquals) {
        stringResource(R.string.settings_haptic_summary_accent, intensityLabel)
    } else {
        intensityLabel
    }
    val expandDescription = stringResource(R.string.settings_haptic_advanced_toggle)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleVibration)
                    .padding(vertical = 4.dp)
                    .testTag("settings_vibration_switch"),
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
                    VibrationStatusIcon(
                        enabled = isVibrationEnabled,
                        tint = badgeTint,
                        gapColor = badgeBg,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feedback_vibration_label),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedVisibility(
                        visible = isVibrationEnabled,
                        enter = fadeIn(tween(180)) + expandVertically(),
                        exit = fadeOut(tween(120)) + shrinkVertically()
                    ) {
                        Text(
                            text = summaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isVibrationEnabled,
                enter = fadeIn(tween(180)) + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut(tween(130)) + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                SettingsInlineExpandPill(
                    icon = Icons.Rounded.Tune,
                    expanded = vibrationExpanded,
                    contentDescription = expandDescription,
                    onClick = onToggleVibrationExpanded,
                    testTag = "settings_haptic_advanced_toggle"
                )
            }

            Switch(
                checked = isVibrationEnabled,
                onCheckedChange = { onToggleVibration() }
            )
        }

        AnimatedVisibility(
            visible = isVibrationEnabled && vibrationExpanded,
            enter = fadeIn(tween(180)) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(140)) + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                                Triple(HapticIntensity.LIGHT, stringResource(R.string.settings_haptic_light), Icons.Rounded.Vibration),
                                Triple(HapticIntensity.MEDIUM, stringResource(R.string.settings_haptic_medium), Icons.Rounded.GraphicEq),
                                Triple(HapticIntensity.STRONG, stringResource(R.string.settings_haptic_strong), Icons.Rounded.Bolt)
                            )
                            hapticOptions.forEach { (intensity, label, icon) ->
                                SettingsChoicePill(
                                    label = label,
                                    icon = icon,
                                    selected = hapticIntensity == intensity,
                                    onClick = { onSelectHapticIntensity(intensity) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_haptic_${intensity.name.lowercase()}"
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleHapticAccentEquals(!hapticAccentEquals) }
                            .padding(vertical = 2.dp)
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
                            checked = hapticAccentEquals,
                            onCheckedChange = onToggleHapticAccentEquals
                        )
                    }

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
                            icon = Icons.Rounded.Bolt,
                            selected = false,
                            onClick = { onTestHaptic(CalcSoundType.EQUALS) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_haptic_test_equals_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInlineExpandPill(
    icon: ImageVector,
    expanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    val expandInteraction = remember { MutableInteractionSource() }
    val isExpandPressed by expandInteraction.collectIsPressedAsState()
    val pillCorner by animateDpAsState(
        targetValue = when {
            isExpandPressed -> 10.dp
            expanded -> 18.dp
            else -> 12.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "inline_expand_pill_corner"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (isExpandPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "inline_expand_pill_scale"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "inline_expand_chevron"
    )
    val pillBg by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "inline_expand_pill_bg"
    )
    val pillFg by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(180),
        label = "inline_expand_pill_fg"
    )
    val pillShape = RoundedCornerShape(pillCorner)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = expandInteraction,
                indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = pillShape,
            color = pillBg,
            border = BorderStroke(
                width = if (expanded) 1.2.dp else 1.dp,
                color = if (expanded) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                }
            ),
            modifier = Modifier
                .height(34.dp)
                .graphicsLayer {
                    scaleX = pillScale
                    scaleY = pillScale
                }
                .clip(pillShape)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = pillFg,
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = pillFg,
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }
    }
}

@Composable
private fun SettingsDynamicColorSection(
    useDynamicColor: Boolean,
    dynamicColorExpanded: Boolean,
    dynamicColorSource: DynamicColorSource,
    dynamicColorVibrancy: DynamicColorVibrancy,
    onToggleDynamicColor: () -> Unit,
    onToggleDynamicColorExpanded: () -> Unit,
    onSelectDynamicColorSource: (DynamicColorSource) -> Unit,
    onSelectDynamicColorVibrancy: (DynamicColorVibrancy) -> Unit
) {
    val badgeBg by animateColorAsState(
        targetValue = if (useDynamicColor) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "dynamic_color_badge_bg"
    )
    val badgeTint by animateColorAsState(
        targetValue = if (useDynamicColor) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "dynamic_color_badge_tint"
    )
    val sourceLabel = when (dynamicColorSource) {
        DynamicColorSource.PRIMARY -> stringResource(R.string.settings_dynamic_source_primary)
        DynamicColorSource.SECONDARY -> stringResource(R.string.settings_dynamic_source_secondary)
        DynamicColorSource.TERTIARY -> stringResource(R.string.settings_dynamic_source_tertiary)
    }
    val vibrancyLabel = when (dynamicColorVibrancy) {
        DynamicColorVibrancy.NATURAL -> stringResource(R.string.settings_dynamic_vibrancy_natural)
        DynamicColorVibrancy.BALANCED -> stringResource(R.string.settings_dynamic_vibrancy_balanced)
        DynamicColorVibrancy.VIVID -> stringResource(R.string.settings_dynamic_vibrancy_vivid)
    }
    val expandDescription = stringResource(
        if (dynamicColorExpanded) {
            R.string.settings_dynamic_color_hide
        } else {
            R.string.settings_dynamic_color_expand
        }
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleDynamicColor)
                    .padding(vertical = 4.dp)
                    .testTag("settings_dynamic_color_switch"),
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
                    Icon(
                        imageVector = Icons.Rounded.ColorLens,
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (useDynamicColor) {
                            "$sourceLabel · $vibrancyLabel"
                        } else {
                            stringResource(R.string.settings_dynamic_color_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (useDynamicColor) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = useDynamicColor,
                enter = fadeIn(tween(180)) + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut(tween(130)) + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                SettingsInlineExpandPill(
                    icon = Icons.Rounded.Palette,
                    expanded = dynamicColorExpanded,
                    contentDescription = expandDescription,
                    onClick = onToggleDynamicColorExpanded,
                    testTag = "settings_dynamic_color_expand_toggle"
                )
            }

            Switch(
                checked = useDynamicColor,
                onCheckedChange = { onToggleDynamicColor() }
            )
        }

        AnimatedVisibility(
            visible = useDynamicColor && dynamicColorExpanded,
            enter = fadeIn(tween(180)) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(140)) + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_dynamic_source_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sourceOptions = listOf(
                            Triple(DynamicColorSource.PRIMARY, stringResource(R.string.settings_dynamic_source_primary), Icons.Rounded.Palette),
                            Triple(DynamicColorSource.SECONDARY, stringResource(R.string.settings_dynamic_source_secondary), Icons.Rounded.Tonality),
                            Triple(DynamicColorSource.TERTIARY, stringResource(R.string.settings_dynamic_source_tertiary), Icons.Rounded.Contrast)
                        )
                        sourceOptions.forEach { (source, label, icon) ->
                            SettingsChoicePill(
                                label = label,
                                icon = icon,
                                selected = dynamicColorSource == source,
                                onClick = { onSelectDynamicColorSource(source) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_dynamic_source_${source.name.lowercase()}"
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    )

                    Text(
                        text = stringResource(R.string.settings_dynamic_vibrancy_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val vibrancyOptions = listOf(
                            Triple(DynamicColorVibrancy.NATURAL, stringResource(R.string.settings_dynamic_vibrancy_natural), Icons.Rounded.WaterDrop),
                            Triple(DynamicColorVibrancy.BALANCED, stringResource(R.string.settings_dynamic_vibrancy_balanced), Icons.Rounded.Tune),
                            Triple(DynamicColorVibrancy.VIVID, stringResource(R.string.settings_dynamic_vibrancy_vivid), Icons.Rounded.AutoAwesome)
                        )
                        vibrancyOptions.forEach { (vibrancy, label, icon) ->
                            SettingsChoicePill(
                                label = label,
                                icon = icon,
                                selected = dynamicColorVibrancy == vibrancy,
                                onClick = { onSelectDynamicColorVibrancy(vibrancy) },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_dynamic_vibrancy_${vibrancy.name.lowercase()}"
                            )
                        }
                    }
                }
            }
        }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerDp by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            selected -> 22.dp
            else -> 14.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "choice_pill_corner"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.015f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "choice_pill_scale"
    )
    val selectedBg = MaterialTheme.colorScheme.primary
    val selectedTextColor = if (selectedBg.luminance() > 0.46f) {
        Color(0xFF14161D)
    } else {
        Color.White
    }
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            selectedBg
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "choice_pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            selectedTextColor
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        },
        animationSpec = tween(180),
        label = "choice_pill_fg"
    )
    val shape = RoundedCornerShape(cornerDp)

    Surface(
        shape = shape,
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
        },
        modifier = modifier
            .height(42.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) contentColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    letterSpacing = (-0.1).sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpressiveTouchPreviewStage(
    state: CalculatorUiState
) {
    val restingCorner = when (state.buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 50.dp
        ButtonShapeStyle.SQUIRCLE -> 20.dp
        ButtonShapeStyle.SHARP -> 11.dp
    }
    val pressedCorner = when (state.buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 14.dp
        ButtonShapeStyle.SQUIRCLE -> 11.dp
        ButtonShapeStyle.SHARP -> 6.dp
    }
    val glowColor = state.glowColorPreset.argb?.let { Color(it) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.settings_expressive_preview_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveCalcButton(
                    label = "7",
                    role = CalcButtonRole.DIGIT,
                    testTag = "settings_expressive_preview_digit",
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    hasGlow = state.glowTarget == GlowTarget.ALL_KEYS,
                    glowIntensity = state.glowIntensity,
                    glowColorOverride = glowColor,
                    colorTintEnabled = state.colorTintDigits,
                    keyPressEffect = state.keyPressEffect,
                    pressSparksEnabled = true,
                    expressiveTouchStyle = state.expressiveTouchStyle,
                    restingCornerRadius = restingCorner,
                    pressedCornerRadius = pressedCorner,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                )
                ExpressiveCalcButton(
                    label = "=",
                    role = CalcButtonRole.EQUALS,
                    testTag = "settings_expressive_preview_equals",
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    hasGlow = state.glowTarget != GlowTarget.OFF,
                    glowIntensity = state.glowIntensity,
                    glowColorOverride = glowColor,
                    colorTintEnabled = true,
                    keyPressEffect = state.keyPressEffect,
                    pressSparksEnabled = true,
                    expressiveTouchStyle = state.expressiveTouchStyle,
                    restingCornerRadius = restingCorner,
                    pressedCornerRadius = pressedCorner,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ResultAnimOptionTile(
    style: ResultAnimStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "result_anim_tile_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        },
        animationSpec = tween(180),
        label = "result_anim_tile_fg"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "result_anim_tile_scale"
    )
    val cornerDp by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            selected -> 22.dp
            else -> 14.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "result_anim_tile_corner"
    )
    val shape = RoundedCornerShape(cornerDp)
    val primaryTint = if (selected) MaterialTheme.colorScheme.primary else contentColor
    val secondaryTint = if (selected) MaterialTheme.colorScheme.tertiary else contentColor.copy(alpha = 0.68f)
    val motionPath = remember { Path() }

    Surface(
        shape = shape,
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
        },
        modifier = modifier
            .height(64.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val r = size.minDimension * 0.42f
                when (style) {
                    ResultAnimStyle.SLIDE_UP -> {
                        val sw = 1.7.dp.toPx()
                        // Trailing baseline bars fading upward
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.42f),
                            start = Offset(cx - r * 0.72f, cy + r * 0.72f),
                            end = Offset(cx + r * 0.72f, cy + r * 0.72f),
                            strokeWidth = sw * 0.9f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.72f),
                            start = Offset(cx - r * 0.56f, cy + r * 0.25f),
                            end = Offset(cx + r * 0.56f, cy + r * 0.25f),
                            strokeWidth = sw,
                            cap = StrokeCap.Round
                        )
                        // Upward chevron
                        motionPath.reset()
                        motionPath.moveTo(cx - r * 0.58f, cy - r * 0.14f)
                        motionPath.lineTo(cx, cy - r * 0.72f)
                        motionPath.lineTo(cx + r * 0.58f, cy - r * 0.14f)
                        drawPath(
                            path = motionPath,
                            color = primaryTint,
                            style = Stroke(width = sw * 1.1f, cap = StrokeCap.Round)
                        )
                    }
                    ResultAnimStyle.ELASTIC_POP -> {
                        drawRoundRect(
                            color = primaryTint.copy(alpha = 0.22f),
                            topLeft = Offset(cx - r * 0.62f, cy - r * 0.48f),
                            size = Size(r * 1.24f, r * 0.96f),
                            cornerRadius = CornerRadius(r * 0.34f, r * 0.34f)
                        )
                        drawRoundRect(
                            color = primaryTint,
                            topLeft = Offset(cx - r * 0.62f, cy - r * 0.48f),
                            size = Size(r * 1.24f, r * 0.96f),
                            cornerRadius = CornerRadius(r * 0.34f, r * 0.34f),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        val tickInner = r * 0.88f
                        val tickOuter = r * 1.15f
                        for (i in 0 until 4) {
                            val angle = (45f + i * 90f) * (PI.toFloat() / 180f)
                            val ca = cos(angle)
                            val sa = sin(angle)
                            drawLine(
                                color = secondaryTint,
                                start = Offset(cx + ca * tickInner, cy + sa * tickInner),
                                end = Offset(cx + ca * tickOuter, cy + sa * tickOuter),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    ResultAnimStyle.SHIMMER_SWEEP -> {
                        drawRoundRect(
                            color = primaryTint.copy(alpha = 0.18f),
                            topLeft = Offset(cx - r * 0.92f, cy - r * 0.56f),
                            size = Size(r * 1.84f, r * 1.12f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f)
                        )
                        drawRoundRect(
                            color = primaryTint,
                            topLeft = Offset(cx - r * 0.92f, cy - r * 0.56f),
                            size = Size(r * 1.84f, r * 1.12f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.45f),
                            start = Offset(cx - r * 0.48f, cy + r * 0.42f),
                            end = Offset(cx + r * 0.48f, cy - r * 0.42f),
                            strokeWidth = 3.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = secondaryTint,
                            start = Offset(cx - r * 0.48f, cy + r * 0.42f),
                            end = Offset(cx + r * 0.48f, cy - r * 0.42f),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    ResultAnimStyle.ZOOM_FOCUS -> {
                        val sw = 1.6.dp.toPx()
                        val b = r * 0.96f
                        val leg = r * 0.44f
                        // Four corner focus viewfinder brackets closing inward
                        motionPath.reset()
                        // Top-left
                        motionPath.moveTo(cx - b, cy - b + leg)
                        motionPath.lineTo(cx - b, cy - b)
                        motionPath.lineTo(cx - b + leg, cy - b)
                        // Top-right
                        motionPath.moveTo(cx + b - leg, cy - b)
                        motionPath.lineTo(cx + b, cy - b)
                        motionPath.lineTo(cx + b, cy - b + leg)
                        // Bottom-right
                        motionPath.moveTo(cx + b, cy + b - leg)
                        motionPath.lineTo(cx + b, cy + b)
                        motionPath.lineTo(cx + b - leg, cy + b)
                        // Bottom-left
                        motionPath.moveTo(cx - b + leg, cy + b)
                        motionPath.lineTo(cx - b, cy + b)
                        motionPath.lineTo(cx - b, cy + b - leg)
                        drawPath(
                            path = motionPath,
                            color = primaryTint,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = secondaryTint.copy(alpha = 0.28f),
                            radius = r * 0.50f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = primaryTint,
                            radius = r * 0.24f,
                            center = Offset(cx, cy)
                        )
                    }
                    ResultAnimStyle.GLIDE_RIGHT -> {
                        val sw = 1.7.dp.toPx()
                        // Speed trail lines from right
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.45f),
                            start = Offset(cx + r * 0.94f, cy - r * 0.45f),
                            end = Offset(cx + r * 0.25f, cy - r * 0.45f),
                            strokeWidth = sw * 0.9f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.72f),
                            start = Offset(cx + r * 0.94f, cy + r * 0.45f),
                            end = Offset(cx + r * 0.12f, cy + r * 0.45f),
                            strokeWidth = sw * 0.9f,
                            cap = StrokeCap.Round
                        )
                        // Center glide arrow pointing left
                        drawLine(
                            color = primaryTint,
                            start = Offset(cx + r * 0.78f, cy),
                            end = Offset(cx - r * 0.72f, cy),
                            strokeWidth = sw,
                            cap = StrokeCap.Round
                        )
                        motionPath.reset()
                        motionPath.moveTo(cx - r * 0.22f, cy - r * 0.48f)
                        motionPath.lineTo(cx - r * 0.76f, cy)
                        motionPath.lineTo(cx - r * 0.22f, cy + r * 0.48f)
                        drawPath(
                            path = motionPath,
                            color = primaryTint,
                            style = Stroke(width = sw * 1.05f, cap = StrokeCap.Round)
                        )
                    }
                    ResultAnimStyle.FLIP_REVEAL -> {
                        // Split-flap flip card with center hinge line
                        drawRoundRect(
                            color = primaryTint.copy(alpha = 0.20f),
                            topLeft = Offset(cx - r * 0.88f, cy - r * 0.78f),
                            size = Size(r * 1.76f, r * 1.56f),
                            cornerRadius = CornerRadius(r * 0.34f, r * 0.34f)
                        )
                        drawRoundRect(
                            color = primaryTint,
                            topLeft = Offset(cx - r * 0.88f, cy - r * 0.78f),
                            size = Size(r * 1.76f, r * 1.56f),
                            cornerRadius = CornerRadius(r * 0.34f, r * 0.34f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Horizontal split-flap hinge
                        drawLine(
                            color = secondaryTint,
                            start = Offset(cx - r * 0.88f, cy),
                            end = Offset(cx + r * 0.88f, cy),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Curved flip arrow indicator on top half
                        drawCircle(
                            color = primaryTint.copy(alpha = 0.85f),
                            radius = r * 0.16f,
                            center = Offset(cx - r * 0.45f, cy - r * 0.36f)
                        )
                        drawCircle(
                            color = secondaryTint.copy(alpha = 0.90f),
                            radius = r * 0.16f,
                            center = Offset(cx + r * 0.45f, cy + r * 0.36f)
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResultAnimPreviewStage(
    style: ResultAnimStyle
) {
    val samples = remember {
        listOf(
            "24 × 7" to "168",
            "15²" to "225",
            "84 ÷ 2" to "42",
            "12 × 9" to "108"
        )
    }
    var sampleIndex by remember { mutableIntStateOf(0) }
    var playTrigger by remember { mutableIntStateOf(0) }

    val revealAnim = remember { Animatable(1f) }
    val shimmerAnim = remember { Animatable(0f) }

    LaunchedEffect(style, playTrigger) {
        when (style) {
            ResultAnimStyle.SLIDE_UP -> {
                shimmerAnim.snapTo(0f)
                revealAnim.snapTo(0f)
                revealAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
                )
            }
            ResultAnimStyle.ELASTIC_POP -> {
                shimmerAnim.snapTo(0f)
                revealAnim.snapTo(0f)
                revealAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.46f,
                        stiffness = 520f
                    )
                )
            }
            ResultAnimStyle.SHIMMER_SWEEP -> {
                revealAnim.snapTo(0f)
                launch {
                    revealAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 160, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
                    )
                }
                launch {
                    shimmerAnim.snapTo(0f)
                    shimmerAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 780, easing = CubicBezierEasing(0.22f, 0.82f, 0.28f, 1f))
                    )
                    shimmerAnim.snapTo(0f)
                }
            }
            ResultAnimStyle.ZOOM_FOCUS -> {
                shimmerAnim.snapTo(0f)
                revealAnim.snapTo(0f)
                revealAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = 480f
                    )
                )
            }
            ResultAnimStyle.GLIDE_RIGHT -> {
                shimmerAnim.snapTo(0f)
                revealAnim.snapTo(0f)
                revealAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 500f
                    )
                )
            }
            ResultAnimStyle.FLIP_REVEAL -> {
                shimmerAnim.snapTo(0f)
                revealAnim.snapTo(0f)
                revealAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.66f,
                        stiffness = 440f
                    )
                )
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val stageCorner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 18.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "result_preview_corner"
    )
    val stageScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "result_preview_scale"
    )
    val stageShape = RoundedCornerShape(stageCorner)
    val (expr, res) = samples[sampleIndex % samples.size]
    val shimmerPrimary = MaterialTheme.colorScheme.primary
    val shimmerTertiary = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = stageShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = stageScale
                scaleY = stageScale
            }
            .clip(stageShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple()
            ) {
                sampleIndex = (sampleIndex + 1) % samples.size
                playTrigger += 1
            }
            .testTag("settings_result_anim_preview_stage")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_result_anim_preview_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = expr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
                Text(
                    text = res,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val p = revealAnim.value
                            if (p != 1f) {
                                alpha = (0.12f + 0.88f * p).coerceIn(0f, 1f)
                                when (style) {
                                    ResultAnimStyle.SLIDE_UP -> {
                                        translationY = (1f - p) * 18.dp.toPx()
                                    }
                                    ResultAnimStyle.ELASTIC_POP -> {
                                        val s = 0.72f + 0.28f * p
                                        scaleX = s
                                        scaleY = s
                                        transformOrigin = TransformOrigin(1f, 0.5f)
                                        translationY = (1f - p) * 6.dp.toPx()
                                    }
                                    ResultAnimStyle.SHIMMER_SWEEP -> {
                                        val s = 0.94f + 0.06f * p
                                        scaleX = s
                                        scaleY = s
                                        transformOrigin = TransformOrigin(1f, 0.5f)
                                    }
                                    ResultAnimStyle.ZOOM_FOCUS -> {
                                        val s = 1.24f - 0.24f * p
                                        scaleX = s
                                        scaleY = s
                                        transformOrigin = TransformOrigin(1f, 0.5f)
                                    }
                                    ResultAnimStyle.GLIDE_RIGHT -> {
                                        translationX = (1f - p) * 34.dp.toPx()
                                    }
                                    ResultAnimStyle.FLIP_REVEAL -> {
                                        rotationX = (1f - p) * -72f
                                        cameraDistance = 12f * density
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                                }
                            }
                            if (style == ResultAnimStyle.SHIMMER_SWEEP && shimmerAnim.value in 0.001f..0.999f) {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                        }
                        .drawWithContent {
                            val sh = shimmerAnim.value
                            val isGleaming = style == ResultAnimStyle.SHIMMER_SWEEP && sh > 0.001f && sh < 0.999f
                            if (isGleaming) {
                                val bandW = (size.width * 0.58f).coerceAtLeast(44.dp.toPx())
                                val startX = -bandW + (size.width + bandW * 2f) * sh
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            shimmerPrimary.copy(alpha = 0.28f),
                                            shimmerTertiary.copy(alpha = 0.24f),
                                            Color.Transparent
                                        ),
                                        start = Offset(startX, 0f),
                                        end = Offset(startX + bandW, size.height)
                                    ),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                            drawContent()
                            if (isGleaming) {
                                val bandW = (size.width * 0.56f).coerceAtLeast(42.dp.toPx())
                                val startX = -bandW + (size.width + bandW * 2f) * sh
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            shimmerTertiary.copy(alpha = 0.95f),
                                            Color.White.copy(alpha = 0.96f),
                                            shimmerTertiary.copy(alpha = 0.95f),
                                            Color.Transparent
                                        ),
                                        start = Offset(startX, 0f),
                                        end = Offset(startX + bandW, size.height)
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun ExpressiveEffectOptionTile(
    style: ExpressiveTouchStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "expressive_tile_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        },
        animationSpec = tween(180),
        label = "expressive_tile_fg"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expressive_tile_scale"
    )
    val cornerDp by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            selected -> 22.dp
            else -> 14.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expressive_tile_corner"
    )
    val shape = RoundedCornerShape(cornerDp)
    val primaryTint = if (selected) MaterialTheme.colorScheme.primary else contentColor
    val secondaryTint = if (selected) MaterialTheme.colorScheme.tertiary else contentColor.copy(alpha = 0.72f)
    val iconPath = remember { Path() }

    Surface(
        shape = shape,
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
        },
        modifier = modifier
            .height(60.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val r = size.minDimension * 0.40f
                when (style) {
                    ExpressiveTouchStyle.TONAL_HALO -> {
                        drawRoundRect(
                            color = secondaryTint.copy(alpha = 0.16f),
                            topLeft = Offset(cx - r * 1.26f, cy - r * 0.96f),
                            size = Size(r * 2.52f, r * 1.92f),
                            cornerRadius = CornerRadius(r * 0.64f, r * 0.64f)
                        )
                        drawRoundRect(
                            color = secondaryTint.copy(alpha = 0.68f),
                            topLeft = Offset(cx - r * 1.26f, cy - r * 0.96f),
                            size = Size(r * 2.52f, r * 1.92f),
                            cornerRadius = CornerRadius(r * 0.64f, r * 0.64f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawRoundRect(
                            color = primaryTint.copy(alpha = 0.22f),
                            topLeft = Offset(cx - r * 0.90f, cy - r * 0.64f),
                            size = Size(r * 1.80f, r * 1.28f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f)
                        )
                        drawRoundRect(
                            color = primaryTint,
                            topLeft = Offset(cx - r * 0.90f, cy - r * 0.64f),
                            size = Size(r * 1.80f, r * 1.28f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                    ExpressiveTouchStyle.FLOWER_MORPH -> {
                        iconPath.reset()
                        val pts = 32
                        for (i in 0 until pts) {
                            val theta = (i.toFloat() / pts.toFloat()) * (2f * PI.toFloat())
                            val rad = r * (0.94f + 0.14f * cos(8f * theta))
                            val px = cx + rad * cos(theta)
                            val py = cy + rad * sin(theta)
                            if (i == 0) iconPath.moveTo(px, py) else iconPath.lineTo(px, py)
                        }
                        iconPath.close()
                        drawPath(iconPath, color = primaryTint.copy(alpha = 0.24f))
                        drawPath(iconPath, color = primaryTint, style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(color = secondaryTint.copy(alpha = 0.85f), radius = r * 0.24f, center = Offset(cx, cy))
                    }
                    ExpressiveTouchStyle.SPRING_WAVE -> {
                        drawCircle(color = primaryTint.copy(alpha = 0.16f), radius = r * 1.04f, center = Offset(cx, cy))
                        drawCircle(
                            color = secondaryTint.copy(alpha = 0.72f),
                            radius = r * 1.04f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = primaryTint,
                            radius = r * 0.64f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        drawCircle(color = primaryTint, radius = r * 0.24f, center = Offset(cx, cy))
                    }
                    ExpressiveTouchStyle.CAPSULE_BURST -> {
                        val sw = 2.5.dp.toPx()
                        val inner = r * 0.44f
                        val outer = r * 1.06f
                        for (i in 0 until 6) {
                            val a = (i * 60f) * (PI.toFloat() / 180f)
                            val ca = cos(a)
                            val sa = sin(a)
                            val col = if (i % 2 == 0) primaryTint else secondaryTint
                            drawLine(
                                color = col.copy(alpha = 0.28f),
                                start = Offset(cx + ca * inner, cy + sa * inner),
                                end = Offset(cx + ca * outer, cy + sa * outer),
                                strokeWidth = sw * 1.45f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = col,
                                start = Offset(cx + ca * inner, cy + sa * inner),
                                end = Offset(cx + ca * outer, cy + sa * outer),
                                strokeWidth = sw,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    ExpressiveTouchStyle.CLOVER_BLOOM -> {
                        iconPath.reset()
                        val pts = 32
                        for (i in 0 until pts) {
                            val theta = (i.toFloat() / pts.toFloat()) * (2f * PI.toFloat())
                            val rad = r * (0.78f + 0.26f * cos(4f * theta))
                            val px = cx + rad * cos(theta)
                            val py = cy + rad * sin(theta)
                            if (i == 0) iconPath.moveTo(px, py) else iconPath.lineTo(px, py)
                        }
                        iconPath.close()
                        drawPath(iconPath, color = primaryTint.copy(alpha = 0.25f))
                        drawPath(iconPath, color = primaryTint, style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(color = secondaryTint, radius = r * 0.24f, center = Offset(cx, cy))
                    }
                    ExpressiveTouchStyle.PRISM_SHIMMER -> {
                        drawRoundRect(
                            color = primaryTint.copy(alpha = 0.18f),
                            topLeft = Offset(cx - r, cy - r * 0.78f),
                            size = Size(r * 2f, r * 1.56f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f)
                        )
                        drawRoundRect(
                            color = primaryTint,
                            topLeft = Offset(cx - r, cy - r * 0.78f),
                            size = Size(r * 2f, r * 1.56f),
                            cornerRadius = CornerRadius(r * 0.42f, r * 0.42f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawLine(
                            color = secondaryTint.copy(alpha = 0.45f),
                            start = Offset(cx - r * 0.56f, cy + r * 0.54f),
                            end = Offset(cx + r * 0.56f, cy - r * 0.54f),
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = secondaryTint,
                            start = Offset(cx - r * 0.56f, cy + r * 0.54f),
                            end = Offset(cx + r * 0.56f, cy - r * 0.54f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
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

    val bgScreenColor = MaterialTheme.colorScheme.background
    val isDarkPreview = bgScreenColor.luminance() < 0.5f
    val displayCardColor = MaterialTheme.colorScheme.surfaceContainer
    val digitKeyColor = if (state.colorTintDigits) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primaryContainer,
            if (isDarkPreview) 0.30f else 0.36f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val operatorKeyColor = if (state.colorTintOperators) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val utilityKeyColor = if (state.colorTintUtilities) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val equalsKeyColor = MaterialTheme.colorScheme.primary
    val fxModeKeyColor = if (state.colorTintOperators || state.colorTintFx) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val fxNormalKeyColor = if (state.colorTintFx) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primaryContainer,
            if (isDarkPreview) 0.30f else 0.36f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
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
    val resolvedDisplayGlowColor = when (state.displayGlowColorPreset) {
        GlowColorPreset.AUTO -> null
        GlowColorPreset.CUSTOM -> Color(state.displayGlowCustomArgb.toInt())
        else -> state.displayGlowColorPreset.argb?.let { Color(it.toInt()) }
    }

    val cornerFactor = when (state.buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 0.50f
        ButtonShapeStyle.SQUIRCLE -> 0.28f
        ButtonShapeStyle.SHARP -> 0.14f
    }

    val livePreviewMix by animateFloatAsState(
        targetValue = if (state.showLivePreview) 1f else 0f,
        animationSpec = tween(180),
        label = "preview_live_hint_mix"
    )

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
                    val padX = 9.dp.toPx()
                    val padTop = 7.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val innerW = (size.width - padX * 2f).coerceAtLeast(10f)

                    val unit = size.height / 256f
                    val kScale = state.keypadScale.coerceIn(0.80f, 1.18f)
                    val fScale = state.fxScale.coerceIn(0.78f, 1.25f)

                    val rowH = (16.5f * unit * kScale).coerceAtLeast(11.5f * unit)
                    val rowGap = (3.2f * unit * (0.85f + 0.15f * kScale)).coerceAtLeast(2.0f * unit)

                    val keypadH = rowH * 5f + rowGap * 4f
                    val fxH = 39f * unit * fScale
                    val fxGap = 3.5f * unit
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

                        when (state.displayGlowStyle) {
                            DisplayGlowStyle.AURORA -> {
                                val c1 = Offset(size.width * 0.25f, displayBottom * 0.30f)
                                val r1 = size.width * 0.52f
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
                                val c2 = Offset(size.width * 0.78f, displayBottom * 0.54f)
                                val r2 = size.width * 0.44f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to ambPrimary.copy(alpha = (0.26f * dGlowMul).coerceIn(0f, 0.56f)),
                                        1f to Color.Transparent,
                                        center = c2,
                                        radius = r2
                                    ),
                                    center = c2,
                                    radius = r2
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.56f to Color.Transparent,
                                        1f to bgScreenColor
                                    ),
                                    size = Size(size.width, displayBottom)
                                )
                            }
                            DisplayGlowStyle.NEON_FRAME -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.64f to Color.Transparent,
                                        1f to bgScreenColor.copy(alpha = 0.80f)
                                    ),
                                    size = Size(size.width, displayBottom)
                                )
                                drawRoundRect(
                                    color = ambPrimary.copy(alpha = (0.65f * dGlowMul).coerceIn(0f, 0.92f)),
                                    topLeft = Offset(2.dp.toPx(), -displayCorner),
                                    size = Size(size.width - 4.dp.toPx(), displayBottom + displayCorner - 2.dp.toPx()),
                                    cornerRadius = CornerRadius(displayCorner, displayCorner),
                                    style = Stroke(width = 1.4.dp.toPx())
                                )
                            }
                            DisplayGlowStyle.BOTTOM_HORIZON -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.20f to Color.Transparent,
                                        0.62f to ambPrimary.copy(alpha = (0.42f * dGlowMul).coerceIn(0f, 0.72f)),
                                        1f to Color.Transparent
                                    ),
                                    size = Size(size.width, displayBottom)
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.60f to Color.Transparent,
                                        1f to bgScreenColor
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

                    // Schematic expression, main number, and live-preview hint lines inside mini display
                    val lineRight = size.width - padX - 2.dp.toPx()
                    val resLineH = (8.0f * unit).coerceAtMost((displayBottom - padTop) * 0.23f).coerceAtLeast(4f)
                    val exprLineH = (3.5f * unit).coerceAtMost((displayBottom - padTop) * 0.11f).coerceAtLeast(2.2f)
                    val liveHintH = (3.2f * unit).coerceAtMost((displayBottom - padTop) * 0.10f).coerceAtLeast(2.0f)
                    val resW = innerW * 0.48f
                    val exprW = innerW * 0.30f
                    val liveHintW = innerW * 0.26f
                    val bottomReserve = if (livePreviewMix > 0.05f) liveHintH + 4.dp.toPx() else 0f
                    val resY = (displayBottom - 7.dp.toPx() - bottomReserve - resLineH).coerceAtLeast(padTop + 14.dp.toPx())
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
                    if (livePreviewMix > 0.01f && resY + resLineH + 3.dp.toPx() + liveHintH <= displayBottom - 3.dp.toPx()) {
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.65f * livePreviewMix),
                            topLeft = Offset(lineRight - liveHintW, resY + resLineH + 3.dp.toPx()),
                            size = Size(liveHintW, liveHintH),
                            cornerRadius = CornerRadius(liveHintH / 2f, liveHintH / 2f)
                        )
                    }

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

                    // 3. Mini 5x3 fx Grid (detached mode, DEG/RAD at col 0)
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
                                    val isModeKey = r == 0 && c == 0
                                    val hasMiniFxGlow = state.glowTarget == GlowTarget.ALL_KEYS ||
                                        (state.glowTarget == GlowTarget.ACCENT_KEYS && isModeKey)
                                    if (hasMiniFxGlow) {
                                        val spread = 1.6f.dp.toPx() * (0.7f + 0.3f * glowIntensity)
                                        val fxGlowAlpha = if (isModeKey) 0.32f else 0.18f
                                        drawRoundRect(
                                            color = primaryColor.copy(alpha = (fxGlowAlpha * glowIntensity * pFx).coerceIn(0f, 0.65f)),
                                            topLeft = Offset(x - spread, y - spread),
                                            size = Size(fxCellW + spread * 2f, fxCellH + spread * 2f),
                                            cornerRadius = CornerRadius(fxCorner + spread, fxCorner + spread)
                                        )
                                    }
                                    val tileColor = when {
                                        isModeKey -> fxModeKeyColor
                                        else -> fxNormalKeyColor
                                    }.copy(alpha = pFx)
                                    drawRoundRect(
                                        color = tileColor,
                                        topLeft = Offset(x, y),
                                        size = Size(fxCellW, fxCellH),
                                        cornerRadius = CornerRadius(fxCorner, fxCorner)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Mini 4x5 Standard Keypad Grid
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
                                val rawGlow = if (isUtility) tertiaryColor else primaryColor
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

            // Right telemetry & Material Expressive fx-state toggle column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsChoicePill(
                    label = stringResource(
                        if (previewFxOpen) {
                            R.string.settings_layout_preview_fx_open
                        } else {
                            R.string.settings_layout_preview_fx_closed
                        }
                    ),
                    icon = Icons.Rounded.Functions,
                    selected = previewFxOpen,
                    onClick = { previewFxOpen = !previewFxOpen },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "settings_layout_preview_toggle_fx"
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
