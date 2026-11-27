package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CalcSoundType
import com.example.data.preferences.KeyPressEffect
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

val LocalPlayCalcSound = compositionLocalOf<(CalcSoundType) -> Unit> { {} }
val LocalPerformCalcHaptic = compositionLocalOf<(CalcSoundType) -> Unit> { {} }

private val GlowFadeEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

// Precomputed 7-direction spark vectors to avoid per-frame trig allocations
private val SPARK_COS = FloatArray(7) { i ->
    cos((-90f + i * (360f / 7f)) * (PI.toFloat() / 180f))
}
private val SPARK_SIN = FloatArray(7) { i ->
    sin((-90f + i * (360f / 7f)) * (PI.toFloat() / 180f))
}

enum class CalcButtonRole {
    DIGIT,
    OPERATOR,
    UTILITY,
    EQUALS,
    SCIENTIFIC,
    SCIENTIFIC_ACTIVE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveCalcButton(
    label: String,
    role: CalcButtonRole,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescriptionText: String = label,
    onLongClick: (() -> Unit)? = null,
    hasGlow: Boolean = false,
    glowIntensity: Float = 1.0f,
    glowColorOverride: Color? = null,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    glowBreathingEnabled: Boolean = false,
    restingCornerRadius: Dp = 50.dp,
    pressedCornerRadius: Dp = 16.dp,
    textStyle: TextStyle? = null,
    textScale: Float = 1f,
    contentScaleState: State<Float>? = null,
    content: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentPlaySound by rememberUpdatedState(LocalPlayCalcSound.current)
    val currentPerformHaptic by rememberUpdatedState(LocalPerformCalcHaptic.current)

    val glowPulse = remember(hasGlow) { if (hasGlow) Animatable(0f) else null }
    val pressEffectAnim = remember(keyPressEffect) {
        if (keyPressEffect == KeyPressEffect.SHOCKWAVE || keyPressEffect == KeyPressEffect.NEON_BURST) {
            Animatable(0f)
        } else {
            null
        }
    }
    val sparkAnim = remember(pressSparksEnabled) {
        if (pressSparksEnabled) Animatable(0f) else null
    }
    val pressPosRef = remember { FloatArray(2) { -1f } }

    val soundType = remember(role) {
        when (role) {
            CalcButtonRole.DIGIT -> CalcSoundType.DIGIT
            CalcButtonRole.OPERATOR,
            CalcButtonRole.SCIENTIFIC,
            CalcButtonRole.SCIENTIFIC_ACTIVE -> CalcSoundType.OPERATOR
            CalcButtonRole.UTILITY -> CalcSoundType.UTILITY
            CalcButtonRole.EQUALS -> CalcSoundType.EQUALS
        }
    }

    // Immediate touch-down audio, vibration, and silky glow/press-effect/spark pulses
    LaunchedEffect(interactionSource, soundType, glowPulse, pressEffectAnim, sparkAnim) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                pressPosRef[0] = interaction.pressPosition.x
                pressPosRef[1] = interaction.pressPosition.y
                currentPlaySound(soundType)
                currentPerformHaptic(soundType)
                if (glowPulse != null) {
                    launch {
                        glowPulse.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 48)
                        )
                        glowPulse.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 320, easing = GlowFadeEasing)
                        )
                    }
                }
                if (pressEffectAnim != null) {
                    launch {
                        pressEffectAnim.snapTo(0f)
                        pressEffectAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 340, easing = GlowFadeEasing)
                        )
                        pressEffectAnim.snapTo(0f)
                    }
                }
                if (sparkAnim != null) {
                    launch {
                        sparkAnim.snapTo(0f)
                        sparkAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = if (role == CalcButtonRole.EQUALS) 460 else 360,
                                easing = GlowFadeEasing
                            )
                        )
                        sparkAnim.snapTo(0f)
                    }
                }
            }
        }
    }

    // Material 3 Expressive snappy shape morphing tuned for 120Hz displays
    val cornerRadiusState = animateDpAsState(
        targetValue = if (isPressed) pressedCornerRadius else restingCornerRadius,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.82f, stiffness = 3600f)
        } else {
            spring(dampingRatio = 0.62f, stiffness = 1150f)
        },
        label = "button_corner_morph"
    )

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.82f, stiffness = 3600f)
        } else {
            spring(dampingRatio = 0.62f, stiffness = 1150f)
        },
        label = "button_scale"
    )

    val restingContainerColor = when (role) {
        CalcButtonRole.DIGIT -> MaterialTheme.colorScheme.surfaceContainerHigh
        CalcButtonRole.OPERATOR -> MaterialTheme.colorScheme.secondaryContainer
        CalcButtonRole.UTILITY -> MaterialTheme.colorScheme.tertiaryContainer
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.primary
        CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.surfaceContainer
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    }
    val pressedContainerColor = when (role) {
        CalcButtonRole.DIGIT, CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.surfaceContainerHighest
        CalcButtonRole.OPERATOR -> MaterialTheme.colorScheme.secondary
        CalcButtonRole.UTILITY -> MaterialTheme.colorScheme.tertiary
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.primary
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    }

    val restingContentColor = when (role) {
        CalcButtonRole.DIGIT -> MaterialTheme.colorScheme.onSurface
        CalcButtonRole.OPERATOR -> MaterialTheme.colorScheme.onSecondaryContainer
        CalcButtonRole.UTILITY -> MaterialTheme.colorScheme.onTertiaryContainer
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.onPrimary
        CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.onSurfaceVariant
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val pressedContentColor = when (role) {
        CalcButtonRole.OPERATOR -> MaterialTheme.colorScheme.onSecondary
        CalcButtonRole.UTILITY -> MaterialTheme.colorScheme.onTertiary
        else -> restingContentColor
    }

    // Mix factor only animates on press, so theme switches snap in 1 frame with zero running color animations
    val pressMixState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 55),
        label = "button_press_mix"
    )
    val contentColor = if (restingContentColor == pressedContentColor) {
        restingContentColor
    } else {
        lerp(restingContentColor, pressedContentColor, pressMixState.value)
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = if (hasGlow) remember(backgroundColor) { backgroundColor.luminance() < 0.5f } else false
    val softGlowColor = if (hasGlow) {
        val rawGlowColor = glowColorOverride ?: when (role) {
            CalcButtonRole.EQUALS -> primaryColor
            CalcButtonRole.UTILITY -> tertiaryColor
            else -> primaryColor
        }
        remember(rawGlowColor, isDark) {
            if (isDark) lerp(rawGlowColor, Color.White, 0.18f) else rawGlowColor
        }
    } else {
        Color.Transparent
    }
    val innerTintColor = if (hasGlow) {
        remember(role, isDark, primaryContainerColor, tertiaryColor, glowColorOverride) {
            if (glowColorOverride != null) {
                lerp(glowColorOverride, Color.White, if (isDark) 0.36f else 0.56f)
            } else {
                when (role) {
                    CalcButtonRole.EQUALS -> lerp(primaryContainerColor, Color.White, if (isDark) 0.35f else 0.55f)
                    CalcButtonRole.UTILITY -> lerp(tertiaryColor, Color.White, if (isDark) 0.40f else 0.65f)
                    else -> Color.White
                }
            }
        }
    } else {
        Color.Transparent
    }
    val clampedGlowIntensity = glowIntensity.coerceIn(0.25f, 1.75f)
    val glowRoleScale = if (role == CalcButtonRole.DIGIT || role == CalcButtonRole.OPERATOR) 0.58f else 1f
    val breathMulState = if (hasGlow && glowBreathingEnabled) {
        val breathTransition = rememberInfiniteTransition(label = "key_glow_breath")
        breathTransition.animateFloat(
            initialValue = 0.78f,
            targetValue = 1.24f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "key_glow_breath_mul"
        )
    } else {
        null
    }
    val sparkTintColor = when {
        hasGlow -> softGlowColor
        glowColorOverride != null -> glowColorOverride
        role == CalcButtonRole.EQUALS -> primaryColor
        role == CalcButtonRole.UTILITY -> tertiaryColor
        else -> primaryColor
    }

    // Dynamic Shape reads cornerRadiusState during outline/draw phase so press morphing avoids recomposition
    val dynamicShape = remember(cornerRadiusState) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val r = with(density) { cornerRadiusState.value.toPx() }
                    .coerceIn(0f, minOf(size.width, size.height) / 2f)
                return Outline.Rounded(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(r, r)
                    )
                )
            }
        }
    }

    val clickIndication = ripple(bounded = true, color = restingContentColor.copy(alpha = 0.20f))

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = clickIndication,
            role = Role.Button,
            onClick = onClick,
            onLongClick = {
                currentPerformHaptic(CalcSoundType.UTILITY)
                onLongClick()
            }
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = clickIndication,
            role = Role.Button,
            onClick = onClick
        )
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
                if (keyPressEffect == KeyPressEffect.SOFT_TILT && pressMixState.value > 0.001f &&
                    size.width > 1f && size.height > 1f
                ) {
                    val px = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                    val py = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                    val nx = ((px / size.width) - 0.5f) * 2f
                    val ny = ((py / size.height) - 0.5f) * 2f
                    val tiltMix = pressMixState.value
                    rotationY = nx * 10f * tiltMix
                    rotationX = -ny * 10f * tiltMix
                    cameraDistance = 12f * density
                }
            }
            .then(
                if ((hasGlow && glowPulse != null) || sparkAnim != null) {
                    Modifier.drawBehind {
                        if (hasGlow && glowPulse != null) {
                            val breathMul = breathMulState?.value ?: 1f
                            val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                            val basePeakAlpha = when {
                                role == CalcButtonRole.EQUALS && isDark -> 0.40f
                                role == CalcButtonRole.EQUALS -> 0.34f
                                isDark -> 0.28f
                                else -> 0.23f
                            }
                            val peakAlpha = (basePeakAlpha * clampedGlowIntensity * glowRoleScale * breathMul +
                                pulse * 0.20f * clampedGlowIntensity).coerceIn(0f, 0.90f)
                            val spreadFactor = (0.70f + 0.30f * clampedGlowIntensity) *
                                (if (glowRoleScale < 1f) 0.78f else 1f) * (0.90f + 0.10f * breathMul)
                            val maxSpreadPx = ((8.5f * spreadFactor) + pulse * 5f * clampedGlowIntensity).dp.toPx()

                            val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                            val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)
                            val roundness = (clampedCornerPx / minHalf).coerceIn(0f, 1f)
                            val cornerExpandRatio = 0.56f + 0.44f * roundness

                            val steps = 3
                            for (i in steps downTo 1) {
                                val t = i.toFloat() / steps.toFloat()
                                val expand = maxSpreadPx * t
                                val falloff = (1f - t * 0.80f)
                                val layerAlpha = (peakAlpha * 0.56f * falloff * falloff).coerceIn(0f, 1f)
                                val layerCorner = (clampedCornerPx + expand * cornerExpandRatio).coerceAtLeast(0f)
                                drawRoundRect(
                                    color = softGlowColor.copy(alpha = layerAlpha),
                                    topLeft = Offset(-expand, -expand),
                                    size = Size(size.width + expand * 2f, size.height + expand * 2f),
                                    cornerRadius = CornerRadius(layerCorner, layerCorner)
                                )
                            }
                        }

                        if (sparkAnim != null) {
                            val sp = sparkAnim.value
                            if (sp > 0.001f && sp < 0.999f) {
                                val cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                                val cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                                val maxDist = (maxOf(size.width, size.height) *
                                    if (role == CalcButtonRole.EQUALS) 0.76f else 0.58f).coerceAtLeast(24.dp.toPx())
                                val ease = 1f - (1f - sp).pow(2.4f)
                                val alpha = (1f - sp).pow(1.35f) * 0.88f
                                val baseR = (if (role == CalcButtonRole.EQUALS) 3.4f else 2.5f).dp.toPx() * (1f - 0.55f * sp)
                                for (i in 0 until SPARK_COS.size) {
                                    val dist = maxDist * ease * (0.72f + 0.28f * ((i % 3) / 2f))
                                    val sx = cx + SPARK_COS[i] * dist
                                    val sy = cy + SPARK_SIN[i] * dist - (6.dp.toPx() * sp)
                                    val dotColor = if (i % 2 == 0) sparkTintColor else Color.White
                                    drawCircle(
                                        color = dotColor.copy(alpha = alpha),
                                        radius = baseR,
                                        center = Offset(sx, sy)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .clip(dynamicShape)
            .drawBehind {
                val mix = pressMixState.value
                val bg = if (mix <= 0.001f || restingContainerColor == pressedContainerColor) {
                    restingContainerColor
                } else {
                    lerp(restingContainerColor, pressedContainerColor, mix)
                }
                drawRect(bg)

                if (pressEffectAnim != null) {
                    val t = pressEffectAnim.value
                    if (t > 0.001f && t < 0.999f) {
                        val ox = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                        val oy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                        val center = Offset(ox, oy)
                        val maxR = maxOf(size.width, size.height) * 0.92f
                        when (keyPressEffect) {
                            KeyPressEffect.SHOCKWAVE -> {
                                val r = (6.dp.toPx() + maxR * t).coerceAtLeast(1f)
                                val waveAlpha = (1f - t).pow(1.45f) * 0.58f
                                drawCircle(
                                    color = restingContentColor.copy(alpha = waveAlpha),
                                    radius = r,
                                    center = center,
                                    style = Stroke(width = (2.6f - 1.6f * t).dp.toPx())
                                )
                            }
                            KeyPressEffect.NEON_BURST -> {
                                val flashAlpha = (1f - t).pow(1.6f) * 0.52f
                                val r = (maxR * (0.45f + 0.55f * t)).coerceAtLeast(1f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.0f to Color.White.copy(alpha = flashAlpha),
                                        0.55f to sparkTintColor.copy(alpha = flashAlpha * 0.72f),
                                        1.0f to Color.Transparent,
                                        center = center,
                                        radius = r
                                    ),
                                    center = center,
                                    radius = r
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
            .then(
                if (hasGlow && glowPulse != null) {
                    Modifier.drawWithCache {
                        val sheenBrush = Brush.verticalGradient(
                            0.0f to innerTintColor,
                            0.55f to innerTintColor.copy(alpha = 0.28f),
                            1.0f to Color.Transparent
                        )
                        val rimBrush = Brush.verticalGradient(
                            0.0f to innerTintColor,
                            1.0f to softGlowColor.copy(alpha = 0.24f)
                        )
                        val inset = 5.dp.toPx()
                        val strokePx = 1.25.dp.toPx()
                        val strokeHalf = strokePx / 2f
                        val rimStroke = Stroke(width = strokePx)

                        onDrawBehind {
                            val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                            val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                            val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)

                            val topSheenAlpha = (((if (role == CalcButtonRole.EQUALS) 0.18f else 0.14f) * clampedGlowIntensity * glowRoleScale) + pulse * 0.14f * clampedGlowIntensity)
                                .coerceIn(0f, 0.72f)
                            drawRect(brush = sheenBrush, alpha = topSheenAlpha)

                            val innerPulseAlpha = (((if (role == CalcButtonRole.EQUALS) 0.11f else 0.075f) * clampedGlowIntensity * glowRoleScale) + pulse * 0.15f * clampedGlowIntensity)
                                .coerceIn(0f, 0.65f)
                            if (size.width > inset * 2f && size.height > inset * 2f) {
                                val innerCorner = (clampedCornerPx - inset * 0.85f).coerceAtLeast(0f)
                                drawRoundRect(
                                    color = innerTintColor.copy(alpha = innerPulseAlpha * 0.72f),
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                                    cornerRadius = CornerRadius(innerCorner, innerCorner)
                                )
                            }

                            val rimTopAlpha = (((if (role == CalcButtonRole.EQUALS) 0.32f else 0.25f) * clampedGlowIntensity * glowRoleScale) + pulse * 0.22f * clampedGlowIntensity)
                                .coerceIn(0f, 0.92f)
                            drawRoundRect(
                                brush = rimBrush,
                                alpha = rimTopAlpha,
                                topLeft = Offset(strokeHalf, strokeHalf),
                                size = Size(
                                    (size.width - strokePx).coerceAtLeast(0f),
                                    (size.height - strokePx).coerceAtLeast(0f)
                                ),
                                cornerRadius = CornerRadius(
                                    (clampedCornerPx - strokeHalf).coerceAtLeast(0f),
                                    (clampedCornerPx - strokeHalf).coerceAtLeast(0f)
                                ),
                                style = rimStroke
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(clickModifier)
            .semantics { contentDescription = contentDescriptionText }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        val innerModifier = Modifier
            .wrapContentHeight(unbounded = true)
            .then(
                if (contentScaleState != null) {
                    Modifier.graphicsLayer {
                        val s = contentScaleState.value
                        scaleX = s
                        scaleY = s
                    }
                } else {
                    Modifier
                }
            )
        if (content != null) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
                    content()
                }
            }
        } else {
            val resolvedStyle = textStyle ?: when (role) {
                CalcButtonRole.SCIENTIFIC, CalcButtonRole.SCIENTIFIC_ACTIVE ->
                    MaterialTheme.typography.labelLarge.copy(
                        fontSize = (12.5f * textScale).sp,
                        fontWeight = FontWeight.Medium
                    )
                CalcButtonRole.OPERATOR, CalcButtonRole.EQUALS ->
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = (24f * textScale).sp,
                        fontWeight = FontWeight.Normal
                    )
                CalcButtonRole.UTILITY ->
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = (19f * textScale).sp,
                        fontWeight = FontWeight.Medium
                    )
                else ->
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = (25f * textScale).sp,
                        fontWeight = FontWeight.Normal
                    )
            }
            Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = resolvedStyle,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(PaddingValues(horizontal = 4.dp))
                )
            }
        }
    }
}

/**
 * Lightweight, flat scientific key used inside the redesigned fx deck.
 * Avoids multi-channel animate*AsState allocations, dynamic Shape outline churn,
 * and nested wrappers so opening/closing the 15-key fx panel stays locked at 90/120Hz.
 */
@Composable
fun ScientificCalcButton(
    label: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    restingCornerRadius: Dp = 15.dp,
    pressedCornerRadius: Dp = 10.dp,
    textStyle: TextStyle? = null,
    containerColor: Color? = null,
    pressedContainerColor: Color? = null,
    contentColorOverride: Color? = null,
    borderColor: Color? = null,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAnim = remember { Animatable(0f) }
    val effectAnim = remember(keyPressEffect) {
        if (keyPressEffect == KeyPressEffect.SHOCKWAVE || keyPressEffect == KeyPressEffect.NEON_BURST) {
            Animatable(0f)
        } else {
            null
        }
    }
    val sparkAnim = remember(pressSparksEnabled) {
        if (pressSparksEnabled) Animatable(0f) else null
    }
    val pressPosRef = remember { FloatArray(2) { -1f } }
    val currentPlaySound by rememberUpdatedState(LocalPlayCalcSound.current)
    val currentPerformHaptic by rememberUpdatedState(LocalPerformCalcHaptic.current)

    LaunchedEffect(interactionSource, effectAnim, sparkAnim) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPosRef[0] = interaction.pressPosition.x
                    pressPosRef[1] = interaction.pressPosition.y
                    currentPlaySound(CalcSoundType.OPERATOR)
                    currentPerformHaptic(CalcSoundType.OPERATOR)
                    launch {
                        pressAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 3600f)
                        )
                    }
                    if (effectAnim != null) {
                        launch {
                            effectAnim.snapTo(0f)
                            effectAnim.animateTo(1f, tween(300, easing = GlowFadeEasing))
                            effectAnim.snapTo(0f)
                        }
                    }
                    if (sparkAnim != null) {
                        launch {
                            sparkAnim.snapTo(0f)
                            sparkAnim.animateTo(1f, tween(320, easing = GlowFadeEasing))
                            sparkAnim.snapTo(0f)
                        }
                    }
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    launch {
                        pressAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.68f, stiffness = 1250f)
                        )
                    }
                }
            }
        }
    }

    val restingBg = containerColor ?: if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val pressedBg = pressedContainerColor ?: if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = contentColorOverride ?: if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val primaryTint = MaterialTheme.colorScheme.primary

    val clipShape = remember(restingCornerRadius) { RoundedCornerShape(restingCornerRadius) }
    val resolvedStyle = textStyle ?: MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val p = pressAnim.value
                if (p > 0.001f) {
                    val s = 1f - 0.055f * p
                    scaleX = s
                    scaleY = s
                    if (keyPressEffect == KeyPressEffect.SOFT_TILT && size.width > 1f && size.height > 1f) {
                        val px = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                        val py = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                        rotationY = ((px / size.width) - 0.5f) * 16f * p
                        rotationX = -((py / size.height) - 0.5f) * 16f * p
                        cameraDistance = 12f * density
                    }
                }
            }
            .then(
                if (sparkAnim != null) {
                    Modifier.drawBehind {
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            val cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                            val cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                            val maxDist = (maxOf(size.width, size.height) * 0.54f).coerceAtLeast(18.dp.toPx())
                            val ease = 1f - (1f - sp).pow(2.4f)
                            val alpha = (1f - sp).pow(1.35f) * 0.80f
                            val baseR = 2.dp.toPx() * (1f - 0.55f * sp)
                            for (i in 0 until SPARK_COS.size) {
                                val dist = maxDist * ease * (0.70f + 0.30f * ((i % 3) / 2f))
                                drawCircle(
                                    color = (if (i % 2 == 0) primaryTint else Color.White).copy(alpha = alpha),
                                    radius = baseR,
                                    center = Offset(cx + SPARK_COS[i] * dist, cy + SPARK_SIN[i] * dist)
                                )
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .clip(clipShape)
            .drawBehind {
                val p = pressAnim.value
                val bg = if (p <= 0.001f || restingBg == pressedBg) restingBg else lerp(restingBg, pressedBg, p)
                val restingPx = restingCornerRadius.toPx()
                val pressedPx = pressedCornerRadius.toPx()
                val maxR = minOf(size.width, size.height) / 2f
                val r = (restingPx + (pressedPx - restingPx) * p).coerceIn(0f, maxR)
                drawRoundRect(
                    color = bg,
                    cornerRadius = CornerRadius(r, r)
                )
                if (effectAnim != null) {
                    val t = effectAnim.value
                    if (t > 0.001f && t < 0.999f) {
                        val ox = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f
                        val oy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f
                        val center = Offset(ox, oy)
                        val maxWaveR = maxOf(size.width, size.height) * 0.85f
                        when (keyPressEffect) {
                            KeyPressEffect.SHOCKWAVE -> {
                                drawCircle(
                                    color = contentColor.copy(alpha = (1f - t).pow(1.45f) * 0.48f),
                                    radius = (4.dp.toPx() + maxWaveR * t).coerceAtLeast(1f),
                                    center = center,
                                    style = Stroke(width = (2.1f - 1.3f * t).dp.toPx())
                                )
                            }
                            KeyPressEffect.NEON_BURST -> {
                                val flashAlpha = (1f - t).pow(1.6f) * 0.44f
                                val rad = (maxWaveR * (0.45f + 0.55f * t)).coerceAtLeast(1f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.0f to Color.White.copy(alpha = flashAlpha),
                                        0.6f to primaryTint.copy(alpha = flashAlpha * 0.65f),
                                        1.0f to Color.Transparent,
                                        center = center,
                                        radius = rad
                                    ),
                                    center = center,
                                    radius = rad
                                )
                            }
                            else -> Unit
                        }
                    }
                }
                if (borderColor != null) {
                    val strokePx = 1.dp.toPx()
                    val half = strokePx / 2f
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(half, half),
                        size = Size((size.width - strokePx).coerceAtLeast(0f), (size.height - strokePx).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius((r - half).coerceAtLeast(0f), (r - half).coerceAtLeast(0f)),
                        style = Stroke(width = strokePx)
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.22f)),
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = resolvedStyle,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

