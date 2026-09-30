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
    primary = UberLightPrimaryAction,
    onPrimary = UberLightOnPrimaryAction,
    background = UberLightBackground,
    onBackground = UberLightTextPrimary,
    surface = UberLightSurface,
    onSurface = UberLightTextPrimary,
    surfaceVariant = UberLightBorderDefault,
    onSurfaceVariant = UberLightTextSecondary,
    secondary = UberLightAccentBlue,
    onSecondary = UberLightOnPrimaryAction,
    error = UberLightError,
    onError = UberLightOnPrimaryAction,
    outline = UberLightBorderDefault,
    outlineVariant = UberLightDivider,
    scrim = UberLightTextPrimary,
    primaryContainer = UberLightSurface,
    onPrimaryContainer = UberLightTextPrimary,
    secondaryContainer = UberLightBackground,
    onSecondaryContainer = UberLightTextPrimary,
    errorContainer = UberLightBackground,
    onErrorContainer = UberLightError,
    surfaceTint = UberLightPrimaryAction,
    tertiary = UberLightAccentBlue,
    onTertiary = UberLightOnPrimaryAction,
    tertiaryContainer = UberLightSurface,
    onTertiaryContainer = UberLightTextPrimary,
    inversePrimary = UberLightOnPrimaryAction,
    inverseSurface = UberLightTextPrimary,
    inverseOnSurface = UberLightBackground,
    surfaceDim = UberLightBackground,
    surfaceBright = UberLightSurface,
    surfaceContainerLowest = UberLightBackground,
    surfaceContainerLow = UberLightSurface,
    surfaceContainer = UberLightSurface,
    surfaceContainerHigh = UberLightBorderDefault,
    surfaceContainerHighest = UberLightBorderDefault
)

private val darkScheme = darkColorScheme(
    primary = UberDarkPrimaryAction,
    onPrimary = UberDarkOnPrimaryAction,
    background = UberDarkBackground,
    onBackground = UberDarkTextPrimary,
    surface = UberDarkSurface,
    onSurface = UberDarkTextPrimary,
    surfaceVariant = UberDarkSurfaceElevated,
    onSurfaceVariant = UberDarkTextSecondary,
    secondary = UberDarkAccentBlue,
    onSecondary = UberDarkPrimaryAction,
    error = UberDarkError,
    onError = UberDarkOnPrimaryAction,
    outline = UberDarkBorderDefault,
    outlineVariant = UberDarkDivider,
    scrim = UberDarkTextPrimary,
    primaryContainer = UberDarkSurfaceElevated,
    onPrimaryContainer = UberDarkTextPrimary,
    secondaryContainer = UberDarkBackground,
    onSecondaryContainer = UberDarkTextPrimary,
    errorContainer = UberDarkBackground,
    onErrorContainer = UberDarkError,
    surfaceTint = UberDarkPrimaryAction,
    tertiary = UberDarkAccentBlue,
    onTertiary = UberDarkOnPrimaryAction,
    tertiaryContainer = UberDarkSurfaceElevated,
    onTertiaryContainer = UberDarkTextPrimary,
    inversePrimary = UberDarkOnPrimaryAction,
    inverseSurface = UberDarkTextPrimary,
    inverseOnSurface = UberDarkBackground,
    surfaceDim = UberDarkBackground,
    surfaceBright = UberDarkSurface,
    surfaceContainerLowest = UberDarkBackground,
    surfaceContainerLow = UberDarkSurface,
    surfaceContainer = UberDarkSurfaceElevated,
    surfaceContainerHigh = UberDarkBorderDefault,
    surfaceContainerHighest = UberDarkBorderDefault
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