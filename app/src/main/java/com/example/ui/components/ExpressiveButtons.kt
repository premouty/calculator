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
import kotlinx.coroutines.launch

val LocalPlayCalcSound = compositionLocalOf<(CalcSoundType) -> Unit> { {} }
val LocalPerformCalcHaptic = compositionLocalOf<(CalcSoundType) -> Unit> { {} }

private val GlowFadeEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

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

    // Immediate touch-down audio, vibration, and silky glow pulse
    LaunchedEffect(interactionSource, soundType, glowPulse) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
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
        val rawGlowColor = when (role) {
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
        remember(role, isDark, primaryContainerColor, tertiaryColor) {
            when (role) {
                CalcButtonRole.EQUALS -> lerp(primaryContainerColor, Color.White, if (isDark) 0.35f else 0.55f)
                CalcButtonRole.UTILITY -> lerp(tertiaryColor, Color.White, if (isDark) 0.40f else 0.65f)
                else -> Color.White
            }
        }
    } else {
        Color.Transparent
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
            }
            .then(
                if (hasGlow && glowPulse != null) {
                    Modifier.drawBehind {
                        val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                        val basePeakAlpha = when {
                            role == CalcButtonRole.EQUALS && isDark -> 0.40f
                            role == CalcButtonRole.EQUALS -> 0.34f
                            isDark -> 0.28f
                            else -> 0.23f
                        }
                        val peakAlpha = basePeakAlpha + pulse * 0.20f
                        val maxSpreadPx = (9.dp + (pulse * 5.5f).dp).toPx()

                        val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                        val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)
                        val roundness = (clampedCornerPx / minHalf).coerceIn(0f, 1f)
                        // Pill keeps exact capsule offset (1.0x expand); Squircle & Sharp hug their tighter corners
                        val cornerExpandRatio = 0.56f + 0.44f * roundness

                        val steps = 7
                        for (i in steps downTo 1) {
                            val t = i.toFloat() / steps.toFloat()
                            val expand = maxSpreadPx * t
                            val falloff = (1f - t * 0.88f)
                            val layerAlpha = (peakAlpha * 0.42f * falloff * falloff).coerceIn(0f, 1f)
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
            .clip(dynamicShape)
            .drawBehind {
                val mix = pressMixState.value
                val bg = if (mix <= 0.001f || restingContainerColor == pressedContainerColor) {
                    restingContainerColor
                } else {
                    lerp(restingContainerColor, pressedContainerColor, mix)
                }
                drawRect(bg)
            }
            .then(
                if (hasGlow && glowPulse != null) {
                    Modifier.drawBehind {
                        val pulse = maxOf(glowPulse.value, if (isPressed) 0.65f else 0f)
                        val minHalf = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
                        val clampedCornerPx = cornerRadiusState.value.toPx().coerceIn(0f, minHalf)

                        // Gentle top-down atmospheric inner sheen
                        val topSheenAlpha = (if (role == CalcButtonRole.EQUALS) 0.18f else 0.14f) + pulse * 0.14f
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to innerTintColor.copy(alpha = topSheenAlpha),
                                0.55f to innerTintColor.copy(alpha = topSheenAlpha * 0.28f),
                                1.0f to Color.Transparent
                            )
                        )

                        // Shape-matched centered inner luminescence (adapts accurately to Pill, Squircle & Sharp)
                        val innerPulseAlpha = (if (role == CalcButtonRole.EQUALS) 0.09f else 0.06f) + pulse * 0.13f
                        val inset1 = 4.dp.toPx()
                        if (size.width > inset1 * 2f && size.height > inset1 * 2f) {
                            val innerCorner1 = (clampedCornerPx - inset1 * 0.85f).coerceAtLeast(0f)
                            drawRoundRect(
                                color = innerTintColor.copy(alpha = innerPulseAlpha * 0.55f),
                                topLeft = Offset(inset1, inset1),
                                size = Size(size.width - inset1 * 2f, size.height - inset1 * 2f),
                                cornerRadius = CornerRadius(innerCorner1, innerCorner1)
                            )
                        }
                        val inset2 = 8.dp.toPx()
                        if (size.width > inset2 * 2f && size.height > inset2 * 2f) {
                            val innerCorner2 = (clampedCornerPx - inset2 * 0.85f).coerceAtLeast(0f)
                            drawRoundRect(
                                color = innerTintColor.copy(alpha = innerPulseAlpha * 0.45f),
                                topLeft = Offset(inset2, inset2),
                                size = Size(size.width - inset2 * 2f, size.height - inset2 * 2f),
                                cornerRadius = CornerRadius(innerCorner2, innerCorner2)
                            )
                        }

                        // Delicate luminous rim highlight matching the button shape
                        val strokePx = 1.25.dp.toPx()
                        val rimTopAlpha = (if (role == CalcButtonRole.EQUALS) 0.32f else 0.25f) + pulse * 0.22f
                        val rimBottomAlpha = (if (role == CalcButtonRole.EQUALS) 0.08f else 0.06f) + pulse * 0.10f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to innerTintColor.copy(alpha = rimTopAlpha),
                                1.0f to softGlowColor.copy(alpha = rimBottomAlpha)
                            ),
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(
                                (size.width - strokePx).coerceAtLeast(0f),
                                (size.height - strokePx).coerceAtLeast(0f)
                            ),
                            cornerRadius = CornerRadius(
                                (clampedCornerPx - strokePx / 2f).coerceAtLeast(0f),
                                (clampedCornerPx - strokePx / 2f).coerceAtLeast(0f)
                            ),
                            style = Stroke(width = strokePx)
                        )
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
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = if (contentScaleState != null) {
                    Modifier.graphicsLayer {
                        val s = contentScaleState.value
                        scaleX = s
                        scaleY = s
                    }
                } else {
                    Modifier
                },
                contentAlignment = Alignment.Center
            ) {
                if (content != null) {
                    content()
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
}
