package com.example.chattaskai.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle

object FontLoader {
    private var lobsterFamily: FontFamily? = null

    fun load(context: Context) {
        try {
            // ONLY load the verified stable Lobster font
            lobsterFamily = FontFamily(
                Font(path = "fonts/lobster_two_regular.ttf", assetManager = context.assets, weight = FontWeight.Normal),
                Font(path = "fonts/lobster_two_italic.ttf", assetManager = context.assets, weight = FontWeight.Normal, style = FontStyle.Italic)
            )
        } catch (e: Exception) {
            lobsterFamily = null
        }
    }

    fun ndot(): FontFamily = FontFamily.Monospace // Use safe system-monospaced for Nothing look
    fun lobster(): FontFamily = lobsterFamily ?: FontFamily.Serif
}
