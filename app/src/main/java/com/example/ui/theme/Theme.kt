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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.example.data.preferences.DynamicColorSource
import com.example.data.preferences.DynamicColorVibrancy

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
    dynamicColorSource: DynamicColorSource = DynamicColorSource.PRIMARY,
    dynamicColorVibrancy: DynamicColorVibrancy = DynamicColorVibrancy.BALANCED,
    customAccentArgb: Long = DEFAULT_CUSTOM_ACCENT_ARGB,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode == ThemeMode.DARK

    val context = LocalContext.current
    val targetColorScheme = remember(
        darkTheme,
        dynamicColor,
        dynamicColorSource,
        dynamicColorVibrancy,
        customAccentArgb,
        context
    ) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val rawDynamic = if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    softenDynamicLightScheme(dynamicLightColorScheme(context))
                }
                applyDynamicColorTuning(
                    base = rawDynamic,
                    darkTheme = darkTheme,
                    source = dynamicColorSource,
                    vibrancy = dynamicColorVibrancy
                )
            }
            !dynamicColor -> {
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

private fun contrastTextFor(bg: Color): Color {
    return if (bg.luminance() > 0.48f) Color(0xFF14161D) else Color.White
}

private fun boostChroma(color: Color, satDelta: Float, valDelta: Float = 0f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val newSat = (hsv[1] + satDelta).coerceIn(0.05f, 0.94f)
    val newVal = (hsv[2] + valDelta).coerceIn(0.10f, 0.98f)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], newSat, newVal)))
}

private fun applyDynamicColorTuning(
    base: ColorScheme,
    darkTheme: Boolean,
    source: DynamicColorSource,
    vibrancy: DynamicColorVibrancy
): ColorScheme {
    val sourced = when (source) {
        DynamicColorSource.PRIMARY -> base
        DynamicColorSource.SECONDARY -> base.copy(
            primary = base.secondary,
            onPrimary = base.onSecondary,
            primaryContainer = base.secondaryContainer,
            onPrimaryContainer = base.onSecondaryContainer,
            secondary = base.tertiary,
            onSecondary = base.onTertiary,
            secondaryContainer = base.tertiaryContainer,
            onSecondaryContainer = base.onTertiaryContainer,
            tertiary = base.primary,
            onTertiary = base.onPrimary,
            tertiaryContainer = base.primaryContainer,
            onTertiaryContainer = base.onPrimaryContainer
        )
        DynamicColorSource.TERTIARY -> base.copy(
            primary = base.tertiary,
            onPrimary = base.onTertiary,
            primaryContainer = base.tertiaryContainer,
            onPrimaryContainer = base.onTertiaryContainer,
            secondary = base.primary,
            onSecondary = base.onPrimary,
            secondaryContainer = base.primaryContainer,
            onSecondaryContainer = base.onPrimaryContainer,
            tertiary = base.secondary,
            onTertiary = base.onSecondary,
            tertiaryContainer = base.secondaryContainer,
            onTertiaryContainer = base.onSecondaryContainer
        )
    }

    return when (vibrancy) {
        DynamicColorVibrancy.NATURAL -> sourced
        DynamicColorVibrancy.BALANCED -> {
            val secContainer = lerp(sourced.secondaryContainer, sourced.primaryContainer, 0.28f)
            val tertContainer = lerp(sourced.tertiaryContainer, sourced.primaryContainer, 0.16f)
            sourced.copy(
                secondaryContainer = secContainer,
                onSecondaryContainer = contrastTextFor(secContainer),
                tertiaryContainer = tertContainer,
                onTertiaryContainer = contrastTextFor(tertContainer),
                surfaceContainerHigh = lerp(
                    sourced.surfaceContainerHigh,
                    sourced.primaryContainer,
                    if (darkTheme) 0.10f else 0.12f
                )
            )
        }
        DynamicColorVibrancy.VIVID -> {
            val vividPrimary = boostChroma(sourced.primary, if (darkTheme) 0.16f else 0.14f)
            val vividPrimaryContainer = boostChroma(sourced.primaryContainer, 0.20f)
            val vividSecContainer = boostChroma(
                lerp(sourced.secondaryContainer, sourced.primaryContainer, 0.42f),
                0.22f
            )
            val vividTertContainer = boostChroma(
                lerp(sourced.tertiaryContainer, sourced.primaryContainer, 0.25f),
                0.18f
            )
            sourced.copy(
                primary = vividPrimary,
                onPrimary = contrastTextFor(vividPrimary),
                primaryContainer = vividPrimaryContainer,
                onPrimaryContainer = contrastTextFor(vividPrimaryContainer),
                secondaryContainer = vividSecContainer,
                onSecondaryContainer = contrastTextFor(vividSecContainer),
                tertiaryContainer = vividTertContainer,
                onTertiaryContainer = contrastTextFor(vividTertContainer),
                surfaceContainerLow = lerp(sourced.surfaceContainerLow, vividPrimaryContainer, 0.10f),
                surfaceContainer = lerp(sourced.surfaceContainer, vividPrimaryContainer, 0.12f),
                surfaceContainerHigh = lerp(
                    sourced.surfaceContainerHigh,
                    vividPrimaryContainer,
                    if (darkTheme) 0.18f else 0.20f
                ),
                surfaceContainerHighest = lerp(sourced.surfaceContainerHighest, vividPrimaryContainer, 0.20f)
            )
        }
    }
}

