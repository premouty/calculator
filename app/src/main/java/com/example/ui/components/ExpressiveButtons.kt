package com.example.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.example.data.preferences.ExpressiveTouchStyle
import com.example.data.preferences.KeyPressEffect
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

val LocalPlayCalcSound = compositionLocalOf<(CalcSoundType) -> Unit> { {} }
val LocalPerformCalcHaptic = compositionLocalOf<(CalcSoundType) -> Unit> { {} }

private val GlowFadeEasing = CubicBezierEasing(0.18f, 1f, 0.32f, 1f)

internal class GlowPaintCache {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    var lastBlurRadius: Float = -1f
    var lastStrokeBlurRadius: Float = -1f
}

internal fun DrawScope.drawSmoothBacklight(
    cache: GlowPaintCache,
    color: Color,
    alpha: Float,
    expandPx: Float,
    blurRadiusPx: Float,
    cornerRadiusPx: Float,
    cornerExpandRatio: Float = 0.72f
) {
    val clampedAlpha = alpha.coerceIn(0f, 0.92f)
    if (clampedAlpha <= 0.002f) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val safeBlur = blurRadiusPx.coerceAtLeast(1f)
        if (abs(safeBlur - cache.lastBlurRadius) >= 0.75f) {
            cache.paint.maskFilter = BlurMaskFilter(safeBlur, BlurMaskFilter.Blur.NORMAL)
            cache.lastBlurRadius = safeBlur
        }
        cache.paint.color = color.copy(alpha = clampedAlpha).toArgb()
        val c = (cornerRadiusPx + expandPx * cornerExpandRatio).coerceAtLeast(0f)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                -expandPx,
                -expandPx,
                size.width + expandPx,
                size.height + expandPx,
                c,
                c,
                cache.paint
            )
        }
    } else {
        // Ultra-smooth 14-step sub-pixel Gaussian fallback for API 24-27
        val totalSpread = expandPx + blurRadiusPx
        val steps = 14
        for (i in steps downTo 1) {
            val t = i.toFloat() / steps.toFloat()
            val curExpand = totalSpread * t
            val gaussian = kotlin.math.exp(-2.8f * t * t)
            val layerAlpha = (clampedAlpha * 0.16f * gaussian).coerceIn(0f, 1f)
            val c = (cornerRadiusPx + curExpand * cornerExpandRatio).coerceAtLeast(0f)
            drawRoundRect(
                color = color.copy(alpha = layerAlpha),
                topLeft = Offset(-curExpand, -curExpand),
                size = Size(size.width + curExpand * 2f, size.height + curExpand * 2f),
                cornerRadius = CornerRadius(c, c)
            )
        }
    }
}

private val ExpressiveTouchEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

// Precomputed Material 3 Expressive 8-lobe scalloped flower ("Cookie") unit vertices (64-pt ultra-smooth curve)
private const val FLOWER_PTS = 64
private val FLOWER_X = FloatArray(FLOWER_PTS) { i ->
    val theta = (i.toFloat() / FLOWER_PTS.toFloat()) * (2f * PI.toFloat())
    val r = 0.88f + 0.12f * cos(8f * theta)
    r * cos(theta)
}
private val FLOWER_Y = FloatArray(FLOWER_PTS) { i ->
    val theta = (i.toFloat() / FLOWER_PTS.toFloat()) * (2f * PI.toFloat())
    val r = 0.88f + 0.12f * cos(8f * theta)
    r * sin(theta)
}

// Precomputed Material 3 Expressive 4-leaf puffy clover unit vertices
private const val CLOVER_PTS = 56
private val CLOVER_X = FloatArray(CLOVER_PTS) { i ->
    val theta = (i.toFloat() / CLOVER_PTS.toFloat()) * (2f * PI.toFloat())
    val r = 0.78f + 0.22f * cos(4f * theta)
    r * cos(theta)
}
private val CLOVER_Y = FloatArray(CLOVER_PTS) { i ->
    val theta = (i.toFloat() / CLOVER_PTS.toFloat()) * (2f * PI.toFloat())
    val r = 0.78f + 0.22f * cos(4f * theta)
    r * sin(theta)
}

