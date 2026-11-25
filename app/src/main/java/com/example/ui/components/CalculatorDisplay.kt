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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.CalcSoundType
import com.example.data.local.CalculationRecord
import com.example.domain.math.MathErrorType
import com.example.ui.CalculatorUiState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val LIZA_DATE_REGEX = Regex("(?<![\\d.])28\\.09(?![\\d])")

private enum class ParticleShape {
    HEART,
    SPARKLE,
    DOT
}

private data class LizaParticle(
    val angleRad: Float,
    val distanceDp: Float,
    val sizeDp: Float,
    val liftDp: Float,
    val rotationDeg: Float,
    val delayFraction: Float,
    val color: Color,
    val shape: ParticleShape
)

private fun buildLizaParticles(): List<LizaParticle> {
    val palette = listOf(
        Color(0xFFFF4D8D),
        Color(0xFFFF77A9),
        Color(0xFFFFA6C9),
        Color(0xFFF43F5E),
        Color(0xFFFFD166),
        Color(0xFFFF66B2)
    )
    val rng = Random(2809)
    return List(36) { index ->
        val baseAngle = if (index < 24) {
            // Upward & outward fan
            (-168f + (156f * index / 23f)) * (PI.toFloat() / 180f)
        } else {
            // 360 degree inner shimmer ring
            ((index - 24) * 30f + rng.nextFloat() * 18f) * (PI.toFloat() / 180f)
        }
        LizaParticle(
            angleRad = baseAngle + (rng.nextFloat() - 0.5f) * 0.24f,
            distanceDp = if (index < 24) 76f + rng.nextFloat() * 140f else 36f + rng.nextFloat() * 58f,
            sizeDp = 4.5f + rng.nextFloat() * 6.5f,
            liftDp = 18f + rng.nextFloat() * 42f,
            rotationDeg = (rng.nextFloat() - 0.5f) * 180f,
            delayFraction = rng.nextFloat() * 0.16f,
            color = palette[index % palette.size],
            shape = when {
                index % 3 == 0 -> ParticleShape.HEART
                index % 3 == 1 -> ParticleShape.SPARKLE
                else -> ParticleShape.DOT
            }
        )
    }
}

