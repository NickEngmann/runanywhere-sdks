package com.runanywhere.runanywherewatch

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Adaptive screen configuration for watch vs phone layouts.
 * Watch (Wear OS): ~192-227dp circular viewport
 * Phone: 360dp+ rectangular viewport
 */
data class ScreenConfig(
    val isWatch: Boolean,
    val screenWidth: Dp,
    val screenHeight: Dp,
    // Time display
    val timeFontSize: TextUnit,
    val dateFontSize: TextUnit,
    val secondsFontSize: TextUnit,
    // Buttons
    val primaryButtonSize: Dp,
    val secondaryButtonSize: Dp,
    val iconButtonSize: Dp,
    // Spacing
    val edgePadding: Dp,
    val itemSpacing: Dp,
    // Text
    val bodyFontSize: TextUnit,
    val captionFontSize: TextUnit,
    val headerFontSize: TextUnit,
    // Status indicators
    val statusDotSize: Dp,
    // Camera
    val cameraPreviewSize: Dp,
    val captureButtonSize: Dp
)

val LocalScreenConfig = compositionLocalOf { phoneConfig(360.dp, 800.dp) }

fun watchConfig(width: Dp, height: Dp) = ScreenConfig(
    isWatch = true,
    screenWidth = width,
    screenHeight = height,
    timeFontSize = 34.sp,
    dateFontSize = 11.sp,
    secondsFontSize = 14.sp,
    primaryButtonSize = 36.dp,
    secondaryButtonSize = 28.dp,
    iconButtonSize = 24.dp,
    edgePadding = 8.dp,
    itemSpacing = 4.dp,
    bodyFontSize = 11.sp,
    captionFontSize = 9.sp,
    headerFontSize = 13.sp,
    statusDotSize = 8.dp,
    cameraPreviewSize = 120.dp,
    captureButtonSize = 44.dp
)

fun phoneConfig(width: Dp, height: Dp) = ScreenConfig(
    isWatch = false,
    screenWidth = width,
    screenHeight = height,
    timeFontSize = 64.sp,
    dateFontSize = 18.sp,
    secondsFontSize = 22.sp,
    primaryButtonSize = 64.dp,
    secondaryButtonSize = 48.dp,
    iconButtonSize = 40.dp,
    edgePadding = 16.dp,
    itemSpacing = 8.dp,
    bodyFontSize = 14.sp,
    captionFontSize = 11.sp,
    headerFontSize = 18.sp,
    statusDotSize = 14.dp,
    cameraPreviewSize = 280.dp,
    captureButtonSize = 72.dp
)

@Composable
fun AdaptiveLayout(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val config = if (maxWidth < 230.dp) {
            watchConfig(maxWidth, maxHeight)
        } else {
            phoneConfig(maxWidth, maxHeight)
        }
        CompositionLocalProvider(LocalScreenConfig provides config) {
            content()
        }
    }
}
