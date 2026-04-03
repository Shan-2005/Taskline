package com.example.chattaskai.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryBlueGreen = Color(0xFF009688) 
val SecondaryTeal = Color(0xFF006064)
val BackgroundLight = Color(0xFFF0F2F5)
val SurfaceLight = Color(0xFFFFFFFF)
val TextLight = Color(0xFF1C1C1E)

val BackgroundDark = Color(0xFF000000) 
val SurfaceDark = Color(0xFF000000)
val TextDark = Color(0xFFF5F5F7) 
val PrimaryDark = Color(0xFFBB86FC)

val UrgentPriority = Color(0xFFFF3B30) 
val MediumPriority = Color(0xFFFF9500) 
val LowPriority = Color(0xFF34C759) 

data class LiquidColors(
    val cyan: Color,
    val purple: Color,
    val pink: Color
)

val PurpleVoid = LiquidColors(
    cyan = Color(0xFFFF00DE), // Electric Pink
    purple = Color(0xFF7D5FFF), // Deep Purple
    pink = Color(0xFFFF5252) // Crimson
)

val MidnightBlue = LiquidColors(
    cyan = Color(0xFF18DCFF), 
    purple = Color(0xFF1E3799), 
    pink = Color(0xFF82CCDD) 
)

val DeepForest = LiquidColors(
    cyan = Color(0xFF00B894), 
    purple = Color(0xFF006266), 
    pink = Color(0xFF55E6C1) 
)

val LiquidColorsDark = PurpleVoid

fun getLiquidColorsById(id: Int): LiquidColors {
    return when(id) {
        0 -> PurpleVoid
        1 -> MidnightBlue
        2 -> DeepForest
        else -> LiquidColorsDark
    }
}

val GlassWhite = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)

val PremiumCyanGradient = listOf(Color(0xFF18DCFF), Color(0xFF009688))
val PremiumPurpleGradient = listOf(Color(0xFF7D5FFF), Color(0xFF512DA8))
val PremiumPinkGradient = listOf(Color(0xFFFF7597), Color(0xFFC2185B))
