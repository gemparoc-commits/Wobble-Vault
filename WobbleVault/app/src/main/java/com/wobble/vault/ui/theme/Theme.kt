package com.wobble.vault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandRedBright,
    onPrimary = Color.White,
    primaryContainer = BrandRedDark,
    onPrimaryContainer = Color(0xFFFFDBD6),
    secondary = Color(0xFF8FB69D),
    onSecondary = Color.White,
    secondaryContainer = BrandGreen,
    onSecondaryContainer = Color(0xFFDCEFE4),
    tertiary = Color(0xFFD9A465),
    onTertiary = Color.White,
    background = BrandBoneDark,
    onBackground = BrandOnDark,
    surface = BrandSurfaceDark,
    onSurface = BrandOnDark,
    surfaceVariant = Color(0xFF332F2B),
    onSurfaceVariant = BrandOnDarkMuted,
    outline = BrandLineDark,
    outlineVariant = BrandLineDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRedSoft,
    onPrimaryContainer = BrandRedDark,
    secondary = BrandGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EEE7),
    onSecondaryContainer = BrandGreen,
    tertiary = BrandAmber,
    onTertiary = Color.White,
    background = BrandBone,
    onBackground = BrandInk,
    surface = BrandPaper,
    onSurface = BrandInk,
    surfaceVariant = Color(0xFFEEE9E1),
    onSurfaceVariant = BrandMuted,
    outline = BrandLine,
    outlineVariant = BrandLine
)

@Composable
fun WobbleVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
