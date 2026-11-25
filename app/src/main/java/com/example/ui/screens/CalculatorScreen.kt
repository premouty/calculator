package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AngleUnit
import com.example.ui.CalculatorUiState
import com.example.ui.CalculatorViewModel
import com.example.ui.components.BigNumberViewerDialog
import com.example.ui.components.CalcButtonRole
import com.example.ui.components.CalculatorDisplay
import com.example.ui.components.CameraMathScannerOverlay
import com.example.ui.components.ExpressiveCalcButton
import com.example.ui.components.FullHistoryBottomSheet
import com.example.ui.components.LocalPerformCalcHaptic
import com.example.ui.components.LocalPlayCalcSound
import com.example.ui.components.TopPullDownHistoryOverlay
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.launch
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

    BackHandler(enabled = state.isTopHistoryOpen) {
        viewModel.setTopHistoryOpen(false)
    }

    CompositionLocalProvider(
        LocalPlayCalcSound provides viewModel::playSound,
        LocalPerformCalcHaptic provides viewModel::performHaptic
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
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
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
                    val expandedRowHeight = if (isCompactVertical) 48.dp else 54.dp
                    val targetStandardRowHeight = lerp(
                        collapsedRowHeight,
                        expandedRowHeight,
                        scientificExpandProgress
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 980.dp)
                    ) {
                        if (isWideOrLandscape) {
                            CalculatorDisplay(
                                state = state,
                                onToggleTheme = viewModel::cycleThemeMode,
                                onToggleSound = viewModel::toggleSoundEnabled,
                                onToggleVibration = viewModel::toggleVibrationEnabled,
                                onToggleAngleUnit = viewModel::toggleAngleUnit,
                                onToggleTopHistory = viewModel::toggleTopHistory,
                                onSetTopHistoryOpen = viewModel::setTopHistoryOpen,
                                onOpenFullHistory = { viewModel.setFullHistorySheetOpen(true) },
                                onInspectBigNumber = viewModel::openBigNumberViewer,
                                compactMode = true,
                                modifier = Modifier.onSizeChanged { displayCardHeightPx = it.height }
                            )

                            ScientificToggleStrip(
                                isExpanded = state.isScientificExpanded,
                                onToggle = viewModel::toggleScientificExpanded,
                                onOpenCameraScanner = { viewModel.setCameraScannerOpen(true) }
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
                                        ScientificKeypadGrid(
                                            isInverse = state.isInverseMode,
                                            angleUnit = state.angleUnit,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                StandardKeypadGrid(
                                    viewModel = viewModel,
                                    rowHeight = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        } else {
                            CalculatorDisplay(
                                state = state,
                                onToggleTheme = viewModel::cycleThemeMode,
                                onToggleSound = viewModel::toggleSoundEnabled,
                                onToggleVibration = viewModel::toggleVibrationEnabled,
                                onToggleAngleUnit = viewModel::toggleAngleUnit,
                                onToggleTopHistory = viewModel::toggleTopHistory,
                                onSetTopHistoryOpen = viewModel::setTopHistoryOpen,
                                onOpenFullHistory = { viewModel.setFullHistorySheetOpen(true) },
                                onInspectBigNumber = viewModel::openBigNumberViewer,
                                compactMode = isCompactVertical,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 190.dp)
                                    .onSizeChanged { displayCardHeightPx = it.height }
                            )

                            ScientificToggleStrip(
                                isExpanded = state.isScientificExpanded,
                                onToggle = viewModel::toggleScientificExpanded,
                                onOpenCameraScanner = { viewModel.setCameraScannerOpen(true) }
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
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
                                        ScientificKeypadGrid(
                                            isInverse = state.isInverseMode,
                                            angleUnit = state.angleUnit,
                                            viewModel = viewModel,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 10.dp)
                                                .height(152.dp)
                                        )
                                    }
                                }

                                StandardKeypadGrid(
                                    viewModel = viewModel,
                                    rowHeight = targetStandardRowHeight,
                                    modifier = Modifier.fillMaxWidth()
                                )
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

                CameraMathScannerOverlay(
                    visible = state.isCameraScannerOpen,
                    angleUnit = state.angleUnit,
                    analysisState = state.cameraScanAnalysisState,
                    onAnalyzeWithAi = viewModel::analyzeCameraPhotoWithAi,
                    onSetOfflineResult = viewModel::setCameraScanOfflineResult,
                    onResetAnalysis = viewModel::resetCameraScanAnalysis,
                    onTransferExpression = viewModel::applyScannedExpression,
                    onDismiss = { viewModel.setCameraScannerOpen(false) }
                )
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

        state.bigNumberViewerRecord?.let { (exprLabel, plainDigits) ->
            BigNumberViewerDialog(
                expressionLabel = exprLabel,
                plainDigits = plainDigits,
                onCopy = copyToClipboard,
                onDismiss = viewModel::closeBigNumberViewer
            )
        }
    }
}

