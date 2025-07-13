package com.hillal.acc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.TextUnit
import com.hillal.acc.R

// ألوان المشروع
private val LightColors = lightColorScheme(
    primary = Color(0xFF2196F3), // أزرق فاتح وخفيف
    onPrimary = Color.White,
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF222222),
    surface = Color.White,
    onSurface = Color(0xFF222222),
    error = Color(0xFFE53E3E),
    onError = Color.White,
    // Custom success colors
    // These are not part of the default ColorScheme, so we will add them as extensions
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
    onError = Color(0xFF23262F),
    // Custom success colors
    // These are not part of the default ColorScheme, so we will add them as extensions
)

// Custom Success Colors
val SuccessLight = Color(0xFF22C55E) // Green
val SuccessContainerLight = Color(0xFFD1FADF) // Light green background
val SuccessDark = Color(0xFF22C55E) // Green
val SuccessContainerDark = Color(0xFF00522E) // Dark green background

// Extensions to access custom colors from MaterialTheme.colorScheme
val ColorScheme.success: Color
    get() = if (isLight) SuccessLight else SuccessDark

val ColorScheme.successContainer: Color
    get() = if (isLight) SuccessContainerLight else SuccessContainerDark

private val ColorScheme.isLight: Boolean
    get() = this.background.luminance() > 0.5f

// الخطوط
val CairoFontFamily = FontFamily(
    Font(R.font.cairo_regular_res, FontWeight.Normal),
    Font(R.font.cairo_bold_res, FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

// الأشكال
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// أبعاد ومسافات مرنة (يمكنك تعديلها حسب الحاجة)
data class AppDimensions(
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val cardCorner: Dp,
    val iconSize: Dp,
    val buttonHeight: Dp = 48.dp,
    val buttonCorner: Dp = 16.dp,
    val successIconSize: Dp = 48.dp,
    val whatsappIconSize: Dp = 32.dp,
    val smsIconSize: Dp = 28.dp,
    val fieldHorizontalPadding: Dp = 2.dp, // جديد
    val cardHeight: Dp = 120.dp, // جديد
    val bodyFont: TextUnit = 14.sp, // جديد
    val statFont: TextUnit = 18.sp, // جديد
    val statLabelFont: TextUnit = 13.sp, // جديد
    // --- إضافات جمالية ---
    val logoSize: Dp,           // حجم الشعار العلوي
    val fieldHeight: Dp,        // ارتفاع حقول الإدخال
    val cardElevation: Dp,      // ظل البطاقات
    val iconSizeSmall: Dp,      // أيقونات صغيرة (مثل أيقونة كلمة السر)
    val fontSmall: TextUnit     // حجم خط صغير (روابط، نصوص مساعدة)
)

// ألوان جمالية إضافية (يمكن استخدامها في الخلفيات أو التدرجات أو العناصر الثانوية)
val Gradient1 = Color(0xFF4F8DFD) // أزرق متدرج عصري
val Gradient2 = Color(0xFF6FE7DD) // أخضر-تركوازي متدرج عصري
val Accent = Color(0xFFFFC542)    // أصفر عصري للأزرار أو الإشعارات
val BackgroundVariant = Color(0xFFF6F8FB) // خلفية ثانوية فاتحة

// يمكن إضافة هذه الألوان إلى MaterialTheme.colorScheme عبر امتدادات:
val ColorScheme.gradient1: Color get() = Gradient1
val ColorScheme.gradient2: Color get() = Gradient2
val ColorScheme.accent: Color get() = Accent
val ColorScheme.backgroundVariant: Color get() = BackgroundVariant

@Composable
fun calculateAppDimensions(): AppDimensions {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val cardCorner = screenWidth * 0.04f
    return AppDimensions(
        spacingSmall = screenWidth * 0.02f,   // 2% من العرض
        spacingMedium = screenWidth * 0.04f,  // 4% من العرض
        spacingLarge = screenWidth * 0.08f,   // 8% من العرض
        cardCorner = cardCorner,              // 4% من العرض
        iconSize = screenWidth * 0.06f,       // 6% من العرض
        buttonHeight = screenHeight * 0.055f, // زر متجاوب
        buttonCorner = cardCorner,
        successIconSize = screenWidth * 0.13f,    // 13% من العرض
        whatsappIconSize = screenWidth * 0.09f,   // 9% من العرض
        smsIconSize = screenWidth * 0.08f,        // 8% من العرض
        fieldHorizontalPadding = screenWidth * 0.006f, // 0.6% من العرض
        cardHeight = screenHeight * 0.15f, // 15% من الارتفاع
        bodyFont = (screenWidth.value * 0.045f).sp, // ديناميكي
        statFont = (screenWidth.value * 0.048f).sp, // ديناميكي
        statLabelFont = (screenWidth.value * 0.035f).sp, // ديناميكي
        // --- إضافات جمالية ---
        logoSize = screenWidth * 0.18f, // شعار كبير نسبيًا
        fieldHeight = screenHeight * 0.081f, // ارتفاع حقل الإدخال متجاوب
        cardElevation = screenWidth * 0.012f, // ظل البطاقة متجاوب (مثلاً 4-8dp)
        iconSizeSmall = screenWidth * 0.042f, // أيقونة صغيرة (مثلاً 14-18dp)
        fontSmall = (screenWidth.value * 0.032f).sp // خط صغير (مثلاً 11-13sp)
    )
}

val LocalAppDimensions = androidx.compose.runtime.staticCompositionLocalOf {
    AppDimensions(
        8.dp, 16.dp, 24.dp, 16.dp, 24.dp, 48.dp, 16.dp, 48.dp, 32.dp, 28.dp, 2.dp,
        120.dp, 14.sp, 18.sp, 13.sp,
        // إضافات جمالية افتراضية
        64.dp, 48.dp, 4.dp, 16.dp, 12.sp
    )
}

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    val dimensions = calculateAppDimensions()
    CompositionLocalProvider(LocalAppDimensions provides dimensions) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
} 