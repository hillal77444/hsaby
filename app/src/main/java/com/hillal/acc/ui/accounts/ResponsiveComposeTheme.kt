package com.hillal.acc.ui.accounts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun ResponsiveAccountsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // تحديد حجم الشاشة
    val screenSize = when {
        screenWidth < 600.dp -> ScreenSize.Small
        screenWidth < 840.dp -> ScreenSize.Medium
        else -> ScreenSize.Large
    }
    
    // تخصيص الألوان حسب حجم الشاشة
    val colorScheme = when (screenSize) {
        ScreenSize.Small -> lightColorScheme(
            primary = Color(0xFF152FD9),
            secondary = Color(0xFF1976D2),
            surface = Color(0xFFF8F9FA),
            onSurface = Color(0xFF212529),
            onPrimary = Color.White,
            onSecondary = Color.White
        )
        ScreenSize.Medium -> lightColorScheme(
            primary = Color(0xFF152FD9),
            secondary = Color(0xFF1976D2),
            surface = Color(0xFFF8F9FA),
            onSurface = Color(0xFF212529),
            onPrimary = Color.White,
            onSecondary = Color.White
        )
        ScreenSize.Large -> lightColorScheme(
            primary = Color(0xFF152FD9),
            secondary = Color(0xFF1976D2),
            surface = Color(0xFFF8F9FA),
            onSurface = Color(0xFF212529),
            onPrimary = Color.White,
            onSecondary = Color.White
        )
    }
    
    // تخصيص Typography حسب حجم الشاشة
    val typography = when (screenSize) {
        ScreenSize.Small -> Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontSize = MaterialTheme.typography.headlineLarge.fontSize * 0.8f
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.8f
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.8f
            ),
            titleMedium = MaterialTheme.typography.titleMedium.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.8f
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.8f
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.8f
            ),
            bodySmall = MaterialTheme.typography.bodySmall.copy(
                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8f
            )
        )
        ScreenSize.Medium -> Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontSize = MaterialTheme.typography.headlineLarge.fontSize * 0.9f
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.9f
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.9f
            ),
            titleMedium = MaterialTheme.typography.titleMedium.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.9f
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.9f
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.9f
            ),
            bodySmall = MaterialTheme.typography.bodySmall.copy(
                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
            )
        )
        ScreenSize.Large -> MaterialTheme.typography
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

enum class ScreenSize {
    Small, Medium, Large
}

@Composable
fun ResponsiveSpacing(): ResponsiveSpacingValues {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < 600.dp -> ResponsiveSpacingValues(
            small = 4.dp,
            medium = 8.dp,
            large = 16.dp,
            xl = 24.dp,
            xxl = 32.dp
        )
        screenWidth < 840.dp -> ResponsiveSpacingValues(
            small = 6.dp,
            medium = 12.dp,
            large = 20.dp,
            xl = 28.dp,
            xxl = 36.dp
        )
        else -> ResponsiveSpacingValues(
            small = 8.dp,
            medium = 16.dp,
            large = 24.dp,
            xl = 32.dp,
            xxl = 40.dp
        )
    }
}

data class ResponsiveSpacingValues(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
    val xl: androidx.compose.ui.unit.Dp,
    val xxl: androidx.compose.ui.unit.Dp
)

@Composable
fun ResponsivePadding(): ResponsivePaddingValues {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < 600.dp -> ResponsivePaddingValues(
            small = 8.dp,
            medium = 12.dp,
            large = 16.dp,
            xl = 20.dp
        )
        screenWidth < 840.dp -> ResponsivePaddingValues(
            small = 12.dp,
            medium = 16.dp,
            large = 20.dp,
            xl = 24.dp
        )
        else -> ResponsivePaddingValues(
            small = 16.dp,
            medium = 20.dp,
            large = 24.dp,
            xl = 28.dp
        )
    }
}

data class ResponsivePaddingValues(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
    val xl: androidx.compose.ui.unit.Dp
) 