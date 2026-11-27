package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AngleUnit
import com.example.data.preferences.ButtonShapeStyle
import com.example.data.preferences.GlowTarget
import com.example.data.preferences.KeyPressEffect
import com.example.data.preferences.PressSparkStyle
import com.example.data.preferences.ThemeAnimStyle
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
import com.example.ui.components.ScientificCalcButton
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
    var captureOldTheme by remember { mutableStateOf(false) }
    var isThemeRevealing by remember { mutableStateOf(false) }
    var revealOriginRoot by remember { mutableStateOf<Offset?>(null) }
    var revealJob by remember { mutableStateOf<Job?>(null) }

    val revealDiagPath = remember { Path() }
    val revealShape = remember(state.themeAnimStyle) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val progress = revealRadiusFraction.value.coerceIn(0f, 1f)
                return when (state.themeAnimStyle) {
                    ThemeAnimStyle.CIRCLE_REVEAL -> {
                        val origin = revealOriginRoot?.let { it - rootBoxOrigin }
                            ?: Offset(size.width * 0.16f, size.height * 0.08f)
                        val maxRadius = hypot(
                            maxOf(origin.x, size.width - origin.x),
                            maxOf(origin.y, size.height - origin.y)
                        ) * 1.02f
                        val r = (maxRadius * progress).coerceAtLeast(1f)
                        Outline.Rounded(
                            RoundRect(
                                left = origin.x - r,
                                top = origin.y - r,
                                right = origin.x + r,
                                bottom = origin.y + r,
                                cornerRadius = CornerRadius(r, r)
                            )
                        )
                    }
                    ThemeAnimStyle.DIAGONAL_WIPE -> {
                        val d = (size.width + size.height) * progress
                        revealDiagPath.reset()
                        revealDiagPath.moveTo(0f, 0f)
                        revealDiagPath.lineTo(minOf(d, size.width), 0f)
                        if (d > size.width) {
                            revealDiagPath.lineTo(size.width, minOf(d - size.width, size.height))
                        }
                        if (d > size.height) {
                            revealDiagPath.lineTo(minOf(d - size.height, size.width), size.height)
                        }
                        revealDiagPath.lineTo(0f, minOf(d, size.height))
                        revealDiagPath.close()
                        Outline.Generic(revealDiagPath)
                    }
                    ThemeAnimStyle.VERTICAL_CURTAIN -> {
                        val h = (size.height * progress).coerceAtLeast(1f)
                        Outline.Rectangle(Rect(0f, 0f, size.width, h))
                    }
                    ThemeAnimStyle.CROSSFADE,
                    ThemeAnimStyle.INSTANT -> {
                        Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
                    }
                }
            }
        }
    }

    val currentThemeAnimStyle by rememberUpdatedState(state.themeAnimStyle)
    val triggerThemeReveal: (() -> Unit) -> Unit = { applyThemeChange ->
        if (currentThemeAnimStyle == ThemeAnimStyle.INSTANT) {
            revealJob?.cancel()
            revealJob = null
            captureOldTheme = false
            isThemeRevealing = false
            applyThemeChange()
        } else if (isThemeRevealing || captureOldTheme) {
            revealJob?.cancel()
            revealJob = null
            captureOldTheme = false
            isThemeRevealing = false
            applyThemeChange()
        } else {
            revealOriginRoot = themeButtonAnchorRoot
            captureOldTheme = true
            revealJob = scope.launch {
                try {
                    withFrameNanos { }
                    withFrameNanos { }
                    revealRadiusFraction.snapTo(0.02f)
                    isThemeRevealing = true
                    captureOldTheme = false
                    applyThemeChange()
                    val duration = when (currentThemeAnimStyle) {
                        ThemeAnimStyle.CROSSFADE -> 300
                        ThemeAnimStyle.VERTICAL_CURTAIN -> 340
                        ThemeAnimStyle.DIAGONAL_WIPE -> 360
                        else -> 360
                    }
                    revealRadiusFraction.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = CubicBezierEasing(0.22f, 0.88f, 0.26f, 1.0f)
                        )
                    )
                } finally {
                    captureOldTheme = false
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
            if (isThemeRevealing) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLayer(oldThemeLayer)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isThemeRevealing) {
                            when (state.themeAnimStyle) {
                                ThemeAnimStyle.CROSSFADE -> {
                                    alpha = revealRadiusFraction.value.coerceIn(0f, 1f)
                                }
                                ThemeAnimStyle.CIRCLE_REVEAL,
                                ThemeAnimStyle.DIAGONAL_WIPE,
                                ThemeAnimStyle.VERTICAL_CURTAIN -> {
                                    clip = true
                                    shape = revealShape
                                }
                                ThemeAnimStyle.INSTANT -> Unit
                            }
                        } else {
                            shape = RectangleShape
                        }
                    }
                    .drawWithContent {
                        if (captureOldTheme) {
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
                    val displayCardHeightPxRef = remember { IntArray(1) }
                    val density = LocalDensity.current
                    val quickHistoryMaxHeight = if (state.isTopHistoryOpen) {
                        val px = displayCardHeightPxRef[0]
                        if (px > 0) with(density) { px.toDp() } else 240.dp
                    } else {
                        240.dp
                    }

                    // Do not delegate with `by` in composition: read inside graphicsLayer for zero-recompose 120Hz
                    val scientificExpandState = animateFloatAsState(
                        targetValue = if (state.isScientificExpanded) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = 680f
                        ),
                        label = "scientific_expand_progress"
                    )
                    val isSciExpandedState = rememberUpdatedState(state.isScientificExpanded)
                    val isSciPanelVisible by remember(scientificExpandState) {
                        derivedStateOf { isSciExpandedState.value || scientificExpandState.value > 0.001f }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val isWideOrLandscape = maxWidth >= 600.dp || maxWidth > maxHeight
                            val isCompactVertical = maxHeight < 700.dp

                            val keypadScale = state.keypadScale.coerceIn(0.80f, 1.18f)
                            val fxScale = state.fxScale.coerceIn(0.78f, 1.25f)
                            val keypadTextScale = (0.85f + 0.15f * keypadScale).coerceIn(0.84f, 1.12f)
                            val fxTextScale = (0.86f + 0.14f * fxScale).coerceIn(0.84f, 1.14f)

                            val resolvedGlowColor = remember(state.glowColorPreset) {
                                state.glowColorPreset.argb?.let { Color(it) }
                            }

                            val baseStdRowHeight = if (isCompactVertical) 49.dp else 53.dp
                            val baseStdRowSpacing = if (isCompactVertical) 6.dp else 8.dp
                            val stdRowSpacing = (baseStdRowSpacing * (0.85f + 0.15f * keypadScale))
                                .coerceIn(4.dp, 10.dp)

                            val baseFxTrayHeight = if (isCompactVertical) 94.dp else 106.dp
                            val desiredFxTrayHeight = baseFxTrayHeight * fxScale
                            val fxBottomGap = if (state.fxUnifiedWithKeypad) {
                                if (isCompactVertical) 10.dp else 12.dp
                            } else {
                                if (isCompactVertical) 8.dp else 10.dp
                            }

                            val maxStdBudget = (maxHeight - 235.dp - (desiredFxTrayHeight * 0.55f))
                                .coerceAtLeast(210.dp)
                            val maxAllowedStdRowH = ((maxStdBudget - stdRowSpacing * 4f - 14.dp) / 5f)
                                .coerceIn(38.dp, 68.dp)
                            val stdRowHeight = (baseStdRowHeight * keypadScale)
                                .coerceIn(38.dp, maxAllowedStdRowH)

                            val keypadTotalEstimate = stdRowHeight * 5f + stdRowSpacing * 4f + 16.dp
                            val maxAllowedFxH = (maxHeight - keypadTotalEstimate - 220.dp)
                                .coerceIn(72.dp, 142.dp)
                            val fxTrayHeight = desiredFxTrayHeight.coerceIn(72.dp, maxAllowedFxH)

                            val standardRestingCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 50.dp
                                ButtonShapeStyle.SQUIRCLE -> 20.dp
                                ButtonShapeStyle.SHARP -> 11.dp
                            }
                            val standardPressedCorner = when (state.buttonShapeStyle) {
                                ButtonShapeStyle.PILL -> 14.dp
                                ButtonShapeStyle.SQUIRCLE -> 11.dp
                                ButtonShapeStyle.SHARP -> 6.dp
                            }

                            val portraitColOriginRef = remember { FloatArray(2) }
                            val portraitKeypadInnerRectRef = remember { FloatArray(4) } // left, top, right, bottom in Column coords
                            val portraitDeckBg = MaterialTheme.colorScheme.surfaceContainerLow
                            val portraitDeckBorder = MaterialTheme.colorScheme.outlineVariant

                            val fxBottomGapPx = with(density) { fxBottomGap.toPx() }
                            val fxTotalPx = with(density) { (fxTrayHeight + fxBottomGap).toPx() }
                            val topPadDp = 8.dp
                            val topPadPx = with(density) { topPadDp.toPx() }
                            val deckPadTopPx = with(density) { 8.dp.toPx() }

                            val collapsedKeypadInnerDp = stdRowHeight * 5f + stdRowSpacing * 4f
                            val rawCompressedRowH = (stdRowHeight * 0.78f).coerceAtLeast(37.dp)
                            val rawCompressedGap = (stdRowSpacing * 0.82f).coerceAtLeast(5.dp)
                            val rawCompressedInnerDp = rawCompressedRowH * 5f + rawCompressedGap * 4f
                            val rawSavedDp = (collapsedKeypadInnerDp - rawCompressedInnerDp).coerceAtLeast(0.dp)
                            val maxAllowedSavedDp = (fxTrayHeight + fxBottomGap - topPadDp).coerceAtLeast(0.dp)
                            val compressRatio = if (rawSavedDp > 0.dp && rawSavedDp > maxAllowedSavedDp) {
                                (maxAllowedSavedDp / rawSavedDp).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                            val unifiedCompressedRowH = stdRowHeight - (stdRowHeight - rawCompressedRowH) * compressRatio
                            val unifiedCompressedGap = stdRowSpacing - (stdRowSpacing - rawCompressedGap) * compressRatio
                            val unifiedExpandedInnerDp = unifiedCompressedRowH * 5f + unifiedCompressedGap * 4f
                            val unifiedSavedPx = with(density) {
                                (collapsedKeypadInnerDp - unifiedExpandedInnerDp).coerceAtLeast(0.dp).toPx()
                            }
                            val unifiedTargetShiftPx = if (state.fxUnifiedWithKeypad) {
                                topPadPx + unifiedSavedPx
                            } else {
                                0f
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 980.dp)
                                    .onGloballyPositioned { coords ->
                                        if (coords.isAttached) {
                                            val pos = coords.positionInRoot()
                                            portraitColOriginRef[0] = pos.x
                                            portraitColOriginRef[1] = pos.y
                                        }
                                    }
                                    .drawBehind {
                                        if (!isWideOrLandscape && state.fxUnifiedWithKeypad) {
                                            val p = scientificExpandState.value.coerceIn(0f, 1f)
                                            val kW = portraitKeypadInnerRectRef[2] - portraitKeypadInnerRectRef[0]
                                            if (p > 0.01f && kW > 1f) {
                                                val padX = 8.dp.toPx()
                                                val padBot = 7.dp.toPx()
                                                val kLeft = portraitKeypadInnerRectRef[0]
                                                val kTop = portraitKeypadInnerRectRef[1]
                                                val kBottom = portraitKeypadInnerRectRef[3]
                                                val deckTopY = kTop + (unifiedSavedPx - fxTotalPx) * p - deckPadTopPx
                                                val deckCorner = 24.dp.toPx()
                                                val rectTopLeft = Offset(kLeft - padX, deckTopY)
                                                val rectSize = Size(
                                                    kW + padX * 2f,
                                                    (kBottom + padBot) - deckTopY
                                                )
                                                drawRoundRect(
                                                    color = portraitDeckBg.copy(alpha = 0.94f * p),
                                                    topLeft = rectTopLeft,
                                                    size = rectSize,
                                                    cornerRadius = CornerRadius(deckCorner, deckCorner)
                                                )
                                                drawRoundRect(
                                                    color = portraitDeckBorder.copy(alpha = 0.28f * p),
                                                    topLeft = rectTopLeft,
                                                    size = rectSize,
                                                    cornerRadius = CornerRadius(deckCorner, deckCorner),
                                                    style = Stroke(width = 1.dp.toPx())
                                                )
                                                val sepY = kTop + unifiedSavedPx * p - fxBottomGapPx * 0.5f
                                                if (sepY > deckTopY + 8.dp.toPx() && sepY < kBottom) {
                                                    drawLine(
                                                        color = portraitDeckBorder.copy(alpha = 0.26f * p),
                                                        start = Offset(kLeft + 6.dp.toPx(), sepY),
                                                        end = Offset(kLeft + kW - 6.dp.toPx(), sepY),
                                                        strokeWidth = 1.dp.toPx()
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                                        modifier = Modifier.onSizeChanged { displayCardHeightPxRef[0] = it.height }
                                    )

                                    ScientificToggleStrip(
                                        isExpanded = state.isScientificExpanded,
                                        onToggle = viewModel::toggleScientificExpanded,
                                        onOpenVoiceInput = { viewModel.setVoiceInputDialogOpen(true) },
                                        glowTarget = state.glowTarget,
                                        glowIntensity = state.glowIntensity,
                                        glowColorOverride = resolvedGlowColor
                                    )

                                    val landscapeUnifiedSurfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    val landscapeUnifiedOutline = MaterialTheme.colorScheme.outlineVariant
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 20.dp, vertical = 8.dp)
                                            .drawBehind {
                                                if (state.fxUnifiedWithKeypad) {
                                                    val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                    if (p > 0.01f) {
                                                        val pad = 6.dp.toPx()
                                                        val corner = 24.dp.toPx()
                                                        drawRoundRect(
                                                            color = landscapeUnifiedSurfaceColor.copy(alpha = 0.86f * p),
                                                            topLeft = Offset(-pad, -pad),
                                                            size = Size(size.width + pad * 2f, size.height + pad * 2f),
                                                            cornerRadius = CornerRadius(corner, corner)
                                                        )
                                                        drawRoundRect(
                                                            color = landscapeUnifiedOutline.copy(alpha = 0.24f * p),
                                                            topLeft = Offset(-pad, -pad),
                                                            size = Size(size.width + pad * 2f, size.height + pad * 2f),
                                                            cornerRadius = CornerRadius(corner, corner),
                                                            style = Stroke(width = 1.dp.toPx())
                                                        )
                                                    }
                                                }
                                            }
                                    ) {
                                        if (isSciPanelVisible) {
                                            val containerWidth = this@BoxWithConstraints.maxWidth.coerceAtMost(980.dp)
                                            val sciRatio = (0.45f * (0.82f + 0.18f * fxScale)).coerceIn(0.35f, 0.54f)
                                            val fullSciWidth = ((containerWidth - 54.dp) * sciRatio).coerceAtLeast(165.dp)
                                            val targetGapDp = if (state.fxUnifiedWithKeypad) 6.dp else 14.dp
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .clipToBounds()
                                                    .layout { measurable, constraints ->
                                                        val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                        val targetFullPx = fullSciWidth.roundToPx()
                                                            .coerceAtMost(constraints.maxWidth)
                                                        val gapPx = (targetGapDp.toPx() * p).roundToInt()
                                                        val placeable = measurable.measure(
                                                            constraints.copy(minWidth = targetFullPx, maxWidth = targetFullPx)
                                                        )
                                                        val visibleW = (placeable.width * p)
                                                            .roundToInt()
                                                            .coerceIn(0, placeable.width)
                                                        layout(visibleW + if (visibleW > 0) gapPx else 0, placeable.height) {
                                                            if (visibleW > 0) {
                                                                placeable.placeRelative(0, 0)
                                                            }
                                                        }
                                                    }
                                            ) {
                                                FxScientificPanel(
                                                    isInverse = state.isInverseMode,
                                                    angleUnit = state.angleUnit,
                                                    buttonShapeStyle = state.buttonShapeStyle,
                                                    viewModel = viewModel,
                                                    enabled = state.isScientificExpanded,
                                                    columns = 3,
                                                    rowSpacing = (7.dp * (0.85f + 0.15f * fxScale)).coerceIn(5.dp, 10.dp),
                                                    textScale = fxTextScale,
                                                    glowTarget = state.glowTarget,
                                                    glowIntensity = state.glowIntensity,
                                                    glowColorOverride = resolvedGlowColor,
                                                    colorTintOperators = state.colorTintOperators,
                                                    colorTintUtilities = state.colorTintUtilities,
                                                    colorTintFx = state.colorTintFx,
                                                    keyPressEffect = state.keyPressEffect,
                                                    pressSparksEnabled = state.pressSparksEnabled,
                                                    pressSparkStyle = state.pressSparkStyle,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer {
                                                            val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                            translationX = -size.width * (1f - p)
                                                        }
                                                )
                                            }
                                        }

                                        StandardKeypadGrid(
                                            viewModel = viewModel,
                                            rowHeight = null,
                                            rowSpacing = (10.dp * (0.85f + 0.15f * keypadScale)).coerceIn(6.dp, 13.dp),
                                            restingCornerRadius = standardRestingCorner,
                                            pressedCornerRadius = standardPressedCorner,
                                            textScale = keypadTextScale,
                                            glowTarget = state.glowTarget,
                                            glowIntensity = state.glowIntensity,
                                            glowColorOverride = resolvedGlowColor,
                                            colorTintDigits = state.colorTintDigits,
                                            colorTintOperators = state.colorTintOperators,
                                            colorTintUtilities = state.colorTintUtilities,
                                            keyPressEffect = state.keyPressEffect,
                                            pressSparksEnabled = state.pressSparksEnabled,
                                            pressSparkStyle = state.pressSparkStyle,
                                            glowBreathingEnabled = state.glowBreathingEnabled,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                } else {
                                    val displayLiftPxState = remember(
                                        scientificExpandState,
                                        fxTotalPx,
                                        unifiedTargetShiftPx,
                                        deckPadTopPx,
                                        state.fxUnifiedWithKeypad
                                    ) {
                                        derivedStateOf {
                                            val p = scientificExpandState.value.coerceIn(0f, 1f)
                                            if (state.fxUnifiedWithKeypad) {
                                                ((fxTotalPx - unifiedTargetShiftPx + deckPadTopPx).coerceAtLeast(0f)) * p
                                            } else {
                                                fxTotalPx * p
                                            }
                                        }
                                    }
                                    val keypadContentScaleState = remember(
                                        scientificExpandState,
                                        stdRowHeight,
                                        unifiedCompressedRowH,
                                        state.fxUnifiedWithKeypad
                                    ) {
                                        val targetContentScale = (1f - (1f - (unifiedCompressedRowH / stdRowHeight).coerceIn(0.74f, 1f)) * 0.65f)
                                            .coerceIn(0.84f, 1f)
                                        derivedStateOf {
                                            if (state.fxUnifiedWithKeypad) {
                                                val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                1f - (1f - targetContentScale) * p
                                            } else {
                                                1f
                                            }
                                        }
                                    }
                                    val fxRevealClipShape = remember(
                                        scientificExpandState,
                                        fxTrayHeight,
                                        unifiedTargetShiftPx,
                                        state.fxUnifiedWithKeypad
                                    ) {
                                        object : Shape {
                                            override fun createOutline(
                                                size: Size,
                                                layoutDirection: LayoutDirection,
                                                density: Density
                                            ): Outline {
                                                val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                val clipBottom = (size.height - (1f - p) * (size.height * 0.14f))
                                                    .coerceIn(0f, size.height)
                                                val clipTop = (clipBottom - size.height * p)
                                                    .coerceIn(0f, clipBottom)
                                                return Outline.Rectangle(
                                                    Rect(
                                                        left = 0f,
                                                        top = clipTop,
                                                        right = size.width,
                                                        bottom = clipBottom
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Upper region: pre-composed Fx grid behind lifted display & toggle strip -> 0 Frame-1 hitch, 0 overlap
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        FxScientificPanel(
                                            isInverse = state.isInverseMode,
                                            angleUnit = state.angleUnit,
                                            buttonShapeStyle = state.buttonShapeStyle,
                                            viewModel = viewModel,
                                            enabled = state.isScientificExpanded,
                                            columns = 5,
                                            rowSpacing = ((if (isCompactVertical) 6.dp else 7.5.dp) * (0.85f + 0.15f * fxScale)).coerceIn(5.dp, 10.dp),
                                            textScale = fxTextScale,
                                            glowTarget = state.glowTarget,
                                            glowIntensity = state.glowIntensity,
                                            glowColorOverride = resolvedGlowColor,
                                            colorTintOperators = state.colorTintOperators,
                                            colorTintUtilities = state.colorTintUtilities,
                                            colorTintFx = state.colorTintFx,
                                            keyPressEffect = state.keyPressEffect,
                                            pressSparksEnabled = state.pressSparksEnabled,
                                            pressSparkStyle = state.pressSparkStyle,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp)
                                                .padding(bottom = fxBottomGap)
                                                .height(fxTrayHeight)
                                                .then(
                                                    if (!state.isScientificExpanded) {
                                                        Modifier.clearAndSetSemantics {}
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .graphicsLayer {
                                                    val p = scientificExpandState.value.coerceIn(0f, 1f)
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                    alpha = if (p <= 0.001f) 0f else ((p - 0.06f) / 0.94f).coerceIn(0f, 1f)
                                                    translationY = unifiedTargetShiftPx * p + (1f - p) * (size.height * 0.14f)
                                                    clip = true
                                                    shape = fxRevealClipShape
                                                }
                                                .drawWithContent {
                                                    if (scientificExpandState.value > 0.001f || isSciExpandedState.value) {
                                                        drawContent()
                                                    }
                                                }
                                        )

                                        Column(modifier = Modifier.fillMaxSize()) {
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
                                                bottomLiftState = displayLiftPxState,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 175.dp)
                                                    .onSizeChanged { displayCardHeightPxRef[0] = it.height }
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        translationY = -displayLiftPxState.value
                                                    }
                                            ) {
                                                ScientificToggleStrip(
                                                    isExpanded = state.isScientificExpanded,
                                                    onToggle = viewModel::toggleScientificExpanded,
                                                    onOpenVoiceInput = { viewModel.setVoiceInputDialogOpen(true) },
                                                    glowTarget = state.glowTarget,
                                                    glowIntensity = state.glowIntensity,
                                                    glowColorOverride = resolvedGlowColor
                                                )
                                            }
                                        }
                                    }

                                    // Lower region: Fixed-height bottom container keeps CalculatorDisplay zero-recompose even when unified keypad compresses!
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(start = 18.dp, end = 18.dp, top = topPadDp, bottom = 8.dp)
                                            .height(collapsedKeypadInnerDp)
                                            .onGloballyPositioned { coords ->
                                                if (coords.isAttached) {
                                                    val pos = coords.positionInRoot()
                                                    val left = pos.x - portraitColOriginRef[0]
                                                    val top = pos.y - portraitColOriginRef[1]
                                                    portraitKeypadInnerRectRef[0] = left
                                                    portraitKeypadInnerRectRef[1] = top
                                                    portraitKeypadInnerRectRef[2] = left + coords.size.width.toFloat()
                                                    portraitKeypadInnerRectRef[3] = top + coords.size.height.toFloat()
                                                }
                                            },
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            StandardKeypadGrid(
                                                viewModel = viewModel,
                                                rowHeight = stdRowHeight,
                                                rowSpacing = stdRowSpacing,
                                                collapsedRowHeight = stdRowHeight,
                                                expandedRowHeight = unifiedCompressedRowH,
                                                collapsedRowSpacing = stdRowSpacing,
                                                expandedRowSpacing = unifiedCompressedGap,
                                                expandProgressState = if (state.fxUnifiedWithKeypad) scientificExpandState else null,
                                                contentScaleState = if (state.fxUnifiedWithKeypad) keypadContentScaleState else null,
                                                restingCornerRadius = standardRestingCorner,
                                                pressedCornerRadius = standardPressedCorner,
                                                textScale = keypadTextScale,
                                                glowTarget = state.glowTarget,
                                                glowIntensity = state.glowIntensity,
                                                glowColorOverride = resolvedGlowColor,
                                                colorTintDigits = state.colorTintDigits,
                                                colorTintOperators = state.colorTintOperators,
                                                colorTintUtilities = state.colorTintUtilities,
                                                keyPressEffect = state.keyPressEffect,
                                                pressSparksEnabled = state.pressSparksEnabled,
                                                pressSparkStyle = state.pressSparkStyle,
                                                glowBreathingEnabled = state.glowBreathingEnabled,
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
                    val progress = revealRadiusFraction.value.coerceIn(0f, 1f)
                    val rimFade = (1f - progress).coerceIn(0f, 1f)
                    if (rimFade > 0.01f) {
                        when (state.themeAnimStyle) {
                            ThemeAnimStyle.CIRCLE_REVEAL -> {
                                val origin = revealOriginRoot?.let { it - rootBoxOrigin }
                                    ?: Offset(size.width * 0.16f, size.height * 0.08f)
                                val maxRadius = hypot(
                                    maxOf(origin.x, size.width - origin.x),
                                    maxOf(origin.y, size.height - origin.y)
                                ) * 1.02f
                                val radius = (maxRadius * progress).coerceAtLeast(1f)
                                drawCircle(
                                    color = revealRimColor.copy(alpha = 0.34f * rimFade),
                                    radius = radius,
                                    center = origin,
                                    style = Stroke(width = 1.75.dp.toPx())
                                )
                            }
                            ThemeAnimStyle.DIAGONAL_WIPE -> {
                                val d = (size.width + size.height) * progress
                                val start = if (d <= size.width) Offset(d, 0f) else Offset(size.width, d - size.width)
                                val end = if (d <= size.height) Offset(0f, d) else Offset(d - size.height, size.height)
                                drawLine(
                                    color = revealRimColor.copy(alpha = 0.45f * rimFade),
                                    start = start,
                                    end = end,
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            ThemeAnimStyle.VERTICAL_CURTAIN -> {
                                val y = size.height * progress
                                drawLine(
                                    color = revealRimColor.copy(alpha = 0.45f * rimFade),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            ThemeAnimStyle.CROSSFADE,
                            ThemeAnimStyle.INSTANT -> Unit
                        }
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
                onSaveScrollPosition = viewModel::setSettingsScrollPosition,
                onToggleVibrationExpanded = viewModel::setSettingsVibrationExpanded,
                onToggleSound = viewModel::toggleSoundEnabled,
                onChangeSoundVolume = viewModel::setSoundVolume,
                onPreviewSoundVolume = viewModel::previewSoundVolume,
                onToggleVibration = { viewModel.toggleVibrationEnabled(view) },
                onSelectHapticIntensity = { intensity -> viewModel.setHapticIntensity(intensity, view) },
                onToggleHapticAccentEquals = { enabled -> viewModel.setHapticAccentEquals(enabled, view) },
                onTestHaptic = { type -> viewModel.testHaptic(type, view) },
                onSelectThemeMode = { mode ->
                    if (mode != state.themeMode) {
                        triggerThemeReveal { viewModel.setThemeMode(mode) }
                    }
                },
                onToggleDynamicColor = viewModel::setUseDynamicColor,
                onSelectDynamicColorSource = viewModel::setDynamicColorSource,
                onSelectDynamicColorVibrancy = viewModel::setDynamicColorVibrancy,
                onToggleColorTintOperators = viewModel::setColorTintOperators,
                onToggleColorTintUtilities = viewModel::setColorTintUtilities,
                onToggleColorTintDigits = viewModel::setColorTintDigits,
                onToggleColorTintFx = viewModel::setColorTintFx,
                onSelectCustomAccentArgb = viewModel::setCustomAccentArgb,
                onSelectButtonShape = viewModel::setButtonShapeStyle,
                onChangeKeypadScale = viewModel::setKeypadScale,
                onChangeFxScale = viewModel::setFxScale,
                onSelectGlowTarget = viewModel::setGlowTarget,
                onChangeGlowIntensity = viewModel::setGlowIntensity,
                onSelectResultAnimStyle = viewModel::setResultAnimStyle,
                onTogglePressSparks = viewModel::setPressSparksEnabled,
                onSelectPressSparkStyle = viewModel::setPressSparkStyle,
                onToggleGlowBreathing = viewModel::setGlowBreathingEnabled,
                onToggleLivePreview = viewModel::setShowLivePreview,
                onToggleAmbientDisplayGlow = viewModel::setAmbientDisplayGlow,
                onSelectDisplayGlowStyle = viewModel::setDisplayGlowStyle,
                onChangeDisplayGlowIntensity = viewModel::setDisplayGlowIntensity,
                onSelectDisplayGlowColorPreset = viewModel::setDisplayGlowColorPreset,
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
    onOpenVoiceInput: () -> Unit,
    glowTarget: GlowTarget = GlowTarget.ACCENT_KEYS,
    glowIntensity: Float = 1f,
    glowColorOverride: Color? = null
) {
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    val arrowRotationState = animateFloatAsState(
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
                            .graphicsLayer { rotationZ = arrowRotationState.value }
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
            glowColor = glowColorOverride ?: MaterialTheme.colorScheme.tertiary,
            glowEnabled = glowTarget != GlowTarget.OFF,
            glowIntensity = glowIntensity,
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
    glowEnabled: Boolean = true,
    glowIntensity: Float = 1f,
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

    val clampedIntensity = glowIntensity.coerceIn(0.25f, 1.75f)

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
                .then(
                    if (glowEnabled) {
                        Modifier.drawBehind {
                            val pulse = maxOf(glowPulse.value, if (isPressed) 0.7f else 0f)
                            val baseAlpha = ((if (isDark) 0.28f else 0.22f) * clampedIntensity + pulse * 0.18f * clampedIntensity)
                                .coerceIn(0f, 0.85f)
                            val maxSpreadPx = ((4f * (0.72f + 0.28f * clampedIntensity)) + pulse * 2f * clampedIntensity).dp.toPx()
                            val halfH = size.height / 2f
                            val steps = 3
                            for (i in steps downTo 1) {
                                val t = i.toFloat() / steps.toFloat()
                                val expand = maxSpreadPx * t
                                val layerAlpha = (baseAlpha * (1f - t * 0.68f) * 0.44f).coerceIn(0f, 1f)
                                val corner = halfH + expand
                                drawRoundRect(
                                    color = softGlowColor.copy(alpha = layerAlpha),
                                    topLeft = Offset(-expand, -expand),
                                    size = Size(size.width + expand * 2f, size.height + expand * 2f),
                                    cornerRadius = CornerRadius(corner, corner)
                                )
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(containerColor)
                .indication(interactionSource, ripple(bounded = true))
                .then(
                    if (glowEnabled) {
                        Modifier.drawWithCache {
                            val sheenBrush = Brush.verticalGradient(
                                0.0f to innerTintColor,
                                0.55f to innerTintColor.copy(alpha = 0.28f),
                                1.0f to Color.Transparent
                            )

                            onDrawBehind {
                                val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                                drawRect(
                                    brush = sheenBrush,
                                    alpha = ((0.14f * clampedIntensity) + pulse * 0.12f * clampedIntensity).coerceIn(0f, 0.72f)
                                )
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
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
private fun FxScientificPanel(
    isInverse: Boolean,
    angleUnit: AngleUnit,
    buttonShapeStyle: ButtonShapeStyle,
    viewModel: CalculatorViewModel,
    enabled: Boolean = true,
    columns: Int = 5,
    rowSpacing: Dp = 6.dp,
    textScale: Float = 1f,
    glowTarget: GlowTarget = GlowTarget.ACCENT_KEYS,
    glowIntensity: Float = 1f,
    glowColorOverride: Color? = null,
    colorTintOperators: Boolean = true,
    colorTintUtilities: Boolean = true,
    colorTintFx: Boolean = true,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    pressSparkStyle: PressSparkStyle = PressSparkStyle.SPARKS,
    modifier: Modifier = Modifier
) {
    val restingCorner = when (buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 50.dp
        ButtonShapeStyle.SQUIRCLE -> 14.dp
        ButtonShapeStyle.SHARP -> 8.dp
    }
    val pressedCorner = when (buttonShapeStyle) {
        ButtonShapeStyle.PILL -> 10.dp
        ButtonShapeStyle.SQUIRCLE -> 8.dp
        ButtonShapeStyle.SHARP -> 4.dp
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val modeBg = if (colorTintOperators || colorTintFx) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val modePressedBg = if (colorTintOperators || colorTintFx) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val modeFg = if (colorTintOperators || colorTintFx) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val invActiveBg = if (colorTintUtilities || colorTintFx) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val invActiveFg = if (colorTintUtilities || colorTintFx) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val normalBg = if (colorTintFx) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primaryContainer,
            if (isDarkTheme) 0.30f else 0.36f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val normalFg = MaterialTheme.colorScheme.onSurface
    val pressedBg = if (colorTintFx) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.primaryContainer,
            if (isDarkTheme) 0.45f else 0.52f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val activeFuncBg = if (isInverse) invActiveBg else normalBg
    val activeFuncFg = if (isInverse) invActiveFg else normalFg

    val fxAccentGlow = glowTarget != GlowTarget.OFF
    val fxAllGlow = glowTarget == GlowTarget.ALL_KEYS
    val fxInvGlow = glowTarget == GlowTarget.ALL_KEYS || (glowTarget == GlowTarget.ACCENT_KEYS && isInverse)

    val fxKeyRimColor: Color? = null

    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = (14.5f * textScale).sp,
        lineHeight = (17f * textScale).sp,
        fontWeight = FontWeight.Medium,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val modeStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = (12.5f * textScale).sp,
        lineHeight = (15f * textScale).sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    // Classic symmetric 5x3 (or 3x5 in landscape) M3 scientific grid in a single 1-pass Layout
    Layout(
        modifier = modifier.testTag("fx_scientific_panel"),
        content = {
            // 0..4: DEG/RAD, INV, sin, cos, tan
            ScientificCalcButton(
                label = angleUnit.name,
                isActive = true,
                testTag = "btn_sci_angle_unit",
                enabled = enabled,
                onClick = viewModel::toggleAngleUnit,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = modeStyle,
                containerColor = modeBg,
                pressedContainerColor = modePressedBg,
                contentColorOverride = modeFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAccentGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = "INV",
                isActive = isInverse,
                testTag = "btn_sci_inv",
                enabled = enabled,
                onClick = viewModel::toggleInverseMode,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = modeStyle,
                containerColor = if (isInverse) invActiveBg else normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = if (isInverse) invActiveFg else normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "sin⁻¹" else "sin",
                isActive = isInverse,
                testTag = "btn_sci_sin",
                enabled = enabled,
                onClick = { viewModel.onFunction(if (isInverse) "sin⁻¹(" else "sin(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "cos⁻¹" else "cos",
                isActive = isInverse,
                testTag = "btn_sci_cos",
                enabled = enabled,
                onClick = { viewModel.onFunction(if (isInverse) "cos⁻¹(" else "cos(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "tan⁻¹" else "tan",
                isActive = isInverse,
                testTag = "btn_sci_tan",
                enabled = enabled,
                onClick = { viewModel.onFunction(if (isInverse) "tan⁻¹(" else "tan(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )

            // 5..9: π, e, xʸ, ln, log
            ScientificCalcButton(
                label = "π",
                isActive = false,
                testTag = "btn_sci_pi",
                enabled = enabled,
                onClick = { viewModel.onConstant("π") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = "e",
                isActive = false,
                testTag = "btn_sci_e",
                enabled = enabled,
                onClick = { viewModel.onConstant("e") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = "xʸ",
                isActive = false,
                testTag = "btn_sci_pow",
                enabled = enabled,
                onClick = { viewModel.onOperator("^") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "eˣ" else "ln",
                isActive = isInverse,
                testTag = "btn_sci_ln",
                enabled = enabled,
                onClick = { if (isInverse) viewModel.onExpE() else viewModel.onFunction("ln(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "10ˣ" else "log",
                isActive = isInverse,
                testTag = "btn_sci_log",
                enabled = enabled,
                onClick = { if (isInverse) viewModel.onExp10() else viewModel.onFunction("log(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )

            // 10..14: (, ), √, x!, ±
            ScientificCalcButton(
                label = "(",
                isActive = false,
                testTag = "btn_sci_lparen",
                enabled = enabled,
                onClick = viewModel::onOpenParenthesis,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = ")",
                isActive = false,
                testTag = "btn_sci_rparen",
                enabled = enabled,
                onClick = viewModel::onCloseParenthesis,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = if (isInverse) "x²" else "√",
                isActive = isInverse,
                testTag = "btn_sci_sqrt",
                enabled = enabled,
                onClick = { if (isInverse) viewModel.onSquare() else viewModel.onFunction("√(") },
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = activeFuncBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = activeFuncFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxInvGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = "x!",
                isActive = false,
                testTag = "btn_sci_fact",
                enabled = enabled,
                onClick = viewModel::onFactorial,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
            ScientificCalcButton(
                label = "±",
                isActive = false,
                testTag = "btn_sci_sign",
                enabled = enabled,
                onClick = viewModel::onToggleSign,
                restingCornerRadius = restingCorner,
                pressedCornerRadius = pressedCorner,
                textStyle = labelStyle,
                containerColor = normalBg,
                pressedContainerColor = pressedBg,
                contentColorOverride = normalFg,
                borderColor = fxKeyRimColor,
                hasGlow = fxAllGlow,
                glowIntensity = glowIntensity,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle
            )
        }
    ) { measurables, constraints ->
        val cols = columns.coerceAtLeast(1)
        val rows = ((measurables.size + cols - 1) / cols).coerceAtLeast(1)
        val maxW = constraints.maxWidth.coerceAtLeast(cols)
        val maxH = constraints.maxHeight.coerceAtLeast(rows)
        val gapPx = rowSpacing.roundToPx().coerceAtLeast(0)

        val totalGapW = (cols - 1) * gapPx
        val baseCellW = ((maxW - totalGapW) / cols).coerceAtLeast(1)
        val extraW = (maxW - totalGapW - baseCellW * cols).coerceAtLeast(0)

        val totalGapH = (rows - 1) * gapPx
        val baseCellH = ((maxH - totalGapH) / rows).coerceAtLeast(1)
        val extraH = (maxH - totalGapH - baseCellH * rows).coerceAtLeast(0)

        val placeables = measurables.mapIndexed { idx, measurable ->
            val col = idx % cols
            val row = idx / cols
            val w = baseCellW + if (col < extraW) 1 else 0
            val h = baseCellH + if (row < extraH) 1 else 0
            measurable.measure(Constraints.fixed(w, h))
        }

        layout(maxW, maxH) {
            var y = 0
            for (row in 0 until rows) {
                var x = 0
                val rowH = baseCellH + if (row < extraH) 1 else 0
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    if (idx < placeables.size) {
                        val p = placeables[idx]
                        p.placeRelative(x, y)
                        x += p.width + gapPx
                    }
                }
                y += rowH + gapPx
            }
        }
    }
}

@Composable
private fun StandardKeypadGrid(
    viewModel: CalculatorViewModel,
    rowHeight: Dp?,
    rowSpacing: Dp = 11.dp,
    collapsedRowHeight: Dp = 62.dp,
    expandedRowHeight: Dp = 48.dp,
    collapsedRowSpacing: Dp = 10.dp,
    expandedRowSpacing: Dp = 8.dp,
    expandProgressState: State<Float>? = null,
    contentScaleState: State<Float>? = null,
    restingCornerRadius: Dp = 50.dp,
    pressedCornerRadius: Dp = 14.dp,
    textScale: Float = 1f,
    glowTarget: GlowTarget = GlowTarget.ACCENT_KEYS,
    glowIntensity: Float = 1f,
    glowColorOverride: Color? = null,
    colorTintDigits: Boolean = true,
    colorTintOperators: Boolean = true,
    colorTintUtilities: Boolean = true,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    pressSparkStyle: PressSparkStyle = PressSparkStyle.SPARKS,
    glowBreathingEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentGlow = glowTarget != GlowTarget.OFF
    val keyGlow = glowTarget == GlowTarget.ALL_KEYS

    val operatorGlyphStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = (32f * textScale).sp,
        lineHeight = (32f * textScale).sp,
        fontWeight = FontWeight.Medium,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val parenStyle = MaterialTheme.typography.headlineMedium.copy(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = (24f * textScale).sp,
        fontWeight = FontWeight.Normal
    )

    // Flat 4x5 grid Layout: eliminates 5 Row nodes & 20 weight() double-measure passes per frame
    Layout(
        modifier = modifier,
        content = {
            // Row 1: AC, (), %, ÷
            ExpressiveCalcButton(
                label = "AC",
                role = CalcButtonRole.UTILITY,
                testTag = "btn_ac",
                hasGlow = accentGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintUtilities,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                onClick = viewModel::onAllClear
            )
            ExpressiveCalcButton(
                label = "( )",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_paren",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                onClick = viewModel::onSmartParenthesis
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                onClick = viewModel::onPercent
            )
            ExpressiveCalcButton(
                label = "÷",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_div",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("÷") }
            )

            // Row 2: 7, 8, 9, ×
            DigitButton("7", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("8", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("9", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            ExpressiveCalcButton(
                label = "×",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_mul",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("×") }
            )

            // Row 3: 4, 5, 6, −
            DigitButton("4", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("5", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("6", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            ExpressiveCalcButton(
                label = "−",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_sub",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("−") }
            )

            // Row 4: 1, 2, 3, +
            DigitButton("1", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("2", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            DigitButton("3", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            ExpressiveCalcButton(
                label = "+",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_add",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintOperators,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                textStyle = operatorGlyphStyle,
                onClick = { viewModel.onOperator("+") }
            )

            // Row 5: 0, ., ⌫, =
            DigitButton("0", viewModel, restingCornerRadius, pressedCornerRadius, contentScaleState, textScale, keyGlow, glowIntensity, glowColorOverride, colorTintDigits, keyPressEffect, pressSparksEnabled, pressSparkStyle, glowBreathingEnabled)
            ExpressiveCalcButton(
                label = ".",
                role = CalcButtonRole.DIGIT,
                testTag = "btn_dot",
                hasGlow = keyGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintDigits,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                onClick = viewModel::onDecimalPoint
            )
            ExpressiveCalcButton(
                label = "⌫",
                role = CalcButtonRole.UTILITY,
                testTag = "btn_backspace",
                hasGlow = accentGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                colorTintEnabled = colorTintUtilities,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                contentDescriptionText = stringResource(R.string.cd_backspace),
                onClick = viewModel::onBackspace,
                onLongClick = viewModel::onAllClear
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
                hasGlow = accentGlow,
                glowIntensity = glowIntensity,
                glowColorOverride = glowColorOverride,
                keyPressEffect = keyPressEffect,
                pressSparksEnabled = pressSparksEnabled,
                pressSparkStyle = pressSparkStyle,
                glowBreathingEnabled = glowBreathingEnabled,
                restingCornerRadius = restingCornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                textScale = textScale,
                contentScaleState = contentScaleState,
                textStyle = operatorGlyphStyle,
                onClick = viewModel::onEquals
            )
        }
    ) { measurables, constraints ->
        val cols = 4
        val rows = (measurables.size + cols - 1) / cols

        val gapPx: Int
        val rowHPx: Int
        if (expandProgressState != null) {
            val p = expandProgressState.value
            val collapsedHPx = collapsedRowHeight.toPx()
            val expandedHPx = expandedRowHeight.toPx()
            rowHPx = (collapsedHPx + (expandedHPx - collapsedHPx) * p).roundToInt().coerceAtLeast(1)
            val collapsedGapPx = collapsedRowSpacing.toPx()
            val expandedGapPx = expandedRowSpacing.toPx()
            gapPx = (collapsedGapPx + (expandedGapPx - collapsedGapPx) * p).roundToInt().coerceAtLeast(0)
        } else if (rowHeight == null) {
            gapPx = rowSpacing.roundToPx().coerceAtLeast(0)
            val totalGaps = (rows - 1) * gapPx
            rowHPx = ((constraints.maxHeight - totalGaps) / rows.coerceAtLeast(1)).coerceAtLeast(1)
        } else {
            gapPx = rowSpacing.roundToPx().coerceAtLeast(0)
            rowHPx = rowHeight.roundToPx().coerceAtLeast(1)
        }

        val totalGapW = (cols - 1) * gapPx
        val baseCellW = ((constraints.maxWidth - totalGapW) / cols).coerceAtLeast(1)
        val extraW = (constraints.maxWidth - totalGapW - baseCellW * cols).coerceAtLeast(0)

        val placeables = measurables.mapIndexed { index, measurable ->
            val col = index % cols
            val cellW = baseCellW + if (col < extraW) 1 else 0
            measurable.measure(Constraints.fixed(cellW, rowHPx))
        }

        val totalHeight = if (rows == 0) 0 else rows * rowHPx + (rows - 1) * gapPx
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            for (row in 0 until rows) {
                var x = 0
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    if (idx < placeables.size) {
                        val p = placeables[idx]
                        p.placeRelative(x, y)
                        x += p.width + gapPx
                    }
                }
                y += rowHPx + gapPx
            }
        }
    }
}

@Composable
private fun DigitButton(
    digit: String,
    viewModel: CalculatorViewModel,
    restingCornerRadius: Dp = 50.dp,
    pressedCornerRadius: Dp = 14.dp,
    contentScaleState: State<Float>? = null,
    textScale: Float = 1f,
    hasGlow: Boolean = false,
    glowIntensity: Float = 1f,
    glowColorOverride: Color? = null,
    colorTintEnabled: Boolean = true,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    pressSparkStyle: PressSparkStyle = PressSparkStyle.SPARKS,
    glowBreathingEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    ExpressiveCalcButton(
        label = digit,
        role = CalcButtonRole.DIGIT,
        testTag = "btn_$digit",
        hasGlow = hasGlow,
        glowIntensity = glowIntensity,
        glowColorOverride = glowColorOverride,
        colorTintEnabled = colorTintEnabled,
        keyPressEffect = keyPressEffect,
        pressSparksEnabled = pressSparksEnabled,
        pressSparkStyle = pressSparkStyle,
        glowBreathingEnabled = glowBreathingEnabled,
        restingCornerRadius = restingCornerRadius,
        pressedCornerRadius = pressedCornerRadius,
        textScale = textScale,
        contentScaleState = contentScaleState,
        onClick = { viewModel.onDigit(digit) },
        modifier = modifier
    )
}

