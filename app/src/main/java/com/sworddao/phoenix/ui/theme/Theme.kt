package com.sworddao.phoenix.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PhoenixLightColorScheme = lightColorScheme(
    primary = PhoenixLightPrimary,
    onPrimary = PhoenixLightOnPrimary,
    primaryContainer = PhoenixLightPrimaryContainer,
    onPrimaryContainer = PhoenixLightOnPrimaryContainer,
    secondary = PhoenixLightSecondary,
    onSecondary = PhoenixLightOnSecondary,
    secondaryContainer = PhoenixLightSecondaryContainer,
    onSecondaryContainer = PhoenixLightOnSecondaryContainer,
    tertiary = PhoenixLightTertiary,
    onTertiary = PhoenixLightOnTertiary,
    tertiaryContainer = PhoenixLightTertiaryContainer,
    onTertiaryContainer = PhoenixLightOnTertiaryContainer,
    background = PhoenixLightBackground,
    onBackground = PhoenixLightOnBackground,
    surface = PhoenixLightSurface,
    onSurface = PhoenixLightOnSurface,
    surfaceVariant = PhoenixLightSurfaceVariant,
    onSurfaceVariant = PhoenixLightOnSurfaceVariant
)

private val PhoenixDarkColorScheme = darkColorScheme(
    primary = PhoenixDarkPrimary,
    onPrimary = PhoenixDarkOnPrimary,
    primaryContainer = PhoenixDarkPrimaryContainer,
    onPrimaryContainer = PhoenixDarkOnPrimaryContainer,
    secondary = PhoenixDarkSecondary,
    onSecondary = PhoenixDarkOnSecondary,
    secondaryContainer = PhoenixDarkSecondaryContainer,
    onSecondaryContainer = PhoenixDarkOnSecondaryContainer,
    tertiary = PhoenixDarkTertiary,
    onTertiary = PhoenixDarkOnTertiary,
    tertiaryContainer = PhoenixDarkTertiaryContainer,
    onTertiaryContainer = PhoenixDarkOnTertiaryContainer,
    background = PhoenixDarkBackground,
    onBackground = PhoenixDarkOnBackground,
    surface = PhoenixDarkSurface,
    onSurface = PhoenixDarkOnSurface,
    surfaceVariant = PhoenixDarkSurfaceVariant,
    onSurfaceVariant = PhoenixDarkOnSurfaceVariant
)

@Composable
fun PhoenixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PhoenixDarkColorScheme
        else -> PhoenixLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PhoenixTypography,
        content = content
    )
}