// Precomputed 10 radial directions for Material Expressive perimeter capsule burst
private const val CAPSULE_COUNT = 10
private val CAPSULE_COS = FloatArray(CAPSULE_COUNT) { i ->
    cos((-90f + i * (360f / CAPSULE_COUNT)) * (PI.toFloat() / 180f))
}
private val CAPSULE_SIN = FloatArray(CAPSULE_COUNT) { i ->
    sin((-90f + i * (360f / CAPSULE_COUNT)) * (PI.toFloat() / 180f))
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
    expressiveTouchStyle: ExpressiveTouchStyle = ExpressiveTouchStyle.TONAL_HALO,
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
    val expressiveGlowCache = remember(pressSparksEnabled) {
        if (pressSparksEnabled) GlowPaintCache() else null
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
                                durationMillis = if (role == CalcButtonRole.EQUALS) 560 else 500,
                                easing = ExpressiveTouchEasing
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
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val expressivePrimaryTint = when {
        hasGlow -> softGlowColor
        glowColorOverride != null -> glowColorOverride
        role == CalcButtonRole.EQUALS -> primaryColor
        role == CalcButtonRole.UTILITY -> tertiaryColor
        else -> primaryColor
    }
    val expressiveAccentTint = MaterialTheme.colorScheme.tertiary

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

    val clickIndication = ripple(
        bounded = true,
        color = restingContentColor.copy(alpha = if (pressSparksEnabled) 0.06f else 0.20f)
    )

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
                    val glowPaintCache = remember { GlowPaintCache() }
                    Modifier.drawBehind {
                        val pulse = maxOf(glowPulse.value, if (isPressed) 0.70f else 0f)
                        val basePeakAlpha = when {
                            role == CalcButtonRole.EQUALS && isDark -> 0.56f
                            role == CalcButtonRole.EQUALS -> 0.44f
                            isDark -> 0.42f
                            else -> 0.34f
                        }
                        val effectiveRoleScale = if (role == CalcButtonRole.DIGIT || role == CalcButtonRole.OPERATOR) 0.72f else 1f
                        val glowAlpha = (basePeakAlpha * clampedGlowIntensity * effectiveRoleScale +
                            pulse * 0.24f * clampedGlowIntensity).coerceIn(0f, 0.88f)
                        val spreadFactor = (0.76f + 0.24f * clampedGlowIntensity) *
                            (if (effectiveRoleScale < 1f) 0.86f else 1f)
                        val expandPx = ((1.8f * spreadFactor) + pulse * 1.3f * clampedGlowIntensity).dp.toPx()
                        val blurRadiusPx = ((9.5f * spreadFactor) + pulse * 4.5f * clampedGlowIntensity).dp.toPx()

                        val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                        val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)
                        val roundness = (clampedCornerPx / minHalf).coerceIn(0f, 1f)
                        val cornerExpandRatio = 0.56f + 0.44f * roundness

                        drawSmoothBacklight(
                            cache = glowPaintCache,
                            color = softGlowColor,
                            alpha = glowAlpha,
                            expandPx = expandPx,
                            blurRadiusPx = blurRadiusPx,
                            cornerRadiusPx = clampedCornerPx,
                            cornerExpandRatio = cornerExpandRatio
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (sparkAnim != null && sparkPath != null && expressiveGlowCache != null) {
                    Modifier.drawBehind {
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                            val liveCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)
                            drawExpressiveOuterEffect(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = expressiveTouchStyle,
                                primaryTint = expressivePrimaryTint,
                                secondaryTint = secondaryColor,
                                tertiaryTint = expressiveAccentTint,
                                isDarkTheme = isDark,
                                isAccentKey = role == CalcButtonRole.EQUALS || role == CalcButtonRole.UTILITY,
                                cornerRadiusPx = liveCornerPx,
                                reusablePath = sparkPath,
                                glowCache = expressiveGlowCache
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
                                        0.55f to expressivePrimaryTint.copy(alpha = flashAlpha * 0.72f),
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
            .then(
                if (sparkAnim != null) {
                    Modifier.drawBehind {
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            drawExpressiveInnerEffect(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = expressiveTouchStyle,
                                primaryTint = expressivePrimaryTint,
                                tertiaryTint = expressiveAccentTint,
                                onPrimaryTint = onPrimaryColor,
                                isDarkTheme = isDark,
                                isSolidPrimaryKey = role == CalcButtonRole.EQUALS
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
    hasGlow: Boolean = false,
    glowIntensity: Float = 1.0f,
    keyPressEffect: KeyPressEffect = KeyPressEffect.SPRING,
    pressSparksEnabled: Boolean = false,
    expressiveTouchStyle: ExpressiveTouchStyle = ExpressiveTouchStyle.TONAL_HALO
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
    val expressiveGlowCache = remember(pressSparksEnabled) {
        if (pressSparksEnabled) GlowPaintCache() else null
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
    val secondaryTint = MaterialTheme.colorScheme.secondary
    val tertiaryTint = MaterialTheme.colorScheme.tertiary
    val onPrimaryTint = MaterialTheme.colorScheme.onPrimary
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
                    val fxGlowCache = remember { GlowPaintCache() }
                    Modifier.drawBehind {
                        val p = pressAnim.value
                        val glowAlpha = ((if (isActive) 0.42f else 0.30f) * clampedGlow + p * 0.22f * clampedGlow)
                            .coerceIn(0f, 0.85f)
                        val spreadFactor = 0.78f + 0.22f * clampedGlow
                        val expandPx = (1.2f * spreadFactor + p * 1.0f * clampedGlow).dp.toPx()
                        val blurRadiusPx = (6.8f * spreadFactor + p * 3.4f * clampedGlow).dp.toPx()
                        val maxR = minOf(size.width, size.height) / 2f
                        val baseCorner = restingCornerRadius.toPx().coerceIn(0f, maxR)

                        drawSmoothBacklight(
                            cache = fxGlowCache,
                            color = softGlowColor,
                            alpha = glowAlpha,
                            expandPx = expandPx,
                            blurRadiusPx = blurRadiusPx,
                            cornerRadiusPx = baseCorner,
                            cornerExpandRatio = 0.72f
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (sparkAnim != null && sparkPath != null && expressiveGlowCache != null) {
                    Modifier.drawBehind {
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            val maxR = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                            val restingPx = restingCornerRadius.toPx()
                            val pressedPx = pressedCornerRadius.toPx()
                            val liveCornerPx = (restingPx + (pressedPx - restingPx) * pressAnim.value)
                                .coerceIn(0f, maxR)
                            drawExpressiveOuterEffect(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = expressiveTouchStyle,
                                primaryTint = primaryTint,
                                secondaryTint = secondaryTint,
                                tertiaryTint = tertiaryTint,
                                isDarkTheme = isDark,
                                isAccentKey = isActive,
                                cornerRadiusPx = liveCornerPx,
                                reusablePath = sparkPath,
                                glowCache = expressiveGlowCache
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
            .then(
                if (sparkAnim != null) {
                    Modifier.drawBehind {
                        val sp = sparkAnim.value
                        if (sp > 0.001f && sp < 0.999f) {
                            drawExpressiveInnerEffect(
                                progress = sp,
                                cx = if (pressPosRef[0] >= 0f) pressPosRef[0] else size.width * 0.5f,
                                cy = if (pressPosRef[1] >= 0f) pressPosRef[1] else size.height * 0.5f,
                                style = expressiveTouchStyle,
                                primaryTint = primaryTint,
                                tertiaryTint = tertiaryTint,
                                onPrimaryTint = onPrimaryTint,
                                isDarkTheme = isDark,
                                isSolidPrimaryKey = isActive
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = contentColor.copy(alpha = if (pressSparksEnabled) 0.07f else 0.22f)
                ),
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

private fun DrawScope.drawBlurredGlowPath(
    cache: GlowPaintCache,
    path: Path,
    color: Color,
    alpha: Float,
    blurRadiusPx: Float
) {
    val safeAlpha = alpha.coerceIn(0f, 0.9f)
    if (safeAlpha <= 0.003f) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val safeBlur = blurRadiusPx.coerceAtLeast(1f)
        if (abs(safeBlur - cache.lastBlurRadius) >= 0.75f) {
            cache.paint.maskFilter = BlurMaskFilter(safeBlur, BlurMaskFilter.Blur.NORMAL)
            cache.lastBlurRadius = safeBlur
        }
        cache.paint.color = color.copy(alpha = safeAlpha).toArgb()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), cache.paint)
        }
    } else {
        drawPath(
            path = path,
            color = color.copy(alpha = (safeAlpha * 0.45f).coerceIn(0f, 1f)),
            style = Stroke(width = blurRadiusPx * 0.7f)
        )
    }
}

private fun DrawScope.drawBlurredGlowStrokeRoundRect(
    cache: GlowPaintCache,
    color: Color,
    alpha: Float,
    expandPx: Float,
    cornerRadiusPx: Float,
    strokeWidthPx: Float,
    blurRadiusPx: Float
) {
    val safeAlpha = alpha.coerceIn(0f, 0.9f)
    if (safeAlpha <= 0.003f) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val safeBlur = blurRadiusPx.coerceAtLeast(1f)
        if (abs(safeBlur - cache.lastStrokeBlurRadius) >= 0.75f) {
            cache.strokePaint.maskFilter = BlurMaskFilter(safeBlur, BlurMaskFilter.Blur.NORMAL)
            cache.lastStrokeBlurRadius = safeBlur
        }
        cache.strokePaint.strokeWidth = strokeWidthPx
        cache.strokePaint.color = color.copy(alpha = safeAlpha).toArgb()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                -expandPx,
                -expandPx,
                size.width + expandPx,
                size.height + expandPx,
                cornerRadiusPx,
                cornerRadiusPx,
                cache.strokePaint
            )
        }
    } else {
        drawRoundRect(
            color = color.copy(alpha = (safeAlpha * 0.4f).coerceIn(0f, 1f)),
            topLeft = Offset(-expandPx, -expandPx),
            size = Size(size.width + expandPx * 2f, size.height + expandPx * 2f),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidthPx * 1.6f)
        )
    }
}

/**
 * Pass 1 (drawn behind the keycap surface): Smooth, GPU-feathered Material 3 Expressive shapes,
 * halos, waves, and perimeter capsules that bloom outside the button without obscuring digits.
 */
private fun DrawScope.drawExpressiveOuterEffect(
    progress: Float,
    cx: Float,
    cy: Float,
    style: ExpressiveTouchStyle,
    primaryTint: Color,
    secondaryTint: Color,
    tertiaryTint: Color,
    isDarkTheme: Boolean,
    isAccentKey: Boolean,
    cornerRadiusPx: Float,
    reusablePath: Path,
    glowCache: GlowPaintCache
) {
    val ease = 1f - (1f - progress).pow(2.45f)
    val fade = (1f - progress).pow(1.35f)
    val minDim = minOf(size.width, size.height).coerceAtLeast(24.dp.toPx())
    val maxDim = maxOf(size.width, size.height).coerceAtLeast(minDim)

    val toneA = if (isDarkTheme) lerp(primaryTint, Color.White, 0.30f) else primaryTint
    val toneB = if (isDarkTheme) lerp(tertiaryTint, Color.White, 0.28f) else tertiaryTint
    val toneC = if (isDarkTheme) lerp(secondaryTint, Color.White, 0.25f) else secondaryTint

    // Gently pull center from finger contact toward button center so wide/tall keys frame evenly
    val bx = cx + (size.width * 0.5f - cx) * (0.48f + 0.36f * ease)
    val by = cy + (size.height * 0.5f - cy) * (0.48f + 0.36f * ease)

    when (style) {
        ExpressiveTouchStyle.TONAL_HALO -> {
            // Soft blurred ambient cushion behind the key
            val expand1 = (if (isAccentKey) 14f else 11.5f).dp.toPx() * ease
            val c1 = (cornerRadiusPx + expand1 * 0.85f).coerceAtLeast(0f)
            drawSmoothBacklight(
                cache = glowCache,
                color = toneA,
                alpha = (0.42f * fade).coerceIn(0f, 1f),
                expandPx = expand1 * 0.35f,
                blurRadiusPx = (10f + 8f * ease).dp.toPx(),
                cornerRadiusPx = cornerRadiusPx
            )
            // Primary luminous feathered + crisp Material rounded contour
            val stroke1 = (2.8f - 1.9f * ease).coerceAtLeast(0.75f).dp.toPx()
            drawBlurredGlowStrokeRoundRect(
                cache = glowCache,
                color = toneA,
                alpha = (0.48f * fade).coerceIn(0f, 1f),
                expandPx = expand1,
                cornerRadiusPx = c1,
                strokeWidthPx = stroke1 * 2.2f,
                blurRadiusPx = 5.dp.toPx()
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        toneA.copy(alpha = (0.88f * fade).coerceIn(0f, 1f)),
                        toneB.copy(alpha = (0.82f * fade).coerceIn(0f, 1f)),
                        toneC.copy(alpha = (0.76f * fade).coerceIn(0f, 1f))
                    ),
                    start = Offset(-expand1, -expand1),
                    end = Offset(size.width + expand1, size.height + expand1)
                ),
                topLeft = Offset(-expand1, -expand1),
                size = Size(size.width + expand1 * 2f, size.height + expand1 * 2f),
                cornerRadius = CornerRadius(c1, c1),
                style = Stroke(width = stroke1)
            )
            // Secondary staggered Material contour
            if (progress > 0.14f) {
                val p2 = ((progress - 0.14f) / 0.86f).coerceIn(0f, 1f)
                val e2 = 1f - (1f - p2).pow(2.2f)
                val f2 = (1f - p2).pow(1.4f)
                val expand2 = (if (isAccentKey) 9.0f else 7.2f).dp.toPx() * e2
                val c2 = (cornerRadiusPx + expand2 * 0.85f).coerceAtLeast(0f)
                drawRoundRect(
                    color = toneB.copy(alpha = (0.64f * f2).coerceIn(0f, 1f)),
                    topLeft = Offset(-expand2, -expand2),
                    size = Size(size.width + expand2 * 2f, size.height + expand2 * 2f),
                    cornerRadius = CornerRadius(c2, c2),
                    style = Stroke(width = (1.6f * (1f - 0.5f * e2)).dp.toPx())
                )
            }
        }

        ExpressiveTouchStyle.FLOWER_MORPH -> {
            // Iconic Material 3 Expressive 8-lobe Scalloped Cookie blooming smoothly BEHIND the keycap
            val targetR = maxOf(minDim * 0.72f, maxDim * 0.54f) + 8.dp.toPx()
            val flowerR = targetR * (0.58f + 0.42f * ease)
            val rot = progress * 0.45f
            val rCos = cos(rot)
            val rSin = sin(rot)

            reusablePath.reset()
            for (i in 0 until FLOWER_PTS) {
                val fx = FLOWER_X[i] * flowerR
                val fy = FLOWER_Y[i] * flowerR
                val px = bx + fx * rCos - fy * rSin
                val py = by + fx * rSin + fy * rCos
                if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            reusablePath.close()

            drawBlurredGlowPath(
                cache = glowCache,
                path = reusablePath,
                color = toneA,
                alpha = (0.46f * fade).coerceIn(0f, 1f),
                blurRadiusPx = 11.dp.toPx()
            )
            drawPath(
                path = reusablePath,
                brush = Brush.radialGradient(
                    0.0f to toneA.copy(alpha = (0.34f * fade).coerceIn(0f, 1f)),
                    0.75f to toneB.copy(alpha = (0.26f * fade).coerceIn(0f, 1f)),
                    1.0f to toneA.copy(alpha = (0.14f * fade).coerceIn(0f, 1f)),
                    center = Offset(bx, by),
                    radius = flowerR
                )
            )
            drawPath(
                path = reusablePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        toneA.copy(alpha = (0.86f * fade).coerceIn(0f, 1f)),
                        toneB.copy(alpha = (0.80f * fade).coerceIn(0f, 1f))
                    ),
                    start = Offset(bx - flowerR, by - flowerR),
                    end = Offset(bx + flowerR, by + flowerR)
                ),
                style = Stroke(width = (2.2f - 1.0f * ease).dp.toPx())
            )
        }

        ExpressiveTouchStyle.SPRING_WAVE -> {
            // Outer Gaussian-soft concentric tonal shockwaves framing the key
            val baseR = maxDim * 0.50f
            for (w in 0..1) {
                val delay = w * 0.16f
                if (progress > delay) {
                    val wp = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    val wEase = 1f - (1f - wp).pow(2.3f)
                    val wFade = (1f - wp).pow(1.4f)
                    val ringR = baseR * 0.72f + (baseR * 0.48f + (if (isAccentKey) 18f else 14f).dp.toPx()) * wEase
                    val col = if (w == 0) toneA else toneB
                    val sw = (2.6f - 0.5f * w - 1.4f * wEase).coerceAtLeast(0.75f).dp.toPx()
                    if (w == 0) {
                        drawCircle(
                            color = col.copy(alpha = (0.20f * wFade).coerceIn(0f, 1f)),
                            radius = ringR,
                            center = Offset(bx, by),
                            style = Stroke(width = sw * 3.2f)
                        )
                    }
                    drawCircle(
                        color = col.copy(alpha = ((0.82f - 0.16f * w) * wFade).coerceIn(0f, 1f)),
                        radius = ringR,
                        center = Offset(bx, by),
                        style = Stroke(width = sw)
                    )
                }
            }
        }

        ExpressiveTouchStyle.CAPSULE_BURST -> {
            // 10 sleek Material Expressive rounded pills emerging cleanly past the keycap perimeter
            val lengthBell = sin(progress * PI.toFloat()).coerceIn(0f, 1f)
            val longLen = (4.0f + 11.5f * lengthBell).dp.toPx()
            val shortLen = (3.0f + 8.0f * lengthBell).dp.toPx()
            val primaryW = (4.6f * (1f - 0.48f * progress)).coerceAtLeast(1.6f).dp.toPx()
            val secondaryW = (3.4f * (1f - 0.48f * progress)).coerceAtLeast(1.3f).dp.toPx()
            val halfW = size.width * 0.5f
            val halfH = size.height * 0.5f
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.5f

            for (i in 0 until CAPSULE_COUNT) {
                val cosA = CAPSULE_COS[i]
                val sinA = CAPSULE_SIN[i]
                // Distance from button center to elliptical perimeter along angle so pills shoot FROM edge outward
                val perimeterR = 1f / kotlin.math.hypot(cosA / halfW, sinA / halfH).coerceAtLeast(0.001f)
                val travel = (18f + if (isAccentKey) 5f else 0f).dp.toPx() * ease *
                    (if (i % 2 == 0) 1.0f else 0.78f)
                val dist = perimeterR * 0.86f + travel
                val len = if (i % 2 == 0) longLen else shortLen
                val width = if (i % 2 == 0) primaryW else secondaryW

                val x2 = centerX + cosA * dist
                val y2 = centerY + sinA * dist
                val x1 = x2 - cosA * len
                val y1 = y2 - sinA * len

                val capCol = when (i % 3) {
                    0 -> toneA
                    1 -> toneB
                    else -> toneC
                }

                drawLine(
                    color = capCol.copy(alpha = (0.26f * fade).coerceIn(0f, 1f)),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = width * 2.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = capCol.copy(alpha = (0.92f * fade).coerceIn(0f, 1f)),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }

        ExpressiveTouchStyle.CLOVER_BLOOM -> {
            // Material 3 Expressive 4-leaf puffy Clover + counter-rotated tertiary echo behind keycap
            val targetR = maxOf(minDim * 0.74f, maxDim * 0.56f) + 9.dp.toPx()
            val outerR = targetR * (0.56f + 0.44f * ease)
            val rot = progress * 0.58f
            val rCos = cos(rot)
            val rSin = sin(rot)

            reusablePath.reset()
            for (i in 0 until CLOVER_PTS) {
                val px0 = CLOVER_X[i] * outerR
                val py0 = CLOVER_Y[i] * outerR
                val px = bx + px0 * rCos - py0 * rSin
                val py = by + px0 * rSin + py0 * rCos
                if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            reusablePath.close()

            drawBlurredGlowPath(
                cache = glowCache,
                path = reusablePath,
                color = toneA,
                alpha = (0.44f * fade).coerceIn(0f, 1f),
                blurRadiusPx = 11.dp.toPx()
            )
            drawPath(
                path = reusablePath,
                brush = Brush.radialGradient(
                    0.0f to toneA.copy(alpha = (0.34f * fade).coerceIn(0f, 1f)),
                    0.75f to toneB.copy(alpha = (0.22f * fade).coerceIn(0f, 1f)),
                    1.0f to toneA.copy(alpha = (0.10f * fade).coerceIn(0f, 1f)),
                    center = Offset(bx, by),
                    radius = outerR
                )
            )
            drawPath(
                path = reusablePath,
                color = toneA.copy(alpha = (0.88f * fade).coerceIn(0f, 1f)),
                style = Stroke(width = (2.1f - 0.9f * ease).dp.toPx())
            )

            // Offset 45-deg soft tertiary silhouette for rich dual-layer Material depth
            val echoR = outerR * 0.86f
            val eRot = rot + (PI.toFloat() / 4f) - progress * 0.20f
            val eCos = cos(eRot)
            val eSin = sin(eRot)
            reusablePath.reset()
            for (i in 0 until CLOVER_PTS) {
                val px0 = CLOVER_X[i] * echoR
                val py0 = CLOVER_Y[i] * echoR
                val px = bx + px0 * eCos - py0 * eSin
                val py = by + px0 * eSin + py0 * eCos
                if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            reusablePath.close()
            drawPath(
                path = reusablePath,
                color = toneB.copy(alpha = (0.58f * fade).coerceIn(0f, 1f)),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        ExpressiveTouchStyle.PRISM_SHIMMER -> {
            // Soft outer chromatic prism rim & diffused halo around the keycap
            val rimExpand = (if (isAccentKey) 8.5f else 6.8f).dp.toPx() * ease
            val rimCorner = (cornerRadiusPx + rimExpand * 0.85f).coerceAtLeast(0f)
            val rimStroke = (2.4f - 1.4f * ease).coerceAtLeast(0.8f).dp.toPx()

            drawSmoothBacklight(
                cache = glowCache,
                color = toneB,
                alpha = (0.36f * fade).coerceIn(0f, 1f),
                expandPx = rimExpand * 0.35f,
                blurRadiusPx = (8f + 6f * ease).dp.toPx(),
                cornerRadiusPx = cornerRadiusPx
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        toneA.copy(alpha = (0.90f * fade).coerceIn(0f, 1f)),
                        toneB.copy(alpha = (0.86f * fade).coerceIn(0f, 1f)),
                        toneC.copy(alpha = (0.82f * fade).coerceIn(0f, 1f))
                    ),
                    start = Offset(-rimExpand, 0f),
                    end = Offset(size.width + rimExpand, size.height)
                ),
                topLeft = Offset(-rimExpand, -rimExpand),
                size = Size(size.width + rimExpand * 2f, size.height + rimExpand * 2f),
                cornerRadius = CornerRadius(rimCorner, rimCorner),
                style = Stroke(width = rimStroke)
            )
        }
    }
}

/**
 * Pass 2 (drawn inside the clipped keycap surface, underneath the digit/operator text):
 * Smooth tonal washes, directional ripples, and glass specular sweeps so the keycap
 * responds directly to finger contact without obscuring typography.
 */
private fun DrawScope.drawExpressiveInnerEffect(
    progress: Float,
    cx: Float,
    cy: Float,
    style: ExpressiveTouchStyle,
    primaryTint: Color,
    tertiaryTint: Color,
    onPrimaryTint: Color,
    isDarkTheme: Boolean,
    isSolidPrimaryKey: Boolean
) {
    val ease = 1f - (1f - progress).pow(2.45f)
    val fade = (1f - progress).pow(1.3f)
    val maxR = (maxOf(size.width, size.height) * 0.95f).coerceAtLeast(36.dp.toPx())

    val toneA = if (isDarkTheme) lerp(primaryTint, Color.White, 0.32f) else primaryTint
    val toneB = if (isDarkTheme) lerp(tertiaryTint, Color.White, 0.28f) else tertiaryTint
    val surfaceAccent = if (isSolidPrimaryKey) {
        onPrimaryTint
    } else if (isDarkTheme) {
        lerp(primaryTint, Color.White, 0.56f)
    } else {
        primaryTint
    }

    when (style) {
        ExpressiveTouchStyle.PRISM_SHIMMER -> {
            val span = size.width + size.height
            val sweepCenter = -size.height * 0.35f + (span + size.height * 0.7f) * ease
            val bandHalf = (size.width * 0.44f).coerceAtLeast(26.dp.toPx())
            val sheenAlpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 0.55f +
                (1f - progress) * 0.22f).coerceIn(0f, 0.70f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        toneA.copy(alpha = sheenAlpha * 0.38f),
                        surfaceAccent.copy(alpha = sheenAlpha * 0.72f),
                        toneB.copy(alpha = sheenAlpha * 0.38f),
                        Color.Transparent
                    ),
                    start = Offset(sweepCenter - bandHalf, 0f),
                    end = Offset(sweepCenter + bandHalf, size.height)
                )
            )
        }
        ExpressiveTouchStyle.SPRING_WAVE -> {
            val washR = (10.dp.toPx() + maxR * 1.15f * ease).coerceAtLeast(2f)
            val alpha = (0.38f * fade).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to toneA.copy(alpha = alpha * 0.25f),
                    0.58f to surfaceAccent.copy(alpha = alpha * 0.95f),
                    0.78f to toneB.copy(alpha = alpha * 0.55f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = washR
                ),
                center = Offset(cx, cy),
                radius = washR
            )
        }
        else -> {
            // Unified Material Expressive radial contact highlight beneath the key label
            val bloomR = (10.dp.toPx() + maxR * 0.95f * ease).coerceAtLeast(2f)
            val bloomAlpha = (0.32f * fade).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to surfaceAccent.copy(alpha = bloomAlpha),
                    0.55f to toneB.copy(alpha = bloomAlpha * 0.40f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = bloomR
                ),
                center = Offset(cx, cy),
                radius = bloomR
            )
        }
    }
}



