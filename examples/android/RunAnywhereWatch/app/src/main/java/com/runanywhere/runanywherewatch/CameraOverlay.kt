package com.runanywhere.runanywherewatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * CameraOverlay composable that provides:
 * - Circular camera preview
 * - Photo capture with flash animation
 * - Photo display with mic button overlay
 * - Quick "What am I looking at?" button
 */
@Composable
fun CameraOverlay(
    onCapturePhoto: (File) -> Unit,
    onAskAboutPhoto: (File) -> Unit,
    onCloseCamera: () -> Unit,
    isShowing: Boolean
) {
    if (!isShowing) return

    var flashOpacity by remember { mutableStateOf(0f) }
    var capturedPhoto by remember { mutableStateOf<File?>(null) }
    var showPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(showPhoto) {
        if (showPhoto) {
            flashOpacity = 1f
            kotlinx.coroutines.delay(100)
            flashOpacity = 0f
        }
    }

    AdaptiveLayout {
        val cfg = LocalScreenConfig.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera preview
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(cfg.cameraPreviewSize)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(
                            if (cfg.isWatch) 1.dp else 2.dp,
                            Color(0xFF00E5FF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera",
                        fontSize = cfg.bodyFontSize,
                        color = Color.White
                    )
                }

                if (flashOpacity > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashOpacity))
                    )
                }
            }

            // Close button — top right
            IconButton(
                onClick = onCloseCamera,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (cfg.isWatch) 20.dp else 16.dp,
                        end = if (cfg.isWatch) 20.dp else 16.dp
                    )
                    .size(if (cfg.isWatch) 28.dp else 40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            ) {
                Text(
                    text = "X",
                    fontSize = if (cfg.isWatch) 14.sp else 20.sp,
                    color = Color.White
                )
            }

            // Viewfinder controls
            if (!showPhoto) {
                // Quick Ask — bottom left (or hidden on small watches)
                if (!cfg.isWatch) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = cfg.edgePadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quick Ask",
                            fontSize = cfg.captionFontSize,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(cfg.itemSpacing))
                        Button(
                            onClick = {
                                val photoFile = File("/tmp/quick_ask_${System.currentTimeMillis()}.jpg")
                                capturedPhoto = photoFile
                                showPhoto = true
                                onCapturePhoto(photoFile)
                                onAskAboutPhoto(photoFile)
                            },
                            modifier = Modifier.size(cfg.secondaryButtonSize),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text(text = "?", fontSize = cfg.headerFontSize)
                        }
                    }
                }

                // Capture button — bottom center
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (cfg.isWatch) 16.dp else 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // On watch: Quick Ask is a small button beside capture
                    if (cfg.isWatch) {
                        Button(
                            onClick = {
                                val photoFile = File("/tmp/quick_ask_${System.currentTimeMillis()}.jpg")
                                capturedPhoto = photoFile
                                showPhoto = true
                                onCapturePhoto(photoFile)
                                onAskAboutPhoto(photoFile)
                            },
                            modifier = Modifier.size(cfg.secondaryButtonSize),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "?", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = {
                            val photoFile = File("/tmp/photo_${System.currentTimeMillis()}.jpg")
                            capturedPhoto = photoFile
                            showPhoto = true
                            onCapturePhoto(photoFile)
                        },
                        modifier = Modifier
                            .size(cfg.captureButtonSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                if (cfg.isWatch) 2.dp else 4.dp,
                                Color(0xFF00E5FF),
                                CircleShape
                            )
                    ) {
                        Text(text = "", fontSize = 1.sp)
                    }
                }
            }

            // Photo review overlay
            if (showPhoto && capturedPhoto != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                ) {
                    // Photo preview
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (cfg.isWatch) 80.dp else 180.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A))
                                .border(
                                    if (cfg.isWatch) 1.dp else 2.dp,
                                    Color(0xFF00E5FF),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Photo",
                                fontSize = cfg.bodyFontSize,
                                color = Color.White
                            )
                        }
                    }

                    // Label
                    Text(
                        text = "Ask about this photo",
                        fontSize = cfg.bodyFontSize,
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (cfg.isWatch) 24.dp else 80.dp)
                    )

                    // Mic button
                    IconButton(
                        onClick = {
                            capturedPhoto?.let { onAskAboutPhoto(it) }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (cfg.isWatch) 16.dp else 32.dp)
                            .size(cfg.captureButtonSize)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                            .border(
                                if (cfg.isWatch) 2.dp else 4.dp,
                                Color.White,
                                CircleShape
                            )
                    ) {
                        Text(
                            text = "MIC",
                            fontSize = cfg.captionFontSize,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * CameraManager - handles camera state, photo capture, and vision query construction.
 * Pure JVM implementation for testing without Android dependencies.
 */
class CameraManager {
    var state: CameraState = CameraState.NOT_INITIALIZED
    var lastPhotoUri: String? = null
    var hasPermission: Boolean = false
    private val photoHistory = mutableListOf<String>()
    private val maxPhotos = 3
    
    fun initializeCamera(): Boolean {
        if (state == CameraState.READY) return false
        if (!requestPermissions()) return false
        state = CameraState.READY
        return true
    }
    
    fun capturePhoto(): Boolean {
        if (state != CameraState.READY) return false
        state = CameraState.CAPTURING
        lastPhotoUri = "file:///camera/photo_${System.currentTimeMillis()}.jpg"
        photoHistory.add(lastPhotoUri!!)
        if (photoHistory.size > maxPhotos) {
            photoHistory.removeAt(0)
        }
        return true
    }
    
    fun resizeImage(width: Int, height: Int): ImageSize {
        val targetSize = 512
        if (width <= targetSize && height <= targetSize) {
            return ImageSize(width, height)
        }
        val scale = minOf(targetSize.toFloat() / width, targetSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        val maxSize = maxOf(newWidth, newHeight)
        return ImageSize(maxSize, maxSize)
    }
    
    fun constructVisionQuery(prompt: String?): String? {
        if (lastPhotoUri == null) return null
        val actualPrompt = prompt ?: "Describe this image"
        return "Image: $lastPhotoUri\nQuery: $actualPrompt"
    }
    
    fun requestPermissions(): Boolean {
        hasPermission = true
        return true
    }
    
    fun clearPhotoUri() {
        lastPhotoUri = null
        state = CameraState.READY
    }
    
    fun getRecentPhotos(): List<String> {
        return photoHistory.toList()
    }
}

enum class CameraState {
    NOT_INITIALIZED,
    READY,
    CAPTURING
}

data class ImageSize(val width: Int, val height: Int)
