package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AngleUnit
import com.example.data.preferences.ButtonShapeStyle
import com.example.ui.CalculatorUiState
import com.example.ui.CalculatorViewModel
import com.example.ui.components.BigNumberViewerDialog
import com.example.ui.components.CalcButtonRole
import com.example.ui.components.CalculatorDisplay
import com.example.ui.components.ExpressiveCalcButton
import com.example.ui.components.FeedbackPermissionDialog
import com.example.ui.components.FullHistoryBottomSheet
import com.example.ui.components.LocalPerformCalcHaptic
import com.example.ui.components.LocalPlayCalcSound
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.TopPullDownHistoryOverlay
import com.example.ui.components.VoiceMathInputOverlay
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    viewModel: CalculatorViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val copiedMsg = stringResource(R.string.copied_snackbar)

    val copyToClipboard: (String) -> Unit = { text ->
        clipboardManager.setText(AnnotatedString(text))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(copiedMsg)
        }
    }

    val view = LocalView.current
    val performHapticWithView: (com.example.audio.CalcSoundType) -> Unit = remember(viewModel, view) {
        { type -> viewModel.performHaptic(type, view) }
    }

    var themeButtonAnchorRoot by remember { mutableStateOf<Offset?>(null) }
    var rootBoxOrigin by remember { mutableStateOf(Offset.Zero) }
    val oldThemeLayer = rememberGraphicsLayer()
    val revealRadiusFraction = remember { Animatable(0f) }
    var isThemeRevealing by remember { mutableStateOf(false) }
    var revealOriginRoot by remember { mutableStateOf<Offset?>(null) }
    var revealJob by remember { mutableStateOf<Job?>(null) }
    val revealClipPath = remember { Path() }

    val triggerThemeReveal: (() -> Unit) -> Unit = { applyThemeChange ->
        if (isThemeRevealing) {
            revealJob?.cancel()
            revealJob = null
            isThemeRevealing = false
            applyThemeChange()
        } else {
            revealOriginRoot = themeButtonAnchorRoot
            isThemeRevealing = true
            applyThemeChange()
            revealJob = scope.launch {
                try {
                    revealRadiusFraction.snapTo(0.01f)
                    revealRadiusFraction.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 420,
                            easing = CubicBezierEasing(0.22f, 0.90f, 0.25f, 1.0f)
                        )
                    )
                } finally {
                    isThemeRevealing = false
                    revealRadiusFraction.snapTo(0f)
                    revealJob = null
                }
            }
        }
    }

    BackHandler(enabled = state.isTopHistoryOpen) {
        viewModel.setTopHistoryOpen(false)
    }

    CompositionLocalProvider(
        LocalPlayCalcSound provides viewModel::playSound,
        LocalPerformCalcHaptic provides performHapticWithView
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        rootBoxOrigin = coords.positionInRoot()
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (!isThemeRevealing) {
                            oldThemeLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(oldThemeLayer)
                        } else {
                            drawContent()
                        }
                    }
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    var displayCardHeightPx by remember { mutableIntStateOf(0) }
                    val density = LocalDensity.current
                    val quickHistoryMaxHeight = remember(displayCardHeightPx, density) {
                        if (displayCardHeightPx > 0) {
                            with(density) { displayCardHeightPx.toDp() }
                        } else {
                            240.dp
                        }
                    }

                    // Single synchronized GPU-friendly progress for scientific panel slide (no child re-squish lag)
                    val scientificExpandProgress by animateFloatAsState(
                        targetValue = if (state.isScientificExpanded) 1f else 0f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "scientific_expand_progress"
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val isWideOrLandscape = maxWidth >= 600.dp || maxWidth > maxHeight
                            val isCompactVertical = maxHeight < 700.dp

                            val collapsedRowHeight = if (isCompactVertical) 56.dp else 62.dp
                            val expandedRowHeight = if (isCompactVertical) 44.dp else 48.dp
                            val targetStandardRowHeight = lerp(
                                collapsedRowHeight,
                                expandedRowHeight,
                                scientificExpandProgress
                            )
                            val collapsedRowSpacing = if (isCompactVertical) 9.dp else 10.dp
                            val expandedRowSpacing = if (isCompactVertical) 6.dp else 8.dp
                            val targetStandardRowSpacing = lerp(
                                collapsedRowSpacing,
                                expandedRowSpacing,
                                scientificExpandProgress
                            )
                            val keypadTextScale = 1f - 0.12f * scientificExpandProgress
                            val baseStandardCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 50.dp
                                ButtonShapeStyle.SQUIRCLE -> 24.dp
                                ButtonShapeStyle.SHARP -> 12.dp
                            }
                            val expandedStandardCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 20.dp
                                ButtonShapeStyle.SQUIRCLE -> 16.dp
                                ButtonShapeStyle.SHARP -> 10.dp
                            }
                            val standardRestingCorner = lerp(baseStandardCorner, expandedStandardCorner, scientificExpandProgress)
                            val baseStandardPressedCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 15.dp
                                ButtonShapeStyle.SQUIRCLE -> 12.dp
                                ButtonShapeStyle.SHARP -> 7.dp
                            }
                            val expandedStandardPressedCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 13.dp
                                ButtonShapeStyle.SQUIRCLE -> 10.dp
                                ButtonShapeStyle.SHARP -> 6.dp
                            }
                            val standardPressedCorner = lerp(baseStandardPressedCorner, expandedStandardPressedCorner, scientificExpandProgress)
                            val sciRestingCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 15.dp
                                ButtonShapeStyle.SQUIRCLE -> 13.dp
                                ButtonShapeStyle.SHARP -> 8.dp
                            }
                            val sciPressedCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 10.dp
                                ButtonShapeStyle.SQUIRCLE -> 9.dp
                                ButtonShapeStyle.SHARP -> 5.dp
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 980.dp)
                            ) {
                                if (isWideOrLandscape) {
                                    CalculatorDisplay(
                                        state = state,
                                        onToggleTheme = { triggerThemeReveal { viewModel.cycleThemeMode() } },
                                        onToggleSound = viewModel::toggleSoundEnabled,
                                        onToggleVibration = { viewModel.toggleVibrationEnabled(view) },
                                        onQuickToggleFeedback = { sound, vib ->
                                            viewModel.setFeedbackEnabled(sound, vib, view)
                                        },
                                        onToggleTopHistory = viewModel::toggleTopHistory,
                                        onSetTopHistoryOpen = viewModel::setTopHistoryOpen,
                                        onOpenFullHistory = { viewModel.setFullHistorySheetOpen(true) },
                                        onOpenSettings = { viewModel.setSettingsSheetOpen(true) },
                                        onInspectBigNumber = viewModel::openBigNumberViewer,
                                        compactMode = true,
                                        onThemeButtonPositioned = { themeButtonAnchorRoot = it },
                                        modifier = Modifier.onSizeChanged { displayCardHeightPx = it.height }
                                    )

                                    ScientificToggleStrip(
                                        isExpanded = state.isScientificExpanded,
                                        onToggle = viewModel::toggleScientificExpanded,
                                        onOpenVoiceInput = { viewModel.setVoiceInputDialogOpen(true) }
                                    )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    lerp(0.dp, 14.dp, scientificExpandProgress)
                                )
                            ) {
                                val containerWidth = this@BoxWithConstraints.maxWidth.coerceAtMost(980.dp)
                                val fullSciWidth = ((containerWidth - 54.dp) * 0.45f).coerceAtLeast(180.dp)
                                if (scientificExpandProgress > 0.001f || state.isScientificExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clipToBounds()
                                            .layout { measurable, constraints ->
                                                val targetFullPx = fullSciWidth.roundToPx()
                                                    .coerceAtMost(constraints.maxWidth)
                                                val placeable = measurable.measure(
                                                    constraints.copy(minWidth = targetFullPx, maxWidth = targetFullPx)
                                                )
                                                val visibleW = (placeable.width * scientificExpandProgress)
                                                    .roundToInt()
                                                    .coerceIn(0, placeable.width)
                                                layout(visibleW, placeable.height) {
                                                    placeable.placeRelativeWithLayer(
                                                        x = visibleW - placeable.width,
                                                        y = 0
                                                    ) {
                                                        alpha = scientificExpandProgress
                                                    }
                                                }
                                            }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                                            ),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                                ScientificKeypadGrid(
                                                    isInverse = state.isInverseMode,
                                                    angleUnit = state.angleUnit,
                                                    viewModel = viewModel,
                                                    restingCornerRadius = sciRestingCorner,
                                                    pressedCornerRadius = sciPressedCorner,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    StandardKeypadGrid(
                                        viewModel = viewModel,
                                        rowHeight = null,
                                        rowSpacing = 10.dp,
                                        textScale = 1f,
                                        restingCornerRadius = standardRestingCorner,
                                        pressedCornerRadius = standardPressedCorner,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            } else {
                                CalculatorDisplay(
                                    state = state,
                                    onToggleTheme = { triggerThemeReveal { viewModel.cycleThemeMode() } },
                                    onToggleSound = viewModel::toggleSoundEnabled,
                                    onToggleVibration = { viewModel.toggleVibrationEnabled(view) },
                                    onQuickToggleFeedback = { sound, vib ->
                                        viewModel.setFeedbackEnabled(sound, vib, view)
                                    },
                                    onToggleTopHistory = viewModel::toggleTopHistory,
                                    onSetTopHistoryOpen = viewModel::setTopHistoryOpen,
                                    onOpenFullHistory = { viewModel.setFullHistorySheetOpen(true) },
                                    onOpenSettings = { viewModel.setSettingsSheetOpen(true) },
                                    onInspectBigNumber = viewModel::openBigNumberViewer,
                                    compactMode = isCompactVertical,
                                    onThemeButtonPositioned = { themeButtonAnchorRoot = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 190.dp)
                                        .onSizeChanged { displayCardHeightPx = it.height }
                                )

                                ScientificToggleStrip(
                                    isExpanded = state.isScientificExpanded,
                                    onToggle = viewModel::toggleScientificExpanded,
                                    onOpenVoiceInput = { viewModel.setVoiceInputDialogOpen(true) }
                                )

                                val portraitPadVertical = lerp(8.dp, 6.dp, scientificExpandProgress)
                                val sciBottomPadding = if (isCompactVertical) 7.dp else 9.dp
                                val sciTrayHeight = if (isCompactVertical) 114.dp else 128.dp

                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 18.dp, vertical = portraitPadVertical)
                                    ) {
                                        if (scientificExpandProgress > 0.001f || state.isScientificExpanded) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clipToBounds()
                                                    .layout { measurable, constraints ->
                                                        val placeable = measurable.measure(constraints)
                                                        val visibleH = (placeable.height * scientificExpandProgress)
                                                            .roundToInt()
                                                            .coerceIn(0, placeable.height)
                                                        layout(placeable.width, visibleH) {
                                                            placeable.placeRelativeWithLayer(
                                                                x = 0,
                                                                y = visibleH - placeable.height
                                                            ) {
                                                                alpha = scientificExpandProgress
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(24.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = sciBottomPadding)
                                                        .height(sciTrayHeight)
                                                ) {
                                                    ScientificKeypadGrid(
                                                        isInverse = state.isInverseMode,
                                                        angleUnit = state.angleUnit,
                                                        viewModel = viewModel,
                                                        rowSpacing = if (isCompactVertical) 5.dp else 6.dp,
                                                        restingCornerRadius = sciRestingCorner,
                                                        pressedCornerRadius = sciPressedCorner,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 8.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                        }

                                        StandardKeypadGrid(
                                            viewModel = viewModel,
                                            rowHeight = targetStandardRowHeight,
                                            rowSpacing = targetStandardRowSpacing,
                                            textScale = keypadTextScale,
                                            restingCornerRadius = standardRestingCorner,
                                            pressedCornerRadius = standardPressedCorner,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                TopPullDownHistoryOverlay(
                    visible = state.isTopHistoryOpen,
                    history = state.history,
                    maxHeight = quickHistoryMaxHeight,
                    onDismiss = { viewModel.setTopHistoryOpen(false) },
                    onOpenFullHistory = { viewModel.setFullHistorySheetOpen(true) },
                    onUseExpression = viewModel::useHistoryExpression,
                    onUseResult = viewModel::useHistoryResult,
                    onCopyResult = copyToClipboard
                )

                VoiceMathInputOverlay(
                    visible = state.isVoiceInputDialogOpen,
                    angleUnit = state.angleUnit,
                    onInsertExpression = { expr ->
                        viewModel.applyVoiceExpression(expr, evaluateImmediately = false)
                    },
                    onEvaluateExpression = { expr ->
                        viewModel.applyVoiceExpression(expr, evaluateImmediately = true)
                    },
                        onDismiss = { viewModel.setVoiceInputDialogOpen(false) }
                    )
                }
            }
        }

            if (isThemeRevealing) {
                val revealRimColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = revealRadiusFraction.value
                    val origin = revealOriginRoot?.let { it - rootBoxOrigin }
                        ?: Offset(size.width * 0.16f, size.height * 0.08f)
                    val maxRadius = hypot(
                        maxOf(origin.x, size.width - origin.x),
                        maxOf(origin.y, size.height - origin.y)
                    ) * 1.04f
                    val radius = (maxRadius * progress).coerceAtLeast(1f)

                    revealClipPath.reset()
                    revealClipPath.addOval(
                        Rect(
                            left = origin.x - radius,
                            top = origin.y - radius,
                            right = origin.x + radius,
                            bottom = origin.y + radius
                        )
                    )

                    clipPath(revealClipPath, clipOp = ClipOp.Difference) {
                        drawLayer(oldThemeLayer)
                    }

                    val rimFade = (1f - progress).coerceIn(0f, 1f)
                    if (rimFade > 0.01f) {
                        drawCircle(
                            color = revealRimColor.copy(alpha = 0.20f * rimFade),
                            radius = radius,
                            center = origin,
                            style = Stroke(width = 7.dp.toPx())
                        )
                        drawCircle(
                            color = revealRimColor.copy(alpha = 0.42f * rimFade),
                            radius = radius,
                            center = origin,
                            style = Stroke(width = 1.75.dp.toPx())
                        )
                    }
                }
            }
        }

        if (state.isFullHistorySheetOpen) {
            FullHistoryBottomSheet(
                history = state.history,
                onDismiss = { viewModel.setFullHistorySheetOpen(false) },
                onUseExpression = viewModel::useHistoryExpression,
                onUseResult = viewModel::useHistoryResult,
                onCopyResult = copyToClipboard,
                onInspectBigNumber = viewModel::openBigNumberViewer,
                onDeleteRecord = viewModel::deleteHistoryRecord,
                onClearAll = viewModel::clearAllHistory
            )
        }

        if (state.isSettingsSheetOpen) {
            SettingsBottomSheet(
                state = state,
                onDismiss = { viewModel.setSettingsSheetOpen(false) },
                onToggleSound = viewModel::toggleSoundEnabled,
                onChangeSoundVolume = viewModel::setSoundVolume,
                onPreviewSoundVolume = viewModel::previewSoundVolume,
                onToggleVibration = { viewModel.toggleVibrationEnabled(view) },
                onSelectHapticIntensity = { intensity -> viewModel.setHapticIntensity(intensity, view) },
                onSelectThemeMode = { mode ->
                    if (mode != state.themeMode) {
                        triggerThemeReveal { viewModel.setThemeMode(mode) }
                    }
                },
                onToggleDynamicColor = viewModel::setUseDynamicColor,
                onSelectCustomAccentArgb = viewModel::setCustomAccentArgb,
                onSelectButtonShape = viewModel::setButtonShapeStyle,
                onToggleLivePreview = viewModel::setShowLivePreview,
                onToggleAmbientDisplayGlow = viewModel::setAmbientDisplayGlow,
                onResetDefaults = viewModel::resetCustomizationsToDefaults
            )
        }

        state.bigNumberViewerRecord?.let { (exprLabel, plainDigits) ->
            BigNumberViewerDialog(
                expressionLabel = exprLabel,
                plainDigits = plainDigits,
                onCopy = copyToClipboard,
                onDismiss = viewModel::closeBigNumberViewer
            )
        }

        if (state.pendingFeedbackPermission != null) {
            FeedbackPermissionDialog(
                onAllow = { viewModel.confirmFeedbackPermission(view) },
                onDismiss = viewModel::dismissFeedbackPermission
            )
        }
    }
}

