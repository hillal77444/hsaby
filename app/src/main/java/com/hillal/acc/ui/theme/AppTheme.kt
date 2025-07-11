package com.hillal.acc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// ألوان المشروع
private val LightColors = lightColorScheme(
    primary = Color(0xFF152FD9),
    onPrimary = Color.White,
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF222222),
    surface = Color.White,
    onSurface = Color(0xFF222222),
    error = Color(0xFFE53E3E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90A4FF),
    onPrimary = Color(0xFF222222),
    secondary = Color(0xFF22C55E),
    onSecondary = Color(0xFF222222),
    background = Color(0xFF181A20),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF23262F),
    onSurface = Color(0xFFF3F4F6),
    error = Color(0xFFE57373),
    onError = Color(0xFF23262F)
)

// الخطوط
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

// الأشكال
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// أبعاد ومسافات مرنة (يمكنك تعديلها حسب الحاجة)
data class AppDimensions(
    val spacingSmall: Int = 8,
    val spacingMedium: Int = 16,
    val spacingLarge: Int = 24,
    val cardCorner: Int = 16,
    val iconSize: Int = 24
)

val LocalAppDimensions = androidx.compose.runtime.staticCompositionLocalOf { AppDimensions() }

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    val dimensions = AppDimensions()
    CompositionLocalProvider(LocalAppDimensions provides dimensions) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
} 