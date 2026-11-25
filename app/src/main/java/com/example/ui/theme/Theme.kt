package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = ExpressivePrimaryDark,
    onPrimary = ExpressiveOnPrimaryDark,
    primaryContainer = ExpressivePrimaryContainerDark,
    onPrimaryContainer = ExpressiveOnPrimaryContainerDark,
    secondary = ExpressiveSecondaryDark,
    onSecondary = ExpressiveOnSecondaryDark,
    secondaryContainer = ExpressiveSecondaryContainerDark,
    onSecondaryContainer = ExpressiveOnSecondaryContainerDark,
    tertiary = ExpressiveTertiaryDark,
    onTertiary = ExpressiveOnTertiaryDark,
    tertiaryContainer = ExpressiveTertiaryContainerDark,
    onTertiaryContainer = ExpressiveOnTertiaryContainerDark,
    background = ExpressiveBackgroundDark,
    onBackground = ExpressiveOnBackgroundDark,
    surface = ExpressiveSurfaceDark,
    onSurface = ExpressiveOnSurfaceDark,
    surfaceVariant = ExpressiveSurfaceVariantDark,
    onSurfaceVariant = ExpressiveOnSurfaceVariantDark,
    outline = ExpressiveOutlineDark,
    outlineVariant = ExpressiveOutlineVariantDark,
    surfaceContainerLowest = ExpressiveSurfaceContainerLowestDark,
    surfaceContainerLow = ExpressiveSurfaceContainerLowDark,
    surfaceContainer = ExpressiveSurfaceContainerDark,
    surfaceContainerHigh = ExpressiveSurfaceContainerHighDark,
    surfaceContainerHighest = ExpressiveSurfaceContainerHighestDark,
    error = ExpressiveErrorDark,
    onError = ExpressiveOnErrorDark,
    errorContainer = ExpressiveErrorContainerDark,
    onErrorContainer = ExpressiveOnErrorContainerDark
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = ExpressivePrimaryLight,
    onPrimary = ExpressiveOnPrimaryLight,
    primaryContainer = ExpressivePrimaryContainerLight,
    onPrimaryContainer = ExpressiveOnPrimaryContainerLight,
    secondary = ExpressiveSecondaryLight,
    onSecondary = ExpressiveOnSecondaryLight,
    secondaryContainer = ExpressiveSecondaryContainerLight,
    onSecondaryContainer = ExpressiveOnSecondaryContainerLight,
    tertiary = ExpressiveTertiaryLight,
    onTertiary = ExpressiveOnTertiaryLight,
    tertiaryContainer = ExpressiveTertiaryContainerLight,
    onTertiaryContainer = ExpressiveOnTertiaryContainerLight,
    background = ExpressiveBackgroundLight,
    onBackground = ExpressiveOnBackgroundLight,
    surface = ExpressiveSurfaceLight,
    onSurface = ExpressiveOnSurfaceLight,
    surfaceVariant = ExpressiveSurfaceVariantLight,
    onSurfaceVariant = ExpressiveOnSurfaceVariantLight,
    outline = ExpressiveOutlineLight,
    outlineVariant = ExpressiveOutlineVariantLight,
    surfaceContainerLowest = ExpressiveSurfaceContainerLowestLight,
    surfaceContainerLow = ExpressiveSurfaceContainerLowLight,
    surfaceContainer = ExpressiveSurfaceContainerLight,
    surfaceContainerHigh = ExpressiveSurfaceContainerHighLight,
    surfaceContainerHighest = ExpressiveSurfaceContainerHighestLight,
    error = ExpressiveErrorLight,
    onError = ExpressiveOnErrorLight,
    errorContainer = ExpressiveErrorContainerLight,
    onErrorContainer = ExpressiveOnErrorContainerLight
)

