package com.example.chattaskai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = Color(0xFF18DCFF),
    background = BackgroundDark,
    surface = BackgroundDark,
    onPrimary = BackgroundDark,
    onBackground = TextDark,
    onSurface = TextDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueGreen,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onBackground = TextLight,
    onSurface = TextLight
)

val LocalLiquidColors = compositionLocalOf { LiquidColorsDark }

@Composable
fun TasklineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeHue: Float = 0f, // Acting as Theme ID
    typography: Typography = Typography, 
    content: @Composable () -> Unit
) {
    val liquidColors = remember(themeHue) {
        getLiquidColorsById(themeHue.toInt())
    }

    val colorScheme = when {
        darkTheme -> DarkColorScheme.copy(secondary = liquidColors.cyan)
        else -> LightColorScheme.copy(secondary = liquidColors.cyan)
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalLiquidColors provides liquidColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