@Composable
private fun ScientificToggleStrip(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenCameraScanner: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scientific_chevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onToggle)
                .testTag("toggle_scientific_panel")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "fx",
                    style = MaterialTheme.typography.labelLarge,
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
                        .size(16.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onOpenCameraScanner)
                .testTag("open_camera_scanner_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DocumentScanner,
                    contentDescription = stringResource(R.string.cd_camera_scan),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.camera_scan_chip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
    modifier: Modifier = Modifier
) {
    val spacing = 7.dp
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "INV",
                role = if (isInverse) CalcButtonRole.SCIENTIFIC_ACTIVE else CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_inv",
                onClick = viewModel::toggleInverseMode,
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "π",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_pi",
                onClick = { viewModel.onConstant("π") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "e",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_e",
                onClick = { viewModel.onConstant("e") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "±",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_sign",
                onClick = viewModel::onToggleSign,
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "cos⁻¹" else "cos",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_cos",
                onClick = { viewModel.onFunction(if (isInverse) "cos⁻¹(" else "cos(") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "tan⁻¹" else "tan",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_tan",
                onClick = { viewModel.onFunction(if (isInverse) "tan⁻¹(" else "tan(") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "eˣ" else "ln",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_ln",
                onClick = { if (isInverse) viewModel.onExpE() else viewModel.onFunction("ln(") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = if (isInverse) "10ˣ" else "log",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_log",
                onClick = { if (isInverse) viewModel.onExp10() else viewModel.onFunction("log(") },
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "(",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_lparen",
                onClick = viewModel::onOpenParenthesis,
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "x!",
                role = CalcButtonRole.SCIENTIFIC,
                testTag = "btn_sci_fact",
                onClick = viewModel::onFactorial,
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 14.dp,
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
    modifier: Modifier = Modifier
) {
    val spacing = 11.dp
    val operatorGlyphStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 32.sp,
        lineHeight = 32.sp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
                onClick = viewModel::onAllClear,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "( )",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_paren",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
                onClick = viewModel::onSmartParenthesis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val parenStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal
                )
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
                onClick = viewModel::onPercent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ExpressiveCalcButton(
                label = "÷",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_div",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
            DigitButton("7", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("8", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("9", viewModel, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "×",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_mul",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
            DigitButton("4", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("5", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("6", viewModel, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "−",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_sub",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
            DigitButton("1", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("2", viewModel, Modifier.weight(1f).fillMaxHeight())
            DigitButton("3", viewModel, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = "+",
                role = CalcButtonRole.OPERATOR,
                testTag = "btn_add",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
            DigitButton("0", viewModel, Modifier.weight(1f).fillMaxHeight())
            ExpressiveCalcButton(
                label = ".",
                role = CalcButtonRole.DIGIT,
                testTag = "btn_dot",
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
                    modifier = Modifier.size(21.dp)
                )
            }
            ExpressiveCalcButton(
                label = "=",
                role = CalcButtonRole.EQUALS,
                testTag = "btn_equals",
                hasGlow = true,
                restingCornerRadius = 50.dp,
                pressedCornerRadius = 16.dp,
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
    modifier: Modifier = Modifier
) {
    ExpressiveCalcButton(
        label = digit,
        role = CalcButtonRole.DIGIT,
        testTag = "btn_$digit",
        restingCornerRadius = 50.dp,
        pressedCornerRadius = 16.dp,
        onClick = { viewModel.onDigit(digit) },
        modifier = modifier
    )
}