@Composable
private fun ScientificToggleStrip(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenVoiceInput: () -> Unit
) {
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scientific_chevron"
    )
    val fxInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val chipTextStyle = MaterialTheme.typography.labelLarge.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(
                    interactionSource = fxInteractionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        playSound(com.example.audio.CalcSoundType.OPERATOR)
                        performHaptic(com.example.audio.CalcSoundType.OPERATOR)
                        onToggle()
                    }
                )
                .testTag("toggle_scientific_panel"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier
                    .height(35.dp)
                    .clip(CircleShape)
                    .indication(fxInteractionSource, ripple(bounded = true))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Functions,
                        contentDescription = stringResource(R.string.cd_scientific_toggle),
                        tint = if (isExpanded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "fx",
                        style = chipTextStyle,
                        color = if (isExpanded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isExpanded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(17.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
        }

        GlowingActionPill(
            label = stringResource(R.string.voice_input_chip),
            icon = Icons.Rounded.Mic,
            contentDescription = stringResource(R.string.cd_voice_input),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            glowColor = MaterialTheme.colorScheme.tertiary,
            testTag = "open_voice_input_button",
            onClick = {
                playSound(com.example.audio.CalcSoundType.OPERATOR)
                performHaptic(com.example.audio.CalcSoundType.OPERATOR)
                onOpenVoiceInput()
            }
        )
    }
}

@Composable
private fun GlowingActionPill(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    glowColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_press_scale"
    )
    val glowPulse = remember { Animatable(0f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            glowPulse.snapTo(1f)
        } else if (glowPulse.value > 0f) {
            glowPulse.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    val surfaceBg = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceBg) { surfaceBg.luminance() < 0.5f }
    val softGlowColor = remember(glowColor, isDark) {
        if (isDark) lerp(glowColor, Color.White, 0.22f) else glowColor
    }
    val innerTintColor = remember(glowColor, isDark) {
        lerp(glowColor, Color.White, if (isDark) 0.45f else 0.58f)
    }

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(35.dp)
                .graphicsLayer {
                    val s = pressScaleState.value
                    scaleX = s
                    scaleY = s
                }
                .drawBehind {
                    val pulse = maxOf(glowPulse.value, if (isPressed) 0.7f else 0f)
                    val baseAlpha = (if (isDark) 0.28f else 0.22f) + pulse * 0.18f
                    val maxSpreadPx = (4.dp + (pulse * 2f).dp).toPx()
                    val halfH = size.height / 2f
                    val steps = 5
                    for (i in steps downTo 1) {
                        val t = i.toFloat() / steps.toFloat()
                        val expand = maxSpreadPx * t
                        val layerAlpha = baseAlpha * (1f - t * 0.70f) * 0.34f
                        val corner = halfH + expand
                        drawRoundRect(
                            color = softGlowColor.copy(alpha = layerAlpha),
                            topLeft = Offset(-expand, -expand),
                            size = Size(size.width + expand * 2f, size.height + expand * 2f),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                    }
                }
                .clip(CircleShape)
                .background(containerColor)
                .indication(interactionSource, ripple(bounded = true))
                .drawBehind {
                    val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                    val topSheenAlpha = 0.14f + pulse * 0.12f
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to innerTintColor.copy(alpha = topSheenAlpha),
                            0.55f to innerTintColor.copy(alpha = topSheenAlpha * 0.28f),
                            1.0f to Color.Transparent
                        )
                    )
                    val strokePx = 1.dp.toPx()
                    val cornerPx = (size.height / 2f).coerceAtLeast(0f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to innerTintColor.copy(alpha = 0.36f + pulse * 0.22f),
                            1.0f to softGlowColor.copy(alpha = 0.12f + pulse * 0.12f)
                        ),
                        topLeft = Offset(strokePx / 2f, strokePx / 2f),
                        size = Size(
                            (size.width - strokePx).coerceAtLeast(0f),
                            (size.height - strokePx).coerceAtLeast(0f)
                        ),
                        cornerRadius = CornerRadius(
                            (cornerPx - strokePx / 2f).coerceAtLeast(0f),
                            (cornerPx - strokePx / 2f).coerceAtLeast(0f)
                        ),
                        style = Stroke(width = strokePx)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun ScientificKeypadGrid(
    isInverse: Boolean,
    angleUnit: AngleUnit,
    viewModel: CalculatorViewModel,
    rowSpacing: Dp = 6.dp,
    restingCornerRadius: Dp = 15.dp,
    pressedCornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val spacing = rowSpacing
    val sciRestingCorner = restingCornerRadius
    val sciPressedCorner = pressedCornerRadius
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1: DEG/RAD, INV, π, e, ±
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ExpressiveCalcButton(
                label = angleUnit.name,
                role = CalcButtonRole.SCIENTIFIC_ACTIVE,
                testTag = "btn_sci_angle_unit",
                onClick = viewModel::toggleAngleUnit,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "INV",
                role = if (isInverse) CalcButtonRole.SCIENTIFIC_ACTIVE else CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_inv",
                onClick = viewModel::toggleInverseMode,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "π",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_pi",
                onClick = { viewModel.onConstant("π") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "e",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_e",
                onClick = { viewModel.onConstant("e") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "±",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_sign",
                onClick = viewModel::onToggleSign,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 2: sin/sin⁻¹, cos/cos⁻¹, tan/tan⁻¹, ln/eˣ, log/10ˣ
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ExpressiveCalcButton(
                label = if (isInverse) "sin⁻¹" else "sin",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_sin",
                onClick = { viewModel.onFunction(if (isInverse) "sin⁻¹(" else "sin(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "cos⁻¹" else "cos",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_cos",
                onClick = { viewModel.onFunction(if (isInverse) "cos⁻¹(" else "cos(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "tan⁻¹" else "tan",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_tan",
                onClick = { viewModel.onFunction(if (isInverse) "tan⁻¹(" else "tan(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "eˣ" else "ln",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_ln",
                onClick = { if (isInverse) viewModel.onExpE() else viewModel.onFunction("ln(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "10ˣ" else "log",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_log",
                onClick = { if (isInverse) viewModel.onExp10() else viewModel.onFunction("log(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 3: √/x², (, ), xʸ, x!
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ExpressiveCalcButton(
                label = if (isInverse) "x²" else "√",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_sqrt",
                onClick = { if (isInverse) viewModel.onSquare() else viewModel.onFunction("√(") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "(",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_lparen",
                onClick = viewModel::onOpenParenthesis,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = ")",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_rparen",
                onClick = viewModel::onCloseParenthesis,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "xʸ",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_pow",
                onClick = { viewModel.onOperator("^") },
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "x!",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_fact",
                onClick = viewModel::onFactorial,
                restingCornerRadius = sciRestingCorner,
                pressedCornerRadius = sciPressedCorner,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun StandardKeypadGrid(
    viewModel: CalculatorViewModel,
    rowHeight: Dp?,
    rowSpacing: Dp = 11.dp,
    textScale: Float = 1f,
    restingCornerRadius: Dp = 50.dp,
    pressedCornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val spacing = rowSpacing
    val operatorGlyphStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = (32f * textScale).sp,
        lineHeight = (32f * textScale).sp,
        fontWeight = FontWeight.Medium,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val r1Mod = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight) else Modifier.weight(1f).fillMaxWidth()
        val r2Mod = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight) else Modifier.weight(1f).fillMaxWidth()
        val r3Mod = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight) else Modifier.weight(1f).fillMaxWidth()
        val r4Mod = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight) else Modifier.weight(1f).fillMaxWidth()
        val r5Mod = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight) else Modifier.weight(1f).fillMaxWidth()

        // Row 1: AC, (), %, ÷
        Row(
            modifier = r1Mod,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ExpressiveCalcButton(
                label = "AC",
                role = CalcButtonRole.UTILITY,
                testTag = "btn_ac",
                hasGlow = true,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                onClick = viewModel::onAllClear,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "( )",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_paren",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                onClick = viewModel::onSmartParenthesis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val parenStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = (24f * textScale).sp,
                    fontWeight = FontWeight.Normal
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8f * textScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "(", style = parenStyle)
                    Text(text = ")", style = parenStyle)
                }
            }
            ExpressiveCalcButton(
                label = "%",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_percent",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                onClick = viewModel::onPercent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "÷",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_div",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("÷") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = r2Mod,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            DigitButton("7", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("8", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("9", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "×",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_mul",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("×") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = r3Mod,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            DigitButton("4", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("5", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("6", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "−",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_sub",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("−") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = r4Mod,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            DigitButton("1", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("2", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            DigitButton("3", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "+",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_add",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("+") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 5: 0, ., ⌫, =
        Row(
            modifier = r5Mod,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            DigitButton("0", viewModel, textScale, restingCornerRadius, pressedCornerRadius, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = ".",
                role = CalcButtonRole.DIGIT,
                testTag = "btn_dot",
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                onClick = viewModel::onDecimalPoint,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "⌫",
                role = CalcButtonRole.UTILITY,
                testTag = "btn_backspace",
                hasGlow = true,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                contentDescriptionText = stringResource(R.string.cd_backspace),
                onClick = viewModel::onBackspace,
                onLongClick = viewModel::onAllClear,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = stringResource(R.string.cd_backspace),
                    modifier = Modifier.size((21f * textScale).dp)
                )
            }
            ExpressiveCalcButton(
                label = "=",
                role = CalcButtonRole.EQUALS,
                testTag = "btn_equals",
                hasGlow = true,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textStyle = operatorGlyphStyle,
                onClick = viewModel::onEquals,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DigitButton(
    digit: String,
    viewModel: CalculatorViewModel,
    textScale: Float = 1f,
    restingCornerRadius: Dp = 50.dp,
    pressedCornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    ExpressiveCalcButton(
        label = digit,
        role = CalcButtonRole.DIGIT,
        testTag = "btn_$digit",
        restingCornerRadius = restingCornerRadius,
        pressedCornerRadius = pressedCornerRadius,
        textScale = textScale,
        onClick = { viewModel.onDigit(digit) },
        modifier = modifier
    )
}
