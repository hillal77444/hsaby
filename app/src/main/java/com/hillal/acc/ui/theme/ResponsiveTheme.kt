package com.hillal.acc.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// تعريف القيم النسبية
data class ResponsiveDimensions(
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val cardHeight: Dp,
    val cardCorner: Dp,
    val titleFont: TextUnit,
    val bodyFont: TextUnit,
    val statFont: TextUnit,
    val statLabelFont: TextUnit,
    val iconSize: Dp
)

val defaultResponsiveDimensions = ResponsiveDimensions(
    spacingSmall = 8.dp,
    spacingMedium = 16.dp,
    spacingLarge = 24.dp,
    cardHeight = 120.dp,
    cardCorner = 16.dp,
    titleFont = 22.sp,
    bodyFont = 14.sp,
    statFont = 18.sp,
    statLabelFont = 13.sp,
    iconSize = 24.dp
)

val LocalResponsiveDimensions = staticCompositionLocalOf { defaultResponsiveDimensions }

@Composable
fun ProvideResponsiveDimensions(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val dimensions = ResponsiveDimensions(
        spacingSmall = screenWidth * 0.02f,
        spacingMedium = screenWidth * 0.04f,
        spacingLarge = screenWidth * 0.08f,
        cardHeight = screenHeight * 0.15f,
        cardCorner = screenWidth * 0.04f,
        titleFont = (screenWidth.value * 0.06f).sp,
        bodyFont = (screenWidth.value * 0.045f).sp,
        statFont = (screenWidth.value * 0.048f).sp,
        statLabelFont = (screenWidth.value * 0.035f).sp,
        iconSize = screenWidth * 0.06f
    )
    CompositionLocalProvider(LocalResponsiveDimensions provides dimensions, content = content)
} 