fun buildCustomColorScheme(seedColor: Color, darkTheme: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    val primaryHue = hsv[0]
    val sat = hsv[1].coerceIn(0.32f, 0.94f)
    // Harmonious analogous shifts so AC/backspace & operators stay cohesive with the chosen accent
    val secondaryHue = (primaryHue + 12f) % 360f
    val tertiaryHue = (primaryHue - 18f + 360f) % 360f

    return if (darkTheme) {
        val primary = hsvColor(primaryHue, (sat * 0.54f).coerceIn(0.28f, 0.62f), 0.94f)
        val primaryContainer = hsvColor(primaryHue, (sat * 0.80f).coerceIn(0.45f, 0.86f), 0.48f)
        val secondary = hsvColor(secondaryHue, (sat * 0.44f).coerceIn(0.24f, 0.52f), 0.88f)
        val secondaryContainer = hsvColor(secondaryHue, (sat * 0.56f).coerceIn(0.30f, 0.64f), 0.36f)
        val tertiary = hsvColor(tertiaryHue, (sat * 0.50f).coerceIn(0.28f, 0.58f), 0.92f)
        val tertiaryContainer = hsvColor(tertiaryHue, (sat * 0.68f).coerceIn(0.38f, 0.72f), 0.44f)

        darkColorScheme(
            primary = primary,
            onPrimary = contrastTextFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastTextFor(primaryContainer),
            secondary = secondary,
            onSecondary = contrastTextFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastTextFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contrastTextFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastTextFor(tertiaryContainer),
            background = hsvColor(primaryHue, (sat * 0.22f).coerceIn(0.14f, 0.24f), 0.085f),
            onBackground = Color(0xFFEAE8F0),
            surface = hsvColor(primaryHue, (sat * 0.22f).coerceIn(0.14f, 0.24f), 0.085f),
            onSurface = Color(0xFFEAE8F0),
            surfaceVariant = hsvColor(primaryHue, (sat * 0.26f).coerceIn(0.16f, 0.30f), 0.30f),
            onSurfaceVariant = Color(0xFFCACBD6),
            outline = hsvColor(primaryHue, (sat * 0.18f).coerceIn(0.12f, 0.22f), 0.58f),
            outlineVariant = hsvColor(primaryHue, (sat * 0.24f).coerceIn(0.16f, 0.28f), 0.30f),
            surfaceContainerLowest = hsvColor(primaryHue, (sat * 0.24f).coerceIn(0.15f, 0.26f), 0.06f),
            surfaceContainerLow = hsvColor(primaryHue, (sat * 0.24f).coerceIn(0.16f, 0.28f), 0.125f),
            surfaceContainer = hsvColor(primaryHue, (sat * 0.26f).coerceIn(0.18f, 0.30f), 0.16f),
            surfaceContainerHigh = hsvColor(primaryHue, (sat * 0.30f).coerceIn(0.20f, 0.34f), 0.20f),
            surfaceContainerHighest = hsvColor(primaryHue, (sat * 0.34f).coerceIn(0.22f, 0.38f), 0.25f),
            error = ExpressiveErrorDark,
            onError = ExpressiveOnErrorDark,
            errorContainer = ExpressiveErrorContainerDark,
            onErrorContainer = ExpressiveOnErrorContainerDark
        )
    } else {
        val isBrightHue = primaryHue in 42f..182f
        val primaryVal = if (isBrightHue) 0.46f else 0.60f
        val tertiaryVal = if (tertiaryHue in 42f..182f) 0.46f else 0.54f
        val primary = hsvColor(primaryHue, (sat * 0.86f).coerceIn(0.48f, 0.92f), primaryVal)
        val primaryContainer = hsvColor(primaryHue, (sat * 0.30f).coerceIn(0.18f, 0.38f), 0.94f)
        val secondary = hsvColor(secondaryHue, (sat * 0.58f).coerceIn(0.32f, 0.66f), 0.46f)
        val secondaryContainer = hsvColor(secondaryHue, (sat * 0.30f).coerceIn(0.18f, 0.36f), 0.90f)
        val tertiary = hsvColor(tertiaryHue, (sat * 0.66f).coerceIn(0.38f, 0.74f), tertiaryVal)
        val tertiaryContainer = hsvColor(tertiaryHue, (sat * 0.32f).coerceIn(0.20f, 0.40f), 0.92f)

        lightColorScheme(
            primary = primary,
            onPrimary = contrastTextFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastTextFor(primaryContainer),
            secondary = secondary,
            onSecondary = contrastTextFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastTextFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contrastTextFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastTextFor(tertiaryContainer),
            background = hsvColor(primaryHue, (sat * 0.14f).coerceIn(0.08f, 0.16f), 0.935f),
            onBackground = Color(0xFF1A1B22),
            surface = hsvColor(primaryHue, (sat * 0.14f).coerceIn(0.08f, 0.16f), 0.935f),
            onSurface = Color(0xFF1A1B22),
            surfaceVariant = hsvColor(primaryHue, (sat * 0.20f).coerceIn(0.13f, 0.24f), 0.86f),
            onSurfaceVariant = Color(0xFF42434E),
            outline = hsvColor(primaryHue, (sat * 0.16f).coerceIn(0.12f, 0.20f), 0.48f),
            outlineVariant = hsvColor(primaryHue, (sat * 0.20f).coerceIn(0.14f, 0.24f), 0.76f),
            surfaceContainerLowest = hsvColor(primaryHue, (sat * 0.10f).coerceIn(0.06f, 0.12f), 0.97f),
            surfaceContainerLow = hsvColor(primaryHue, (sat * 0.16f).coerceIn(0.10f, 0.18f), 0.905f),
            surfaceContainer = hsvColor(primaryHue, (sat * 0.18f).coerceIn(0.12f, 0.22f), 0.88f),
            surfaceContainerHigh = hsvColor(primaryHue, (sat * 0.22f).coerceIn(0.14f, 0.26f), 0.85f),
            surfaceContainerHighest = hsvColor(primaryHue, (sat * 0.24f).coerceIn(0.16f, 0.28f), 0.82f),
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
