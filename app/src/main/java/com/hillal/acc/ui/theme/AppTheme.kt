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
    val buttonHeight: Dp = 42.dp,
    val buttonCorner: Dp = 14.dp,
    val successIconSize: Dp = 44.dp,
    val whatsappIconSize: Dp = 32.dp,
    val smsIconSize: Dp = 24.dp, // يجب أن يكون معرف مرة واحدة فقط
    val fieldHorizontalPadding: Dp = 2.dp, // جديد
    val cardHeight: Dp = 120.dp, // جديد
    val bodyFont: TextUnit = 13.sp, // جديد
    val statFont: TextUnit = 15.sp, // جديد
    val statLabelFont: TextUnit = 10.sp, // جديد
    // --- إضافات جمالية ---
    val logoSize: Dp,           // حجم الشعار العلوي
    val fieldHeight: Dp,        // ارتفاع حقول الإدخال
    val cardElevation: Dp,      // ظل البطاقات
    val iconSizeSmall: Dp,      // أيقونات صغيرة (مثل أيقونة كلمة السر)
    val fontSmall: TextUnit,     // حجم خط صغير (روابط، نصوص مساعدة)
    val spacingTiny: Dp = 4.dp, // جديد
    val iconSpacing: Dp = 8.dp, // جديد
    val cardWidthRatio: Float = 0.97f, // جديد
    val logoScaleIn: Float = 0.7f, // جديد
    val logoContentScale: Float = 0.8f, // جديد
    val buttonSpacing: Dp = 8.dp, // جديد
    val errorCardVerticalPadding: Dp = 2.dp, // جديد
    val textFieldHeight: Dp = 56.dp, // جديد
    val rowSpacing: Dp = 8.dp, // جديد
    val menuIconSize: Dp = 24.dp, // جديد
    val cardMaxHeight: Dp = 160.dp, // جديد
    val cardElevationSmall: Dp = 2.dp, // جديد
    val dialogCorner: Dp = 16.dp, // جديد
    val dialogPadding: Dp = 24.dp, // جديد
    val dialogIconSize: Dp = 48.dp, // جديد
    val dividerPadding: Dp = 8.dp, // جديد
    val minDialogHeight: Dp = 120.dp, // جديد
    val radioButtonSize: Dp = 24.dp // جديد
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
    val base = if (screenWidth < screenHeight) screenWidth else screenHeight
    val cardCorner = screenWidth * 0.04f

    val fieldHeight = (screenHeight.value * 0.08f).coerceIn(48f, 64f).dp
    val bodyFont = (screenWidth.value * 0.045f).coerceIn(14f, 18f).sp
    val fontSmall = (screenWidth.value * 0.03f).coerceIn(11f, 14f).sp
    val statFont = (screenWidth.value * 0.048f).coerceIn(14f, 18f).sp
    val statLabelFont = (screenWidth.value * 0.035f).coerceIn(10f, 13f).sp

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
        smsIconSize = base * 0.07f,
        fieldHorizontalPadding = screenWidth * 0.006f, // 0.6% من العرض
        cardHeight = screenHeight * 0.15f, // 15% من الارتفاع
        bodyFont = bodyFont, // ديناميكي
        statFont = statFont, // ديناميكي
        statLabelFont = statLabelFont, // ديناميكي
        // --- إضافات جمالية ---
        logoSize = screenWidth * 0.18f, // شعار كبير نسبيًا
        fieldHeight = fieldHeight, // ارتفاع حقل الإدخال متجاوب
        cardElevation = screenWidth * 0.012f, // ظل البطاقة متجاوب (مثلاً 4-8dp)
        iconSizeSmall = screenWidth * 0.042f, // أيقونة صغيرة (مثلاً 14-18dp)
        fontSmall = fontSmall, // خط صغير (مثلاً 11-13sp)
        spacingTiny = screenWidth * 0.008f, // 0.8% من العرض
        iconSpacing = screenWidth * 0.012f, // 1.2% من العرض
        cardWidthRatio = 0.97f, // نفس النسبة المستخدمة في fillMaxWidth
        logoScaleIn = 0.7f, // نفس النسبة المستخدمة في scaleIn
        logoContentScale = 0.8f, // نفس النسبة المستخدمة في logo size
        buttonSpacing = screenWidth * 0.02f, // 2% من العرض
        errorCardVerticalPadding = screenHeight * 0.003f, // 0.3% من الارتفاع
        textFieldHeight = (screenHeight * 0.09f).coerceAtLeast(54.dp),
        rowSpacing = base * 0.018f,
        menuIconSize = base * 0.055f,
        cardMaxHeight = (screenHeight * 0.18f).coerceAtMost(160.dp),
        cardElevationSmall = base * 0.004f,
        dialogCorner = base * 0.025f,
        dialogPadding = base * 0.045f,
        dialogIconSize = base * 0.11f,
        dividerPadding = base * 0.015f,
        minDialogHeight = (screenHeight * 0.22f).coerceAtMost(180.dp),
        radioButtonSize = base * 0.045f
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