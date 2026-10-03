package com.example.multilink.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val lightScheme = lightColorScheme(
    primary = LightPrimaryAction,
    onPrimary = LightOnPrimaryAction,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    secondary = LightAccentBlue,
    onSecondary = LightOnPrimaryAction,
    error = LightError,
    onError = LightOnPrimaryAction,
    outline = LightBorderDefault,
    outlineVariant = LightDivider,
    scrim = LightTextPrimary,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightTextPrimary,
    secondaryContainer = LightBackground,
    onSecondaryContainer = LightTextPrimary,
    errorContainer = LightBackground,
    onErrorContainer = LightError,
    surfaceTint = LightPrimaryAction,
    tertiary = LightAccentBlue,
    onTertiary = LightOnPrimaryAction,
    tertiaryContainer = LightSurfaceElevated,
    onTertiaryContainer = LightTextPrimary,
    inversePrimary = LightOnPrimaryAction,
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBackground,
    surfaceDim = LightBackground,
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightBorderDefault,
    surfaceContainerHighest = LightBorderDefault
)

private val darkScheme = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkOnPrimaryAction,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    secondary = DarkAccentBlue,
    onSecondary = DarkPrimaryAction,
    error = DarkError,
    onError = DarkOnPrimaryAction,
    outline = DarkBorderDefault,
    outlineVariant = DarkDivider,
    scrim = DarkTextPrimary,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondaryContainer = DarkBackground,
    onSecondaryContainer = DarkTextPrimary,
    errorContainer = DarkBackground,
    onErrorContainer = DarkError,
    surfaceTint = DarkPrimaryAction,
    tertiary = DarkAccentBlue,
    onTertiary = DarkOnPrimaryAction,
    tertiaryContainer = DarkSurfaceElevated,
    onTertiaryContainer = DarkTextPrimary,
    inversePrimary = DarkOnPrimaryAction,
    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBackground,
    surfaceDim = DarkBackground,
    surfaceBright = DarkSurface,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkBorderDefault,
    surfaceContainerHighest = DarkBorderDefault
)

@Composable
fun MultiLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}