@Composable
fun CalculatorDisplay(
    state: CalculatorUiState,
    onToggleTheme: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
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
    val playSound = LocalPlayCalcSound.current
    val performHaptic = LocalPerformCalcHaptic.current

    val mainDisplayText = state.formattedExpression.ifEmpty { "0" }
    val errorText = when (state.errorType) {
        MathErrorType.DIVISION_BY_ZERO -> stringResource(R.string.error_division_by_zero)
        MathErrorType.DOMAIN_ERROR -> stringResource(R.string.error_domain)
        MathErrorType.SYNTAX_ERROR -> stringResource(R.string.error_syntax)
        MathErrorType.OVERFLOW -> stringResource(R.string.error_overflow)
        null -> null
    }

    val isLizaEasterEgg = errorText == null &&
        !state.isResultDisplayed &&
        LIZA_DATE_REGEX.containsMatchIn(state.rawExpression)

    val lizaParticles = remember { buildLizaParticles() }
    val particleProgress = remember { Animatable(0f) }
    var lizaReplayTrigger by remember { mutableIntStateOf(0) }
    var isLizaCooldown by remember { mutableStateOf(false) }
    var displayRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var burstOriginOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(isLizaEasterEgg, lizaReplayTrigger) {
        if (isLizaEasterEgg) {
            isLizaCooldown = true
            launch {
                delay(3000L)
                isLizaCooldown = false
            }
            particleProgress.snapTo(0f)
            particleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1450,
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
        } else {
            isLizaCooldown = false
            particleProgress.snapTo(0f)
        }
    }

    LaunchedEffect(mainDisplayText, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { displayRootCoords = it },
            propagateMinConstraints = true
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
                    ThemeModeChip(
                        themeMode = state.themeMode,
                        onClick = {
                            playSound(CalcSoundType.OPERATOR)
                            performHaptic(CalcSoundType.OPERATOR)
                            onToggleTheme()
                        }
                    )

                    // Right utility pills: Unified Sound & Vibration pill + History pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FeedbackTogglePill(
                            soundEnabled = state.isSoundEnabled,
                            vibrationEnabled = state.isVibrationEnabled,
                            onToggleSound = {
                                performHaptic(CalcSoundType.UTILITY)
                                onToggleSound()
                            },
                            onToggleVibration = {
                                playSound(CalcSoundType.UTILITY)
                                onToggleVibration()
                            },
                            onQuickToggleAll = {
                                val turnOnBoth = !state.isSoundEnabled && !state.isVibrationEnabled
                                if (turnOnBoth || state.isSoundEnabled) onToggleSound()
                                if (turnOnBoth || state.isVibrationEnabled) onToggleVibration()
                            }
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    playSound(CalcSoundType.OPERATOR)
                                    performHaptic(CalcSoundType.OPERATOR)
                                    onOpenFullHistory()
                                }
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
                    SelectionContainer {
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

                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val baseDisplayStyle = MaterialTheme.typography.displayLarge
                val activeFontWeight = if (state.isResultDisplayed) FontWeight.Medium else FontWeight.Normal

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (compactMode) 2.dp else 8.dp)
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val safeAvailableWidthPx = with(density) {
                        (maxWidth - 6.dp).toPx().coerceAtLeast(1f)
                    }
                    val maxFontSp = if (compactMode) 44f else 58f
                    val minFontSp = if (compactMode) 20f else 24f

                    val dynamicFontSize = remember(
                        mainDisplayText,
                        safeAvailableWidthPx,
                        maxFontSp,
                        minFontSp,
                        activeFontWeight,
                        baseDisplayStyle
                    ) {
                        var candidateSp = maxFontSp
                        val maxWidthMeasured = textMeasurer.measure(
                            text = mainDisplayText,
                            style = baseDisplayStyle.copy(
                                fontSize = candidateSp.sp,
                                lineHeight = (candidateSp * 1.15f).sp,
                                letterSpacing = 0.25.sp,
                                fontWeight = activeFontWeight
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width.toFloat()

                        if (maxWidthMeasured > safeAvailableWidthPx) {
                            val ratio = (safeAvailableWidthPx / maxWidthMeasured).coerceIn(0.2f, 1f)
                            candidateSp = (candidateSp * ratio).coerceIn(minFontSp, maxFontSp)
                            // Refine slightly so glyphs never clip past the edge when >= minFontSp
                            var stepDown = 0
                            while (candidateSp > minFontSp && stepDown < 4) {
                                val checkWidth = textMeasurer.measure(
                                    text = mainDisplayText,
                                    style = baseDisplayStyle.copy(
                                        fontSize = candidateSp.sp,
                                        lineHeight = (candidateSp * 1.15f).sp,
                                        letterSpacing = 0.25.sp,
                                        fontWeight = activeFontWeight
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                ).size.width.toFloat()
                                if (checkWidth <= safeAvailableWidthPx) break
                                candidateSp = (candidateSp - 1f).coerceAtLeast(minFontSp)
                                stepDown++
                            }
                        }
                        candidateSp.sp
                    }

                    val hasHorizontalOverflow = scrollState.maxValue > 0
                    val fadeLeft = scrollState.value > 0
                    val fadeRight = scrollState.value < scrollState.maxValue

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasHorizontalOverflow && (fadeLeft || fadeRight)) {
                                    Modifier
                                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                        .drawWithContent {
                                            drawContent()
                                            val fadePx = 22.dp.toPx().coerceAtMost(size.width * 0.16f)
                                            if (fadeLeft && fadePx > 0f) {
                                                drawRect(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black),
                                                        startX = 0f,
                                                        endX = fadePx
                                                    ),
                                                    size = Size(fadePx, size.height),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                            if (fadeRight && fadePx > 0f) {
                                                drawRect(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(Color.Black, Color.Transparent),
                                                        startX = size.width - fadePx,
                                                        endX = size.width
                                                    ),
                                                    topLeft = Offset(size.width - fadePx, 0f),
                                                    size = Size(fadePx, size.height),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                        }
                                } else {
                                    Modifier
                                }
                            )
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionContainer {
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
                                    style = baseDisplayStyle.copy(
                                        fontSize = dynamicFontSize,
                                        lineHeight = dynamicFontSize * 1.15f,
                                        letterSpacing = 0.25.sp,
                                        fontWeight = if (isRes) FontWeight.Medium else FontWeight.Normal
                                    ),
                                    color = displayTextColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .padding(horizontal = if (hasHorizontalOverflow) 4.dp else 0.dp)
                                        .testTag("calculator_main_display")
                                )
                            }
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (errorText != null) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("calculator_error_text")
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = isLizaEasterEgg,
                                enter = fadeIn(tween(180)) + scaleIn(
                                    initialScale = 0.68f,
                                    animationSpec = spring(
                                        dampingRatio = 0.55f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)
                            ) {
                                val pinkPillColor = lerp(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    Color(0xFFFF69B4),
                                    0.24f
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = pinkPillColor,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(enabled = !isLizaCooldown) {
                                            playSound(CalcSoundType.OPERATOR)
                                            performHaptic(CalcSoundType.OPERATOR)
                                            lizaReplayTrigger++
                                        }
                                        .onGloballyPositioned { pillCoords ->
                                            val root = displayRootCoords
                                            if (root != null && root.isAttached && pillCoords.isAttached) {
                                                val rel = pillCoords.positionInRoot() - root.positionInRoot()
                                                burstOriginOffset = Offset(
                                                    x = rel.x + pillCoords.size.width * 0.5f,
                                                    y = rel.y + pillCoords.size.height * 0.5f
                                                )
                                            }
                                        }
                                        .testTag("easter_egg_liza_text")
                                ) {
                                    Text(
                                        text = stringResource(R.string.easter_egg_liza),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            if (!state.isResultDisplayed && state.livePreview.isNotEmpty()) {
                                SelectionContainer {
                                    Text(
                                        text = "= ${state.livePreview}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("calculator_live_preview")
                                    )
                                }
                            }
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
                            onClick = {
                                playSound(CalcSoundType.UTILITY)
                                performHaptic(CalcSoundType.UTILITY)
                                onToggleTopHistory()
                            }
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

            // Heart & sparkle particle burst overlay (reads progress only in Draw phase for 120fps)
            val reusableHeartPath = remember { Path() }
            Canvas(modifier = Modifier.matchParentSize()) {
                val p = particleProgress.value
                if (p <= 0f || p >= 1f) return@Canvas

                val origin = burstOriginOffset ?: Offset(
                    x = size.width * 0.74f,
                    y = size.height - 54.dp.toPx()
                )

                for (particle in lizaParticles) {
                    val localT = ((p - particle.delayFraction) / (1f - particle.delayFraction))
                        .coerceIn(0f, 1f)
                    if (localT <= 0f) continue

                    val easeOut = 1f - (1f - localT).pow(3)
                    val distPx = particle.distanceDp.dp.toPx() * easeOut
                    val liftPx = particle.liftDp.dp.toPx() * localT
                    val cx = origin.x + cos(particle.angleRad) * distPx
                    val cy = origin.y + sin(particle.angleRad) * distPx - liftPx

                    val alpha = when {
                        localT < 0.14f -> localT / 0.14f
                        localT > 0.62f -> ((1f - localT) / 0.38f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                    val scale = when {
                        localT < 0.16f -> 0.35f + 0.65f * (localT / 0.16f)
                        else -> 1f - 0.35f * localT
                    }
                    val radius = particle.sizeDp.dp.toPx() * scale
                    val drawColor = particle.color.copy(alpha = alpha * 0.92f)

                    when (particle.shape) {
                        ParticleShape.DOT -> {
                            drawCircle(
                                color = drawColor.copy(alpha = alpha * 0.28f),
                                radius = radius * 1.7f,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = drawColor,
                                radius = radius * 0.72f,
                                center = Offset(cx, cy)
                            )
                        }
                        ParticleShape.SPARKLE -> {
                            rotate(degrees = particle.rotationDeg * localT, pivot = Offset(cx, cy)) {
                                val arm = radius * 1.25f
                                val waist = radius * 0.32f
                                reusableHeartPath.reset()
                                reusableHeartPath.moveTo(cx, cy - arm)
                                reusableHeartPath.lineTo(cx + waist, cy - waist)
                                reusableHeartPath.lineTo(cx + arm, cy)
                                reusableHeartPath.lineTo(cx + waist, cy + waist)
                                reusableHeartPath.lineTo(cx, cy + arm)
                                reusableHeartPath.lineTo(cx - waist, cy + waist)
                                reusableHeartPath.lineTo(cx - arm, cy)
                                reusableHeartPath.lineTo(cx - waist, cy - waist)
                                reusableHeartPath.close()
                                drawPath(path = reusableHeartPath, color = drawColor)
                            }
                        }
                        ParticleShape.HEART -> {
                            rotate(degrees = particle.rotationDeg * 0.45f * localT, pivot = Offset(cx, cy)) {
                                val s = radius * 1.15f
                                reusableHeartPath.reset()
                                reusableHeartPath.moveTo(cx, cy + s * 0.85f)
                                reusableHeartPath.cubicTo(
                                    cx - s * 1.45f, cy + s * 0.05f,
                                    cx - s * 0.95f, cy - s * 1.05f,
                                    cx, cy - s * 0.25f
                                )
                                reusableHeartPath.cubicTo(
                                    cx + s * 0.95f, cy - s * 1.05f,
                                    cx + s * 1.45f, cy + s * 0.05f,
                                    cx, cy + s * 0.85f
                                )
                                reusableHeartPath.close()
                                drawPath(path = reusableHeartPath, color = drawColor)
                            }
                        }
                    }
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

                    Spacer(modifier = Modifier.height(8.dp))

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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clipToBounds()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 10.dp),
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
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                                Text(
                                                    text = "= ${record.formattedResult}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { onUseResult(record) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onDismiss)
                                .testTag("top_history_collapse_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Text(
                                    text = stringResource(R.string.history_collapse),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedbackTogglePill(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onQuickToggleAll: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val anyEnabled = soundEnabled || vibrationEnabled

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "feedback_press_scale"
    )
    val bgColor = if (anyEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val activeTint = MaterialTheme.colorScheme.primary
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Box {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier
                .height(36.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = { menuExpanded = true },
                    onLongClick = onQuickToggleAll
                )
                .testTag("feedback_mode_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = stringResource(R.string.cd_feedback_toggle),
                    tint = if (soundEnabled) activeTint else inactiveTint,
                    modifier = Modifier.size(16.dp)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(13.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = if (vibrationEnabled) activeTint else inactiveTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 216.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FeedbackMenuSwitchRow(
                    icon = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    label = stringResource(R.string.feedback_sound_label),
                    checked = soundEnabled,
                    onToggle = onToggleSound,
                    testTag = "feedback_menu_sound_row"
                )
                FeedbackMenuSwitchRow(
                    icon = Icons.Rounded.Vibration,
                    label = stringResource(R.string.feedback_vibration_label),
                    checked = vibrationEnabled,
                    onToggle = onToggleVibration,
                    testTag = "feedback_menu_vibration_row"
                )
            }
        }
    }
}

@Composable
private fun FeedbackMenuSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
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
    val chipBgColor = MaterialTheme.colorScheme.surfaceContainerHighest

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
                    (fadeIn(tween(130)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = tween(130)
                    )) togetherWith (fadeOut(tween(90)) + scaleOut(targetScale = 0.7f))
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
                    (fadeIn(tween(130)) + slideInVertically { it / 4 }) togetherWith
                        (fadeOut(tween(90)) + slideOutVertically { -it / 4 })
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
