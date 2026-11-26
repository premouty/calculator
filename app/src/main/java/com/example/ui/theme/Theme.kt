package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat

enum class ThemeMode {
    DARK,
    LIGHT
}

const val DEFAULT_CUSTOM_ACCENT_ARGB: Long = 0xFF3E5AA9L

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
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    customAccentArgb: Long = DEFAULT_CUSTOM_ACCENT_ARGB,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode == ThemeMode.DARK

    val context = LocalContext.current
    val targetColorScheme = remember(darkTheme, dynamicColor, customAccentArgb, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    softenDynamicLightScheme(dynamicLightColorScheme(context))
                }
            }
            !dynamicColor && customAccentArgb != DEFAULT_CUSTOM_ACCENT_ARGB -> {
                buildCustomColorScheme(
                    seedColor = Color(customAccentArgb.toInt()),
                    darkTheme = darkTheme
                )
            }
            darkTheme -> ExpressiveDarkColorScheme
            else -> ExpressiveLightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.setBackgroundDrawable(targetColorScheme.background.toArgb().toDrawable())
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = targetColorScheme,
        typography = CalculatorTypography,
        content = content
    )
}

private fun hsvColor(hue: Float, sat: Float, value: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(h, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        )
    )
}

fun buildCustomColorScheme(seedColor: Color, darkTheme: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    val primaryHue = hsv[0]
    val sat = hsv[1].coerceIn(0.26f, 0.92f)
    val secondaryHue = (primaryHue + 22f) % 360f
    val tertiaryHue = (primaryHue + 62f) % 360f

    return if (darkTheme) {
        darkColorScheme(
            primary = hsvColor(primaryHue, (sat * 0.46f).coerceIn(0.24f, 0.50f), 0.96f),
            onPrimary = hsvColor(primaryHue, (sat * 0.85f).coerceAtLeast(0.55f), 0.24f),
            primaryContainer = hsvColor(primaryHue, (sat * 0.78f).coerceAtLeast(0.45f), 0.48f),
            onPrimaryContainer = hsvColor(primaryHue, 0.20f, 0.98f),
            secondary = hsvColor(secondaryHue, (sat * 0.34f).coerceIn(0.18f, 0.38f), 0.88f),
            onSecondary = hsvColor(secondaryHue, 0.52f, 0.22f),
            secondaryContainer = hsvColor(secondaryHue, (sat * 0.44f).coerceIn(0.24f, 0.48f), 0.36f),
            onSecondaryContainer = hsvColor(secondaryHue, 0.18f, 0.94f),
            tertiary = hsvColor(tertiaryHue, (sat * 0.44f).coerceIn(0.25f, 0.50f), 0.95f),
            onTertiary = hsvColor(tertiaryHue, 0.65f, 0.24f),
            tertiaryContainer = hsvColor(tertiaryHue, (sat * 0.62f).coerceIn(0.36f, 0.65f), 0.44f),
            onTertiaryContainer = hsvColor(tertiaryHue, 0.20f, 0.96f),
            background = hsvColor(primaryHue, 0.14f, 0.085f),
            onBackground = Color(0xFFE6E4EC),
            surface = hsvColor(primaryHue, 0.14f, 0.085f),
            onSurface = Color(0xFFE6E4EC),
            surfaceVariant = hsvColor(primaryHue, 0.16f, 0.30f),
            onSurfaceVariant = Color(0xFFC5C6D0),
            outline = hsvColor(primaryHue, 0.12f, 0.58f),
            outlineVariant = hsvColor(primaryHue, 0.16f, 0.30f),
            surfaceContainerLowest = hsvColor(primaryHue, 0.16f, 0.06f),
            surfaceContainerLow = hsvColor(primaryHue, 0.14f, 0.12f),
            surfaceContainer = hsvColor(primaryHue, 0.14f, 0.15f),
            surfaceContainerHigh = hsvColor(primaryHue, 0.14f, 0.19f),
            surfaceContainerHighest = hsvColor(primaryHue, 0.15f, 0.24f),
            error = ExpressiveErrorDark,
            onError = ExpressiveOnErrorDark,
            errorContainer = ExpressiveErrorContainerDark,
            onErrorContainer = ExpressiveOnErrorContainerDark
        )
    } else {
        val isBrightHue = primaryHue in 42f..178f
        val primaryVal = if (isBrightHue) 0.48f else 0.64f
        val tertiaryVal = if (tertiaryHue in 42f..178f) 0.46f else 0.54f
        lightColorScheme(
            primary = hsvColor(primaryHue, (sat * 0.85f).coerceIn(0.48f, 0.92f), primaryVal),
            onPrimary = Color.White,
            primaryContainer = hsvColor(primaryHue, (sat * 0.24f).coerceIn(0.14f, 0.30f), 0.96f),
            onPrimaryContainer = hsvColor(primaryHue, 0.88f, 0.24f),
            secondary = hsvColor(secondaryHue, (sat * 0.48f).coerceIn(0.24f, 0.55f), 0.46f),
            onSecondary = Color.White,
            secondaryContainer = hsvColor(secondaryHue, (sat * 0.24f).coerceIn(0.14f, 0.28f), 0.93f),
            onSecondaryContainer = hsvColor(secondaryHue, 0.65f, 0.22f),
            tertiary = hsvColor(tertiaryHue, (sat * 0.62f).coerceIn(0.34f, 0.72f), tertiaryVal),
            onTertiary = Color.White,
            tertiaryContainer = hsvColor(tertiaryHue, (sat * 0.24f).coerceIn(0.16f, 0.30f), 0.95f),
            onTertiaryContainer = hsvColor(tertiaryHue, 0.72f, 0.24f),
            background = hsvColor(primaryHue, 0.08f, 0.93f),
            onBackground = Color(0xFF1A1B22),
            surface = hsvColor(primaryHue, 0.08f, 0.93f),
            onSurface = Color(0xFF1A1B22),
            surfaceVariant = hsvColor(primaryHue, 0.13f, 0.86f),
            onSurfaceVariant = Color(0xFF44454F),
            outline = hsvColor(primaryHue, 0.12f, 0.48f),
            outlineVariant = hsvColor(primaryHue, 0.14f, 0.76f),
            surfaceContainerLowest = hsvColor(primaryHue, 0.05f, 0.97f),
            surfaceContainerLow = hsvColor(primaryHue, 0.09f, 0.90f),
            surfaceContainer = hsvColor(primaryHue, 0.10f, 0.88f),
            surfaceContainerHigh = hsvColor(primaryHue, 0.11f, 0.85f),
            surfaceContainerHighest = hsvColor(primaryHue, 0.12f, 0.82f),
            error = ExpressiveErrorLight,
            onError = ExpressiveOnErrorLight,
            errorContainer = ExpressiveErrorContainerLight,
            onErrorContainer = ExpressiveOnErrorContainerLight
        )
    }
}

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
