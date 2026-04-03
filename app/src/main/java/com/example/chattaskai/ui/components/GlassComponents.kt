package com.example.chattaskai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chattaskai.ui.theme.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

// Gradients are now handled dynamically inside components

/**
 * A modifier that applies an animated "Breathing" gradient.
 */
@Composable
fun Modifier.liquidGradient(
    colors: List<Color>,
    durationMillis: Int = 5000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    return this.background(
        Brush.linearGradient(
            colors = colors,
            start = Offset(offset, offset),
            end = Offset(offset + 500f, offset + 500f)
        )
    )
}

/**
 * A highly premium Glassmorphism modifier.
 * Applies blur, a glass-white background, and a subtle gradient border.
 */
fun Modifier.glassMorphism(
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.07f, // Default to 7% for subtle translucency
    baseColor: Color = Color.White // Support for Dark/Black glass
) = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(baseColor.copy(alpha = alpha))
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.03f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * An animated, "Liquid" background that creates a mesmerizing, premium feel.
 */
@Composable
fun LiquidBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    
    val xOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    
    val yOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )

    val liquidColors = LocalLiquidColors.current
    
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(
                color = liquidColors.purple.copy(alpha = 0.4f),
                radius = 600f,
                center = Offset(xOffset1, yOffset1)
            )
            drawCircle(
                color = liquidColors.cyan.copy(alpha = 0.3f),
                radius = 500f,
                center = Offset(size.width - xOffset1 * 0.8f, size.height - yOffset1 * 1.2f)
            )
            drawCircle(
                color = liquidColors.pink.copy(alpha = 0.2f),
                radius = 700f,
                center = Offset(xOffset1 * 1.2f, size.height - yOffset1)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when(priority.lowercase()) {
        "high" -> com.example.chattaskai.ui.theme.UrgentPriority
        "medium" -> com.example.chattaskai.ui.theme.MediumPriority
        else -> com.example.chattaskai.ui.theme.LowPriority
    }
    
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        androidx.compose.material3.Text(
            text = priority.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
    }
}
