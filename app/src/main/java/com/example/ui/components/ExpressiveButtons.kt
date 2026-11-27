package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.zIndex
import com.example.audio.CalcSoundType
import com.example.data.preferences.KeyPressEffect
import com.example.data.preferences.PressSparkStyle
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

val LocalPlayCalcSound = compositionLocalOf<(CalcSoundType) -> Unit> { {} }
val LocalPerformCalcHaptic = compositionLocalOf<(CalcSoundType) -> Unit> { {} }

private val GlowFadeEasing = CubicBezierEasing(0.18f, 1f, 0.32f, 1f)

// Precomputed 14-direction spark lookup tables (zero per-frame allocations)
private const val SPARK_COUNT = 14
private val SPARK_ANGLES_RAD = FloatArray(SPARK_COUNT) { i ->
    val jitter = ((i * 37) % 13) - 6f
    ((-90f + i * (360f / SPARK_COUNT) + jitter) * (PI.toFloat() / 180f))
}
private val SPARK_COS = FloatArray(SPARK_COUNT) { i -> cos(SPARK_ANGLES_RAD[i]) }
private val SPARK_SIN = FloatArray(SPARK_COUNT) { i -> sin(SPARK_ANGLES_RAD[i]) }
private val SPARK_SPEED = FloatArray(SPARK_COUNT) { i -> 0.72f + ((i * 53) % 10) * 0.045f }
private val SPARK_CURL = FloatArray(SPARK_COUNT) { i -> if (i % 2 == 0) 1f else -1f }

