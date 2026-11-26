package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onQuickToggleFeedback: (Boolean, Boolean) -> Unit,
    onToggleTopHistory: () -> Unit,
    onSetTopHistoryOpen: (Boolean) -> Unit,
    onOpenFullHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onInspectBigNumber: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false,
    onThemeButtonPositioned: ((Offset) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val errorTremor = remember { Animatable(0f) }
    val errorFlash = remember { Animatable(0f) }
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
    val hasError = errorText != null
    var lastErrorText by remember { mutableStateOf("") }
    if (errorText != null) {
        lastErrorText = errorText
    }
    val persistentErrorAlpha by animateFloatAsState(
        targetValue = if (hasError) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "persistent_error_alpha"
    )

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
                    easing = LinearEasing
                )
            )
        } else {
            isLizaCooldown = false
            particleProgress.snapTo(0f)
        }
    }

    val resultRevealAnim = remember { Animatable(1f) }
    var prevIsResultDisplayed by remember { mutableStateOf(state.isResultDisplayed) }
    LaunchedEffect(state.isResultDisplayed, state.rawExpression.isEmpty()) {
        if (scrollState.value != 0) {
            scrollState.scrollTo(0)
        }
        if (state.isResultDisplayed && !prevIsResultDisplayed) {
            resultRevealAnim.snapTo(0f)
            resultRevealAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
            )
        } else if (!state.isResultDisplayed) {
            resultRevealAnim.snapTo(1f)
        }
        prevIsResultDisplayed = state.isResultDisplayed
    }

    LaunchedEffect(state.errorTriggerId) {
        if (state.errorTriggerId > 0L) {
            launch {
                errorTremor.snapTo(0f)
                errorTremor.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 440, easing = LinearEasing)
                )
                errorTremor.snapTo(0f)
            }
            launch {
                errorFlash.snapTo(0f)
                errorFlash.animateTo(1f, animationSpec = tween(durationMillis = 80))
                errorFlash.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 560,
                        easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                    )
                )
            }
        }
    }

    val cardShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    val errorAccentColor = MaterialTheme.colorScheme.error
    val ambientPrimaryColor = MaterialTheme.colorScheme.primary
    val ambientTertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorBorderPath = remember { Path() }

    val ambientDrift = if (state.ambientDisplayGlow) {
        val transition = rememberInfiniteTransition(label = "display_ambient_glow")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "display_ambient_phase"
        ).value
    } else {
        0f
    }

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
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
                .drawBehind {
                    if (state.ambientDisplayGlow) {
                        val c1 = Offset(
                            x = size.width * (0.20f + 0.16f * ambientDrift),
                            y = size.height * (0.28f + 0.14f * (1f - ambientDrift))
                        )
                        val r1 = size.maxDimension * 0.62f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to ambientPrimaryColor.copy(alpha = 0.11f),
                                0.6f to ambientPrimaryColor.copy(alpha = 0.04f),
                                1.0f to Color.Transparent,
                                center = c1,
                                radius = r1
                            ),
                            center = c1,
                            radius = r1
                        )

                        val c2 = Offset(
                            x = size.width * (0.82f - 0.14f * ambientDrift),
                            y = size.height * (0.72f - 0.12f * ambientDrift)
                        )
                        val r2 = size.maxDimension * 0.56f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to ambientTertiaryColor.copy(alpha = 0.09f),
                                0.6f to ambientTertiaryColor.copy(alpha = 0.03f),
                                1.0f to Color.Transparent,
                                center = c2,
                                radius = r2
                            ),
                            center = c2,
                            radius = r2
                        )
                    }

                    val flash = errorFlash.value
                    val persist = persistentErrorAlpha
                    if (flash > 0.005f || persist > 0.005f) {
                        val auraAlpha = (0.055f * persist + 0.13f * flash).coerceIn(0f, 0.24f)
                        val auraCenter = Offset(size.width * 0.72f, size.height * 0.78f)
                        val auraRadius = size.maxDimension * 0.75f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to errorAccentColor.copy(alpha = auraAlpha),
                                0.65f to errorAccentColor.copy(alpha = auraAlpha * 0.35f),
                                1.0f to Color.Transparent,
                                center = auraCenter,
                                radius = auraRadius
                            ),
                            center = auraCenter,
                            radius = auraRadius
                        )

                        val cornerPx = 32.dp.toPx()
                        val strokePx = 1.5.dp.toPx()
                        val inset = strokePx / 2f
                        val strokeAlpha = (0.28f * persist + 0.56f * flash).coerceIn(0f, 0.85f)
                        errorBorderPath.reset()
                        errorBorderPath.addRoundRect(
                            RoundRect(
                                left = inset,
                                top = -cornerPx,
                                right = size.width - inset,
                                bottom = size.height - inset,
                                topLeftCornerRadius = CornerRadius.Zero,
                                topRightCornerRadius = CornerRadius.Zero,
                                bottomLeftCornerRadius = CornerRadius(cornerPx - inset, cornerPx - inset),
                                bottomRightCornerRadius = CornerRadius(cornerPx - inset, cornerPx - inset)
                            )
                        )
                        drawPath(
                            path = errorBorderPath,
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.45f to errorAccentColor.copy(alpha = strokeAlpha * 0.35f),
                                1.0f to errorAccentColor.copy(alpha = strokeAlpha)
                            ),
                            style = Stroke(width = strokePx)
                        )
                    }
                }
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
                        onPositioned = onThemeButtonPositioned,
                        onClick = {
                            playSound(CalcSoundType.OPERATOR)
                            performHaptic(CalcSoundType.OPERATOR)
                            onToggleTheme()
                        }
                    )

                    // Right utility pills: Unified Sound & Vibration pill + History pill + Settings pill
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
                                onQuickToggleFeedback(turnOnBoth, turnOnBoth)
                            },
                            onOpenSettings = onOpenSettings
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

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    playSound(CalcSoundType.OPERATOR)
                                    performHaptic(CalcSoundType.OPERATOR)
                                    onOpenSettings()
                                }
                                .testTag("open_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = stringResource(R.string.cd_settings_button),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
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

                val errorTintColor = lerp(
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.error,
                    0.76f + 0.24f * errorFlash.value
                )
                val displayTextColor by animateColorAsState(
                    targetValue = when {
                        hasError -> errorTintColor
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
                        .graphicsLayer {
                            val t = errorTremor.value
                            if (t > 0f && t < 1f) {
                                val decay = (1f - t).pow(1.45f)
                                val wave = sin(t * PI.toFloat() * 5f)
                                translationX = wave * 11.dp.toPx() * decay
                                val scaleBump = 1f + 0.015f * sin(t * PI.toFloat())
                                scaleX = scaleBump
                                scaleY = scaleBump
                            }
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val safeAvailableWidthPx = with(density) {
                        (maxWidth - 10.dp).toPx().coerceAtLeast(1f)
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
                        fun measureWidth(spVal: Float): Float = textMeasurer.measure(
                            text = mainDisplayText,
                            style = baseDisplayStyle.copy(
                                fontSize = spVal.sp,
                                lineHeight = (spVal * 1.15f).sp,
                                letterSpacing = 0.25.sp,
                                fontWeight = activeFontWeight
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width.toFloat()

                        if (measureWidth(maxFontSp) <= safeAvailableWidthPx) {
                            maxFontSp.sp
                        } else if (measureWidth(minFontSp) > safeAvailableWidthPx) {
                            minFontSp.sp
                        } else {
                            var low = minFontSp
                            var high = maxFontSp
                            repeat(10) {
                                val mid = (low + high) * 0.5f
                                if (measureWidth(mid) <= safeAvailableWidthPx) {
                                    low = mid
                                } else {
                                    high = mid
                                }
                            }
                            low.sp
                        }
                    }

                    val hasHorizontalOverflow = scrollState.maxValue > 0
                    val fadeLeft = hasHorizontalOverflow && scrollState.value < scrollState.maxValue
                    val fadeRight = hasHorizontalOverflow && scrollState.value > 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .then(
                                if (fadeLeft || fadeRight) {
                                    Modifier
                                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                        .drawWithContent {
                                            drawContent()
                                            val fadePx = 24.dp.toPx().coerceAtMost(size.width * 0.16f)
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
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState, reverseScrolling = true),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer {
                                Text(
                                    text = mainDisplayText,
                                    style = baseDisplayStyle.copy(
                                        fontSize = dynamicFontSize,
                                        lineHeight = dynamicFontSize * 1.15f,
                                        letterSpacing = 0.25.sp,
                                        fontWeight = activeFontWeight
                                    ),
                                    color = displayTextColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .graphicsLayer {
                                            val p = resultRevealAnim.value
                                            if (p < 1f) {
                                                alpha = 0.25f + 0.75f * p
                                                translationY = (1f - p) * 14.dp.toPx()
                                            }
                                        }
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
                        .heightIn(min = 38.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = errorText != null,
                        enter = fadeIn(tween(150)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) +
                            scaleIn(
                                initialScale = 0.86f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.90f)
                    ) {
                        val iconPulseScale = 1f + 0.16f * errorFlash.value
                        val pillContainerColor = lerp(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.error,
                            0.18f * errorFlash.value
                        )
                        Surface(
                            shape = CircleShape,
                            color = pillContainerColor,
                            modifier = Modifier.graphicsLayer {
                                val t = errorTremor.value
                                if (t > 0f && t < 1f) {
                                    val decay = (1f - t).pow(1.6f)
                                    translationX = sin(t * PI.toFloat() * 5f) * 4.dp.toPx() * decay
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer {
                                            scaleX = iconPulseScale
                                            scaleY = iconPulseScale
                                        }
                                )
                                Text(
                                    text = errorText ?: lastErrorText,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("calculator_error_text")
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = errorText == null,
                        enter = fadeIn(tween(140)),
                        exit = fadeOut(tween(90))
                    ) {
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

                            if (state.showLivePreview && !state.isResultDisplayed && state.livePreview.isNotEmpty()) {
                                SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
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
                val pullHandleCd = stringResource(R.string.cd_pull_handle)
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
                        .semantics { contentDescription = pullHandleCd }
                        .padding(top = 6.dp, bottom = 5.dp)
                        .testTag("top_history_pull_handle"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
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
    onQuickToggleAll: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val anyEnabled = soundEnabled || vibrationEnabled

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "feedback_press_scale"
    )
    // Mix factors animate only when sound/vibration toggles, so theme changes snap in 1 frame with zero color lag
    val anyMix by animateFloatAsState(
        targetValue = if (anyEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "feedback_pill_bg_mix"
    )
    val soundMix by animateFloatAsState(
        targetValue = if (soundEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 210),
        label = "feedback_sound_mix"
    )
    val vibMix by animateFloatAsState(
        targetValue = if (vibrationEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 210),
        label = "feedback_vibration_mix"
    )

    val bgColor = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        anyMix
    )
    val activeTint = MaterialTheme.colorScheme.primary
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
    val soundTint = lerp(inactiveTint, activeTint, soundMix)
    val vibrationTint = lerp(inactiveTint, activeTint, vibMix)

    val soundEnablePulse = remember { Animatable(1f) }
    val soundEnableTilt = remember { Animatable(0f) }
    var prevSoundEnabled by remember { mutableStateOf(soundEnabled) }
    LaunchedEffect(soundEnabled) {
        if (soundEnabled && !prevSoundEnabled) {
            launch {
                soundEnablePulse.snapTo(0.84f)
                soundEnablePulse.animateTo(
                    targetValue = 1.26f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                soundEnablePulse.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                soundEnableTilt.snapTo(0f)
                soundEnableTilt.animateTo(-14f, tween(90))
                soundEnableTilt.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
        prevSoundEnabled = soundEnabled
    }

    val vibEnableAnim = remember { Animatable(0f) }
    var prevVibrationEnabled by remember { mutableStateOf(vibrationEnabled) }
    LaunchedEffect(vibrationEnabled) {
        if (vibrationEnabled && !prevVibrationEnabled) {
            vibEnableAnim.snapTo(0f)
            vibEnableAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = LinearEasing)
            )
            vibEnableAnim.snapTo(0f)
        }
        prevVibrationEnabled = vibrationEnabled
    }

    Box {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier
                .height(36.dp)
                .graphicsLayer {
                    val s = pressScaleState.value
                    scaleX = s
                    scaleY = s
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
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = soundEnablePulse.value
                            scaleY = soundEnablePulse.value
                            rotationZ = soundEnableTilt.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = soundEnabled,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.72f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.72f))
                        },
                        label = "feedback_sound_icon"
                    ) { enabled ->
                        Icon(
                            imageVector = if (enabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = stringResource(R.string.cd_feedback_toggle),
                            tint = soundTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(13.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            val t = vibEnableAnim.value
                            if (t > 0f && t < 1f) {
                                val decay = (1f - t).pow(1.25f)
                                val wave = sin(t * PI.toFloat() * 5f)
                                translationX = wave * 2.4.dp.toPx() * decay
                                rotationZ = wave * 11f * decay
                                val pop = 1f + 0.24f * sin(t * PI.toFloat())
                                scaleX = pop
                                scaleY = pop
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    VibrationStatusIcon(
                        enabled = vibrationEnabled,
                        tint = vibrationTint,
                        gapColor = bgColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                    isVibrationRow = false,
                    onToggle = onToggleSound,
                    testTag = "feedback_menu_sound_row"
                )
                FeedbackMenuSwitchRow(
                    icon = Icons.Rounded.Vibration,
                    label = stringResource(R.string.feedback_vibration_label),
                    checked = vibrationEnabled,
                    isVibrationRow = true,
                    onToggle = onToggleVibration,
                    testTag = "feedback_menu_vibration_row"
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            menuExpanded = false
                            onOpenSettings()
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("feedback_menu_open_settings"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_open_all),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VibrationStatusIcon(
    enabled: Boolean,
    tint: Color,
    gapColor: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val slashProgress by animateFloatAsState(
        targetValue = if (enabled) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "vibration_slash_progress"
    )
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            if (slashProgress > 0.01f) {
                val start = Offset(size.width * 0.14f, size.height * 0.14f)
                val fullEnd = Offset(size.width * 0.86f, size.height * 0.86f)
                val end = Offset(
                    x = start.x + (fullEnd.x - start.x) * slashProgress,
                    y = start.y + (fullEnd.y - start.y) * slashProgress
                )
                val gapShift = 0.95.dp.toPx()
                drawLine(
                    color = gapColor,
                    start = Offset(start.x + gapShift, start.y - gapShift),
                    end = Offset(end.x + gapShift, end.y - gapShift),
                    strokeWidth = 3.0.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = start,
                    end = end,
                    strokeWidth = 1.75.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Vibration,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FeedbackMenuSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    isVibrationRow: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    val checkedMix by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menu_row_icon_mix"
    )
    val iconTint = lerp(
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.primary,
        checkedMix
    )
    val toggleAnim = remember { Animatable(0f) }
    var prevChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked && !prevChecked) {
            toggleAnim.snapTo(0f)
            toggleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = LinearEasing)
            )
            toggleAnim.snapTo(0f)
        }
        prevChecked = checked
    }

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
        val iconModifier = Modifier
            .size(19.dp)
            .graphicsLayer {
                val t = toggleAnim.value
                if (t > 0f && t < 1f) {
                    val pop = 1f + 0.24f * sin(t * PI.toFloat())
                    scaleX = pop
                    scaleY = pop
                    if (isVibrationRow) {
                        val decay = (1f - t).pow(1.2f)
                        rotationZ = sin(t * PI.toFloat() * 5f) * 11f * decay
                    } else {
                        rotationZ = -12f * sin(t * PI.toFloat())
                    }
                }
            }
        if (isVibrationRow) {
            VibrationStatusIcon(
                enabled = checked,
                tint = iconTint,
                gapColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = iconModifier
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = iconModifier
            )
        }
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
    onPositioned: ((Offset) -> Unit)? = null,
    onClick: () -> Unit
) {
    val label = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "theme_chip_scale"
    )
    val chipBgColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val iconSpin = remember { Animatable(0f) }
    val switchBurst = remember { Animatable(0f) }
    var prevThemeMode by remember { mutableStateOf(themeMode) }

    LaunchedEffect(themeMode) {
        if (themeMode != prevThemeMode) {
            prevThemeMode = themeMode
            launch {
                val startDeg = iconSpin.value
                iconSpin.animateTo(
                    targetValue = startDeg + 360f,
                    animationSpec = spring(
                        dampingRatio = 0.64f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                switchBurst.snapTo(1f)
                switchBurst.animateTo(0f, tween(durationMillis = 360, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
            }
        }
    }

    Surface(
        shape = CircleShape,
        color = chipBgColor,
        modifier = Modifier
            .height(36.dp)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val pos = coords.positionInRoot()
                    onPositioned?.invoke(
                        Offset(
                            x = pos.x + coords.size.width * 0.5f,
                            y = pos.y + coords.size.height * 0.5f
                        )
                    )
                }
            }
            .graphicsLayer {
                val burstScale = 1f + 0.06f * sin(switchBurst.value * PI.toFloat())
                val s = pressScaleState.value * burstScale
                scaleX = s
                scaleY = s
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
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = iconSpin.value
                        val pop = 1f + 0.25f * sin(switchBurst.value * PI.toFloat())
                        scaleX = pop
                        scaleY = pop
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = themeMode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(
                            initialScale = 0.55f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )) togetherWith (fadeOut(tween(110)) + scaleOut(targetScale = 0.55f))
                    },
                    label = "theme_icon"
                ) { mode ->
                    val icon = when (mode) {
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
            }
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    (fadeIn(tween(170)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(110)) + slideOutVertically { -it / 3 })
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

@Composable
fun FeedbackPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isMediaVolumeZero = remember(context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am != null && am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        }.getOrDefault(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.feedback_permission_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.feedback_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (isMediaVolumeZero) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                }
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.feedback_permission_muted_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                modifier = Modifier.testTag("feedback_permission_allow_button")
            ) {
                Text(text = stringResource(R.string.feedback_permission_allow))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isMediaVolumeZero) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.feedback_permission_system_settings))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("feedback_permission_deny_button")
                ) {
                    Text(text = stringResource(R.string.feedback_permission_deny))
                }
            }
        }
    )
}
