package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CalculationRecord
import com.example.domain.math.MathErrorType
import com.example.ui.CalculatorUiState
import com.example.ui.theme.ThemeMode
import kotlin.math.roundToInt

@Composable
fun CalculatorDisplay(
    state: CalculatorUiState,
    onToggleTheme: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleAngleUnit: () -> Unit,
    onToggleTopHistory: () -> Unit,
    onSetTopHistoryOpen: (Boolean) -> Unit,
    onOpenFullHistory: () -> Unit,
    onInspectBigNumber: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val scrollState = rememberScrollState()
    val shakeOffset = remember { Animatable(0f) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val mainDisplayText = state.formattedExpression.ifEmpty { "0" }

    LaunchedEffect(mainDisplayText) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    LaunchedEffect(state.errorTriggerId) {
        if (state.errorTriggerId > 0L) {
            val sequence = listOf(-18f, 18f, -12f, 12f, -6f, 0f)
            for (target in sequence) {
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 40))
            }
        }
    }

    val cardShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.isTopHistoryOpen) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator > 50f && !state.isTopHistoryOpen) {
                            onSetTopHistoryOpen(true)
                        } else if (dragAccumulator < -50f && state.isTopHistoryOpen) {
                            onSetTopHistoryOpen(false)
                        }
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Symmetrical Top Utility Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeModeChip(
                        themeMode = state.themeMode,
                        onClick = onToggleTheme
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleAngleUnit)
                            .testTag("display_angle_chip")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = state.angleUnit.name,
                                transitionSpec = {
                                    (fadeIn() + slideInVertically { it / 2 }) togetherWith
                                        (fadeOut() + slideOutVertically { -it / 2 })
                                },
                                label = "angle_unit_anim"
                            ) { unitText ->
                                Text(
                                    text = unitText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Right utility pills: Vibration toggle + Sound toggle + History pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VibrationTogglePill(
                        enabled = state.isVibrationEnabled,
                        onClick = onToggleVibration
                    )

                    SoundTogglePill(
                        enabled = state.isSoundEnabled,
                        onClick = onToggleSound
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenFullHistory)
                            .testTag("open_history_modal_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = stringResource(R.string.cd_history_button),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            if (state.history.isNotEmpty()) {
                                Text(
                                    text = if (state.history.size > 99) "99+" else state.history.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Bottom portion of display: Completed expression, Active display, Preview, Pull handle
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = state.isResultDisplayed && state.lastCompletedExpression.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 }
                ) {
                    Text(
                        text = "${state.lastCompletedExpression} =",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                val dynamicFontSize = remember(mainDisplayText, compactMode) {
                    val base = if (compactMode) 44 else 58
                    when {
                        mainDisplayText.length <= 8 -> base.sp
                        mainDisplayText.length <= 12 -> (base - 10).sp
                        mainDisplayText.length <= 17 -> (base - 18).sp
                        else -> (base - 24).coerceAtLeast(26).sp
                    }
                }

                val displayTextColor by animateColorAsState(
                    targetValue = when {
                        state.rawExpression.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        state.isResultDisplayed -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(durationMillis = 120),
                    label = "display_text_color"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (compactMode) 2.dp else 8.dp)
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = state.isResultDisplayed,
                            transitionSpec = {
                                if (targetState && !initialState) {
                                    (slideInVertically { it / 2 } + fadeIn(tween(130))) togetherWith
                                        (slideOutVertically { -it / 3 } + fadeOut(tween(90)))
                                } else {
                                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                }
                            },
                            label = "main_display_transition"
                        ) { isRes ->
                            Text(
                                text = mainDisplayText,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = dynamicFontSize,
                                    lineHeight = dynamicFontSize * 1.15f,
                                    fontWeight = if (isRes) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = displayTextColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("calculator_main_display")
                            )
                        }
                    }
                }

                // Big number inspection pill (for results > 15 digits)
                val resultMeta = state.activeResultMeta
                AnimatedVisibility(
                    visible = state.isResultDisplayed && resultMeta != null && resultMeta.digitCount > 15,
                    enter = fadeIn() + expandVertically() + scaleIn(initialScale = 0.92f),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (resultMeta != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onInspectBigNumber(
                                            state.lastCompletedExpression.ifEmpty { resultMeta.displayText },
                                            resultMeta.plainText
                                        )
                                    }
                                    .testTag("big_number_inspect_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInFull,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${stringResource(R.string.big_number_digits_count, resultMeta.digitCount)} • ${stringResource(R.string.tap_for_full_number)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                val errorText = when (state.errorType) {
                    MathErrorType.DIVISION_BY_ZERO -> stringResource(R.string.error_division_by_zero)
                    MathErrorType.DOMAIN_ERROR -> stringResource(R.string.error_domain)
                    MathErrorType.SYNTAX_ERROR -> stringResource(R.string.error_syntax)
                    MathErrorType.OVERFLOW -> stringResource(R.string.error_overflow)
                    null -> null
                }

                val isFortyTwoEasterEgg = state.isResultDisplayed &&
                    state.activeResultMeta?.plainText == "42" &&
                    errorText == null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    when {
                        errorText != null -> {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("calculator_error_text")
                            )
                        }
                        isFortyTwoEasterEgg -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.testTag("easter_egg_liza_text")
                            ) {
                                Text(
                                    text = stringResource(R.string.easter_egg_liza),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                        !state.isResultDisplayed && state.livePreview.isNotEmpty() -> {
                            Text(
                                text = "= ${state.livePreview}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("calculator_live_preview")
                            )
                        }
                    }
                }

                val pullHandleAlpha by animateFloatAsState(
                    targetValue = if (state.isTopHistoryOpen) 0f else 1f,
                    animationSpec = tween(durationMillis = 140),
                    label = "pull_handle_alpha"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = pullHandleAlpha
                            scaleY = 0.7f + 0.3f * pullHandleAlpha
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            enabled = !state.isTopHistoryOpen,
                            onClick = onToggleTopHistory
                        )
                        .padding(top = 6.dp, bottom = 4.dp)
                        .testTag("top_history_pull_handle"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_pull_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TopPullDownHistoryOverlay(
    visible: Boolean,
    history: List<CalculationRecord>,
    maxHeight: Dp = 240.dp,
    onDismiss: () -> Unit,
    onOpenFullHistory: () -> Unit,
    onUseExpression: (CalculationRecord) -> Unit,
    onUseResult: (CalculationRecord) -> Unit,
    onCopyResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragUpAccumulator by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(140)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragUpAccumulator = 0f },
                            onDragEnd = {
                                if (dragUpAccumulator < -45f) {
                                    onDismiss()
                                }
                                dragUpAccumulator = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragUpAccumulator += dragAmount
                            }
                        )
                    }
                    .testTag("top_quick_history_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_quick_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onOpenFullHistory,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("quick_drawer_view_all_button")
                        ) {
                            Text(
                                text = stringResource(R.string.history_view_all),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.history_empty_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            items(history.take(5), key = { it.id }) { record ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onCopyResult(record.plainResult) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_copy_result),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = record.expression,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onUseExpression(record) }
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                            Text(
                                                text = "= ${record.formattedResult}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onUseResult(record) }
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VibrationTogglePill(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "vibration_press_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 220),
        label = "vibration_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "vibration_tint"
    )

    val wiggleAngle = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (enabled) {
            pulseScale.animateTo(1.22f, tween(90))
            for (angle in listOf(-16f, 16f, -10f, 10f, 0f)) {
                wiggleAngle.animateTo(angle, tween(42))
            }
            pulseScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        } else {
            pulseScale.animateTo(0.80f, tween(100))
            pulseScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("vibration_mode_toggle")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Vibration,
                contentDescription = stringResource(R.string.cd_vibration_toggle),
                tint = iconTint,
                modifier = Modifier
                    .size(17.dp)
                    .graphicsLayer {
                        rotationZ = wiggleAngle.value
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }
            )
        }
    }
}

@Composable
private fun SoundTogglePill(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sound_press_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 220),
        label = "sound_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "sound_tint"
    )

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("sound_mode_toggle")
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = enabled,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.5f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.5f))
                },
                label = "sound_icon_anim"
            ) { isOn ->
                Icon(
                    imageVector = if (isOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = stringResource(R.string.cd_sound_toggle),
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
    themeMode: ThemeMode,
    onClick: () -> Unit
) {
    val label = when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "theme_chip_scale"
    )
    val chipBgColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 300),
        label = "theme_chip_bg"
    )

    Surface(
        shape = CircleShape,
        color = chipBgColor,
        modifier = Modifier
            .height(36.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("theme_mode_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedContent(
                targetState = themeMode,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.55f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )) togetherWith (fadeOut(tween(140)) + scaleOut(targetScale = 0.55f))
                },
                label = "theme_icon"
            ) { mode ->
                val icon = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.cd_theme_button),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(130)) + slideOutVertically { -it / 3 })
                },
                label = "theme_label"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