// Precomputed 12-shell firework lookup tables
private const val FW_COUNT = 12
private val FW_ANGLES_RAD = FloatArray(FW_COUNT) { i ->
    ((-90f + i * (360f / FW_COUNT) + if (i % 2 == 0) 4f else -4f) * (PI.toFloat() / 180f))
}
private val FW_COS = FloatArray(FW_COUNT) { i -> cos(FW_ANGLES_RAD[i]) }
private val FW_SIN = FloatArray(FW_COUNT) { i -> sin(FW_ANGLES_RAD[i]) }
private val FW_SUB_COS = FloatArray(FW_COUNT * 2) { idx ->
    val shell = idx / 2
    val offset = if (idx % 2 == 0) 0.42f else -0.42f
    cos(FW_ANGLES_RAD[shell] + offset)
}
private val FW_SUB_SIN = FloatArray(FW_COUNT * 2) { idx ->
    val shell = idx / 2
    val offset = if (idx % 2 == 0) 0.42f else -0.42f
    sin(FW_ANGLES_RAD[shell] + offset)
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
    colorTintEnabled: Boolean = true,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    pressSparkStyle: PressSparkStyle = PressSparkStyle.SPARKS,
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
    val sparkPath = remember(pressSparksEnabled) {
        if (pressSparksEnabled) Path() else null
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
                                durationMillis = if (role == CalcButtonRole.EQUALS) 640 else 560,
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

    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkTheme = remember(backgroundColor) { backgroundColor.luminance() < 0.5f }
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary

    val restingContainerColor = when (role) {
        CalcButtonRole.DIGIT -> if (colorTintEnabled) {
            lerp(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                primaryContainerColor,
                if (isDarkTheme) 0.30f else 0.36f
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        CalcButtonRole.OPERATOR -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
        CalcButtonRole.UTILITY -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.primary
        CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.surfaceContainer
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    }
    val pressedContainerColor = when (role) {
        CalcButtonRole.DIGIT -> if (colorTintEnabled) {
            lerp(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                primaryContainerColor,
                if (isDarkTheme) 0.45f else 0.52f
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
        CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.surfaceContainerHighest
        CalcButtonRole.OPERATOR -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        CalcButtonRole.UTILITY -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.primary
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    }

    val restingContentColor = when (role) {
        CalcButtonRole.DIGIT -> MaterialTheme.colorScheme.onSurface
        CalcButtonRole.OPERATOR -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        CalcButtonRole.UTILITY -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        CalcButtonRole.EQUALS -> MaterialTheme.colorScheme.onPrimary
        CalcButtonRole.SCIENTIFIC -> MaterialTheme.colorScheme.onSurfaceVariant
        CalcButtonRole.SCIENTIFIC_ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val pressedContentColor = when (role) {
        CalcButtonRole.OPERATOR -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        CalcButtonRole.UTILITY -> if (colorTintEnabled) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
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

    val isDark = isDarkTheme
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
    val sparkTintColor = when {
        hasGlow -> softGlowColor
        glowColorOverride != null -> glowColorOverride
        role == CalcButtonRole.EQUALS -> primaryColor
        role == CalcButtonRole.UTILITY -> tertiaryColor
        else -> primaryColor
    }
    val sparkAccentColor = MaterialTheme.colorScheme.tertiary

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

    val isSparkVisible = sparkAnim != null && sparkAnim.value > 0.001f

    Box(
        modifier = modifier
            .zIndex(if (isSparkVisible || isPressed) 2f else 0f)
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
                if (hasGlow && glowPulse != null) {
                    Modifier.drawBehind {
                        val pulse = maxOf(glowPulse.value, if (isPressed) 0.70f else 0f)
                        val basePeakAlpha = when {
                            role == CalcButtonRole.EQUALS && isDark -> 0.46f
                            role == CalcButtonRole.EQUALS -> 0.36f
                            isDark -> 0.34f
                            else -> 0.26f
                        }
                        val effectiveRoleScale = if (role == CalcButtonRole.DIGIT || role == CalcButtonRole.OPERATOR) 0.72f else 1f
                        val peakAlpha = (basePeakAlpha * clampedGlowIntensity * effectiveRoleScale +
                            pulse * 0.22f * clampedGlowIntensity).coerceIn(0f, 0.92f)
                        val spreadFactor = (0.72f + 0.28f * clampedGlowIntensity) *
                            (if (effectiveRoleScale < 1f) 0.84f else 1f)
                        val maxSpreadPx = ((10.5f * spreadFactor) + pulse * 5.5f * clampedGlowIntensity).dp.toPx()

                        val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                        val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)
                        val roundness = (clampedCornerPx / minHalf).coerceIn(0f, 1f)
                        val cornerExpandRatio = 0.56f + 0.44f * roundness

                        val steps = 5
                        for (i in steps downTo 1) {
                            val t = i.toFloat() / steps.toFloat()
                            val expand = maxSpreadPx * t
                            val falloff = (1f - t * 0.78f)
                            val layerAlpha = (peakAlpha * 0.44f * falloff * falloff).coerceIn(0f, 1f)
                            val layerCorner = (clampedCornerPx + expand * cornerExpandRatio).coerceAtLeast(0f)
                            drawRoundRect(
                                color = softGlowColor.copy(alpha = layerAlpha),
                                topLeft = Offset(-expand, -expand),
                                size = Size(size.width + expand * 2f, size.height + expand * 2f),
                                cornerRadius = CornerRadius(layerCorner, layerCorner)
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (sparkAnim != null && sparkPath != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            drawPressSparks(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = pressSparkStyle,
                                primaryTint = sparkTintColor,
                                accentTint = sparkAccentColor,
                                isAccentKey = role == CalcButtonRole.EQUALS || role == CalcButtonRole.UTILITY,
                                starPath = sparkPath
                            )
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
                            0.55f to innerTintColor.copy(alpha = 0.26f),
                            1.0f to Color.Transparent
                        )

                        onDrawBehind {
                            val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                            val topSheenAlpha = (((if (role == CalcButtonRole.EQUALS) 0.16f else 0.12f) *
                                clampedGlowIntensity * glowRoleScale) + pulse * 0.12f * clampedGlowIntensity)
                                .coerceIn(0f, 0.55f)
                            drawRect(brush = sheenBrush, alpha = topSheenAlpha)

                            // Smooth edge-free radial press glow (never creates an inner rectangular contour)
                            if (pulse > 0.005f) {
                                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                val rad = (maxOf(size.width, size.height) * 0.65f).coerceAtLeast(1f)
                                val innerAlpha = (pulse * 0.22f * clampedGlowIntensity).coerceIn(0f, 0.45f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.0f to innerTintColor.copy(alpha = innerAlpha),
                                        0.65f to innerTintColor.copy(alpha = innerAlpha * 0.35f),
                                        1.0f to Color.Transparent,
                                        center = center,
                                        radius = rad
                                    ),
                                    center = center,
                                    radius = rad
                                )
                            }
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
    hasGlow: Boolean = false,
    glowIntensity: Float = 1.0f,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    pressSparkStyle: PressSparkStyle = PressSparkStyle.SPARKS
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
    val sparkPath = remember(pressSparksEnabled) {
        if (pressSparksEnabled) Path() else null
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
                            sparkAnim.animateTo(1f, tween(480, easing = GlowFadeEasing))
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
    val tertiaryTint = MaterialTheme.colorScheme.tertiary
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val softGlowColor = remember(primaryTint, tertiaryTint, isActive, isDark) {
        val raw = if (isActive) tertiaryTint else primaryTint
        if (isDark) lerp(raw, Color.White, 0.18f) else raw
    }
    val clampedGlow = glowIntensity.coerceIn(0.25f, 1.75f)

    val clipShape = remember(restingCornerRadius) { RoundedCornerShape(restingCornerRadius) }
    val resolvedStyle = textStyle ?: MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium
    )
    val isSparkVisible = sparkAnim != null && sparkAnim.value > 0.001f

    Box(
        modifier = modifier
            .zIndex(if (isSparkVisible || pressAnim.value > 0.05f) 2f else 0f)
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
                if (hasGlow) {
                    Modifier.drawBehind {
                        val p = pressAnim.value
                        val baseAlpha = (if (isActive) 0.30f else 0.22f) * clampedGlow + p * 0.16f * clampedGlow
                        val maxSpreadPx = ((6.5f * (0.75f + 0.25f * clampedGlow)) + p * 3.5f * clampedGlow).dp.toPx()
                        val maxR = minOf(size.width, size.height) / 2f
                        val baseCorner = restingCornerRadius.toPx().coerceIn(0f, maxR)
                        val steps = 4
                        for (i in steps downTo 1) {
                            val t = i.toFloat() / steps.toFloat()
                            val expand = maxSpreadPx * t
                            val falloff = 1f - t * 0.78f
                            val layerAlpha = (baseAlpha * 0.45f * falloff * falloff).coerceIn(0f, 0.85f)
                            val c = (baseCorner + expand * 0.7f).coerceAtLeast(0f)
                            drawRoundRect(
                                color = softGlowColor.copy(alpha = layerAlpha),
                                topLeft = Offset(-expand, -expand),
                                size = Size(size.width + expand * 2f, size.height + expand * 2f),
                                cornerRadius = CornerRadius(c, c)
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (sparkAnim != null && sparkPath != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            drawPressSparks(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = pressSparkStyle,
                                primaryTint = primaryTint,
                                accentTint = tertiaryTint,
                                isAccentKey = isActive,
                                starPath = sparkPath
                            )
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
                if (hasGlow) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = (0.10f * clampedGlow + p * 0.10f).coerceIn(0f, 0.32f)),
                            0.55f to Color.Transparent
                        )
                    )
                }
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

private fun DrawScope.drawPressSparks(
    progress: Float,
    cx: Float,
    cy: Float,
    style: PressSparkStyle,
    primaryTint: Color,
    accentTint: Color,
    isAccentKey: Boolean,
    starPath: Path
) {
    val ease = 1f - (1f - progress).pow(2.35f)
    val fade = (1f - progress).pow(1.25f)
    val maxDist = (maxOf(size.width, size.height) * if (isAccentKey) 0.92f else 0.78f)
        .coerceAtLeast(34.dp.toPx())
    val goldColor = Color(0xFFFFD86B)
    val hotCyan = lerp(primaryTint, Color.White, 0.38f)
    val warmAccent = lerp(accentTint, Color.White, 0.28f)

    when (style) {
        PressSparkStyle.SPARKS -> {
            // 1. Initial plasma flash & subtle shock ring at tap center
            if (progress < 0.34f) {
                val flashP = (1f - progress / 0.34f).pow(1.5f)
                val flashR = (10.dp.toPx() + maxDist * 0.45f * progress).coerceAtLeast(2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.65f * flashP),
                        0.5f to hotCyan.copy(alpha = 0.34f * flashP),
                        1.0f to Color.Transparent,
                        center = Offset(cx, cy),
                        radius = flashR
                    ),
                    center = Offset(cx, cy),
                    radius = flashR
                )
            }

            // 2. 14 curved comet-tail sparks + secondary embers
            val headBaseR = (if (isAccentKey) 3.4f else 2.9f).dp.toPx() * (1f - 0.45f * progress)
            val tailFactor = (0.28f + 0.18f * (1f - progress))
            val gravityPx = 11.dp.toPx() * progress * progress
            val liftPx = 9.dp.toPx() * progress

            for (i in 0 until SPARK_COUNT) {
                val speed = SPARK_SPEED[i]
                val dist = maxDist * ease * speed
                val curl = SPARK_CURL[i] * 5.5.dp.toPx() * progress * (1f - 0.35f * progress)
                val cosA = SPARK_COS[i]
                val sinA = SPARK_SIN[i]
                // Perpendicular curl (-sinA, cosA) gives dynamic curved plasma trails
                val sx = cx + cosA * dist - sinA * curl
                val sy = cy + sinA * dist + cosA * curl + gravityPx - liftPx

                val tailDist = dist * (1f - tailFactor).coerceAtLeast(0.12f)
                val tailCurl = curl * 0.55f
                val tx = cx + cosA * tailDist - sinA * tailCurl
                val ty = cy + sinA * tailDist + cosA * tailCurl + gravityPx * 0.5f - liftPx * 0.7f

                val col = when (i % 3) {
                    0 -> hotCyan
                    1 -> goldColor
                    else -> warmAccent
                }
                val pFade = fade * (0.76f + 0.24f * abs(((progress * 4f + i * 0.19f) % 1f) * 2f - 1f))
                drawLine(
                    color = col.copy(alpha = (0.86f * pFade).coerceIn(0f, 1f)),
                    start = Offset(tx, ty),
                    end = Offset(sx, sy),
                    strokeWidth = (headBaseR * 0.92f).coerceAtLeast(1.4f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = col.copy(alpha = (0.34f * pFade).coerceIn(0f, 1f)),
                    radius = headBaseR * 2.2f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (0.96f * pFade).coerceIn(0f, 1f)),
                    radius = headBaseR * 0.78f,
                    center = Offset(sx, sy)
                )
                if (i % 2 == 0) {
                    val eDist = dist * 0.62f
                    val ex = cx + cosA * eDist + sinA * curl * 0.6f
                    val ey = cy + sinA * eDist - cosA * curl * 0.6f + gravityPx * 0.6f
                    drawCircle(
                        color = goldColor.copy(alpha = (0.72f * pFade).coerceIn(0f, 1f)),
                        radius = headBaseR * 0.48f,
                        center = Offset(ex, ey)
                    )
                }
            }
        }
        PressSparkStyle.STARLIGHT -> {
            // 1. Expanding celestial shimmer halo
            if (progress < 0.48f) {
                val haloFade = (1f - progress / 0.48f).pow(1.4f)
                val haloR = (8.dp.toPx() + maxDist * 0.55f * ease).coerceAtLeast(2f)
                drawCircle(
                    color = goldColor.copy(alpha = 0.28f * haloFade),
                    radius = haloR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }

            // 2. 10 rotating & twinkling 4-point sparkles + dust orbs
            val baseStarR = (if (isAccentKey) 7.4f else 6.2f).dp.toPx() * (0.72f + 0.45f * (1f - progress))
            for (i in 0 until 10) {
                val idx = (i * SPARK_COUNT) / 10
                val dist = maxDist * 0.92f * ease * (0.58f + 0.42f * ((i % 3) / 2f))
                val sx = cx + SPARK_COS[idx] * dist
                val sy = cy + SPARK_SIN[idx] * dist - (10.dp.toPx() * progress)
                val starColor = when (i % 3) {
                    0 -> Color.White
                    1 -> goldColor
                    else -> hotCyan
                }
                val twinkle = 0.55f + 0.45f * abs(((progress * 2.8f + i * 0.23f) % 1f) * 2f - 1f)
                val alpha = (fade * twinkle).coerceIn(0f, 1f)
                val r = baseStarR * (if (i % 2 == 0) 1.0f else 0.72f) * (0.75f + 0.35f * twinkle)
                val inner = r * 0.28f

                // Smooth rotation without Canvas matrix push/pop
                val rot = (if (i % 2 == 0) 1f else -1f) * (progress * 2.2f + i * 0.4f)
                val rCos = cos(rot)
                val rSin = sin(rot)

                starPath.reset()
                starPath.moveTo(sx - r * rSin, sy - r * rCos)
                starPath.lineTo(sx + inner * rCos - (-inner) * rSin, sy + inner * rSin + (-inner) * rCos)
                starPath.lineTo(sx + r * rCos, sy + r * rSin)
                starPath.lineTo(sx + inner * rCos - inner * rSin, sy + inner * rSin + inner * rCos)
                starPath.lineTo(sx + r * rSin, sy + r * rCos)
                starPath.lineTo(sx - inner * rCos - inner * rSin, sy - inner * rSin + inner * rCos)
                starPath.lineTo(sx - r * rCos, sy - r * rSin)
                starPath.lineTo(sx - inner * rCos - (-inner) * rSin, sy - inner * rSin + (-inner) * rCos)
                starPath.close()

                drawCircle(
                    color = starColor.copy(alpha = (0.34f * alpha).coerceIn(0f, 1f)),
                    radius = r * 1.45f,
                    center = Offset(sx, sy)
                )
                drawPath(
                    path = starPath,
                    color = starColor.copy(alpha = (0.98f * alpha).coerceIn(0f, 1f))
                )
                drawCircle(
                    color = Color.White.copy(alpha = (0.95f * alpha).coerceIn(0f, 1f)),
                    radius = r * 0.24f,
                    center = Offset(sx, sy)
                )

                // Trailing stardust orb
                val dustD = dist * 0.56f
                val dx = cx + SPARK_COS[(idx + 1) % SPARK_COUNT] * dustD
                val dy = cy + SPARK_SIN[(idx + 1) % SPARK_COUNT] * dustD - (6.dp.toPx() * progress)
                drawCircle(
                    color = goldColor.copy(alpha = (0.75f * alpha).coerceIn(0f, 1f)),
                    radius = 1.8.dp.toPx() * (1f - 0.4f * progress),
                    center = Offset(dx, dy)
                )
            }
        }
        PressSparkStyle.FIREWORK -> {
            // Stage 1: Crisp launch shockwave ring
            val ringProgress = (progress / 0.68f).coerceIn(0f, 1f)
            if (ringProgress < 0.99f) {
                val ringAlpha = (1f - ringProgress).pow(1.6f) * 0.56f
                val ringRadius = (8.dp.toPx() + maxDist * 0.85f * (1f - (1f - ringProgress).pow(2f))).coerceAtLeast(2f)
                drawCircle(
                    color = goldColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = (2.6f * (1f - 0.6f * ringProgress)).dp.toPx())
                )
            }

            // Stage 2: 12 willow main shells with gravity droop + 24 cascading sub-embers
            val gravity = 20.dp.toPx() * progress * progress
            val shellR = (if (isAccentKey) 3.4f else 2.8f).dp.toPx() * (1f - 0.42f * progress)
            val bloomStart = 0.20f
            val subEase = if (progress > bloomStart) {
                val subP = ((progress - bloomStart) / (1f - bloomStart)).coerceIn(0f, 1f)
                1f - (1f - subP).pow(1.9f)
            } else {
                0f
            }

            for (i in 0 until FW_COUNT) {
                val d1 = maxDist * 0.98f * ease * (0.78f + 0.22f * (i % 2))
                val sx = cx + FW_COS[i] * d1
                val sy = cy + FW_SIN[i] * d1 + gravity - (7.dp.toPx() * progress)
                val tx = cx + FW_COS[i] * (d1 * 0.72f)
                val ty = cy + FW_SIN[i] * (d1 * 0.72f) + gravity * 0.55f - (5.dp.toPx() * progress)

                val col = when (i % 4) {
                    0 -> hotCyan
                    1 -> warmAccent
                    2 -> goldColor
                    else -> Color.White
                }

                // Glowing willow trail + shell core
                drawLine(
                    color = col.copy(alpha = (0.78f * fade).coerceIn(0f, 1f)),
                    start = Offset(tx, ty),
                    end = Offset(sx, sy),
                    strokeWidth = (shellR * 0.85f).coerceAtLeast(1.2f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = col.copy(alpha = (0.36f * fade).coerceIn(0f, 1f)),
                    radius = shellR * 2.1f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (0.96f * fade).coerceIn(0f, 1f)),
                    radius = shellR * 0.85f,
                    center = Offset(sx, sy)
                )

                // Cascading secondary burst sparks splitting off each shell
                if (subEase > 0.01f) {
                    val subDist = maxDist * 0.34f * subEase
                    val subGravity = 12.dp.toPx() * subEase * subEase
                    val subFade = (fade * (1f - subEase * 0.28f)).coerceIn(0f, 1f)
                    for (branch in 0..1) {
                        val bIdx = i * 2 + branch
                        val bx = sx + FW_SUB_COS[bIdx] * subDist
                        val by = sy + FW_SUB_SIN[bIdx] * subDist + subGravity
                        val subCol = if (branch == 0) goldColor else col
                        drawLine(
                            color = subCol.copy(alpha = (0.68f * subFade).coerceIn(0f, 1f)),
                            start = Offset(sx, sy),
                            end = Offset(bx, by),
                            strokeWidth = (shellR * 0.52f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = (0.90f * subFade).coerceIn(0f, 1f)),
                            radius = shellR * 0.52f,
                            center = Offset(bx, by)
                        )
                    }
                }
            }
        }
    }
}