@Composable
fun ExpressiveCalcTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val rawColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                softenDynamicLightScheme(dynamicLightColorScheme(context))
            }
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    val animatedColorScheme = rememberAnimatedColorScheme(rawColorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = CalculatorTypography,
        content = content
    )
}

/**
 * Tones down the stark pure-white glare of Android's default dynamic light palette
 * by gently blending surface tiers with the user's Material You surfaceVariant/primaryContainer hues.
 */
private fun softenDynamicLightScheme(base: ColorScheme): ColorScheme {
    val softBg = lerp(base.background, base.surfaceVariant, 0.36f)
    val softDisplay = lerp(
        lerp(base.surfaceContainer, base.surfaceVariant, 0.34f),
        base.primaryContainer,
        0.10f
    )
    return base.copy(
        background = softBg,
        surface = softBg,
        surfaceBright = lerp(base.surfaceBright, base.surfaceVariant, 0.25f),
        surfaceDim = lerp(base.surfaceDim, base.surfaceVariant, 0.35f),
        surfaceContainerLowest = lerp(base.surfaceContainerLowest, base.surfaceVariant, 0.20f),
        surfaceContainerLow = lerp(base.surfaceContainerLow, base.surfaceVariant, 0.38f),
        surfaceContainer = softDisplay,
        surfaceContainerHigh = lerp(base.surfaceContainerHigh, base.surfaceVariant, 0.42f),
        surfaceContainerHighest = lerp(base.surfaceContainerHighest, base.surfaceVariant, 0.48f)
    )
}

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = label
    )
    return animated
}

@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateThemeColor(target.primary, "primary"),
        onPrimary = animateThemeColor(target.onPrimary, "onPrimary"),
        primaryContainer = animateThemeColor(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animateThemeColor(target.onPrimaryContainer, "onPrimaryContainer"),
        inversePrimary = animateThemeColor(target.inversePrimary, "inversePrimary"),
        secondary = animateThemeColor(target.secondary, "secondary"),
        onSecondary = animateThemeColor(target.onSecondary, "onSecondary"),
        secondaryContainer = animateThemeColor(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animateThemeColor(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animateThemeColor(target.tertiary, "tertiary"),
        onTertiary = animateThemeColor(target.onTertiary, "onTertiary"),
        tertiaryContainer = animateThemeColor(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = animateThemeColor(target.onTertiaryContainer, "onTertiaryContainer"),
        background = animateThemeColor(target.background, "background"),
        onBackground = animateThemeColor(target.onBackground, "onBackground"),
        surface = animateThemeColor(target.surface, "surface"),
        onSurface = animateThemeColor(target.onSurface, "onSurface"),
        surfaceVariant = animateThemeColor(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animateThemeColor(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceTint = animateThemeColor(target.surfaceTint, "surfaceTint"),
        inverseSurface = animateThemeColor(target.inverseSurface, "inverseSurface"),
        inverseOnSurface = animateThemeColor(target.inverseOnSurface, "inverseOnSurface"),
        error = animateThemeColor(target.error, "error"),
        onError = animateThemeColor(target.onError, "onError"),
        errorContainer = animateThemeColor(target.errorContainer, "errorContainer"),
        onErrorContainer = animateThemeColor(target.onErrorContainer, "onErrorContainer"),
        outline = animateThemeColor(target.outline, "outline"),
        outlineVariant = animateThemeColor(target.outlineVariant, "outlineVariant"),
        scrim = animateThemeColor(target.scrim, "scrim"),
        surfaceBright = animateThemeColor(target.surfaceBright, "surfaceBright"),
        surfaceDim = animateThemeColor(target.surfaceDim, "surfaceDim"),
        surfaceContainer = animateThemeColor(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animateThemeColor(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animateThemeColor(target.surfaceContainerHighest, "surfaceContainerHighest"),
        surfaceContainerLow = animateThemeColor(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainerLowest = animateThemeColor(target.surfaceContainerLowest, "surfaceContainerLowest")
    )
}

