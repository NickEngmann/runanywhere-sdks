package com.runanywhere.runanywherewatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    
    // Handle flash animation on capture
    LaunchedEffect(showPhoto) {
        if (showPhoto) {
            // Flash animation
            flashOpacity = 1f
            kotlinx.coroutines.delay(100)
            flashOpacity = 0f
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Circular camera preview (simulated with placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simulated camera preview - in real app would use CameraX
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
            ) {
                Text(
                    text = "📷",
                    fontSize = 64.sp
                )
            }
            
            // Flash overlay
            if (flashOpacity > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashOpacity))
                )
            }
        }
        
        // Close button (top-right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onCloseCamera,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            ) {
                Text(
                    text = "✕",
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
        
        // Quick "What am I looking at?" button (center-left)
        if (!showPhoto) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quick Ask",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Simulate capture and auto-ask
                        val photoFile = File("/tmp/quick_ask_${System.currentTimeMillis()}.jpg")
                        capturedPhoto = photoFile
                        showPhoto = true
                        onCapturePhoto(photoFile)
                        onAskAboutPhoto(photoFile)
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 24.sp
                    )
                }
            }
        }
        
        // Capture button (bottom-center)
        if (!showPhoto) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val photoFile = File("/tmp/photo_${System.currentTimeMillis()}.jpg")
                        capturedPhoto = photoFile
                        showPhoto = true
                        onCapturePhoto(photoFile)
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color(0xFF00E5FF), CircleShape)
                )
            }
        }
        
        // Photo display with mic button overlay
        if (showPhoto && capturedPhoto != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                // Display captured photo (simulated)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    ) {
                        Text(
                            text = "🖼️",
                            fontSize = 64.sp
                        )
                    }
                }
                
                // "Ask about this" prompt
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ask about this photo",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                
                // Mic button overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            capturedPhoto?.let { onAskAboutPhoto(it) }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                            .border(4.dp, Color.White, CircleShape)
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * CameraManager - handles camera state, photo capture, and vision query construction
 * Pure JVM implementation for testing without Android dependencies
 */
class CameraManager {
    var state: CameraState = CameraState.NOT_INITIALIZED
    var lastPhotoUri: String? = null
    var hasPermission: Boolean = false
    private val photoHistory = mutableListOf<String>()
    private val maxPhotos = 3
    
    /**
     * Initialize camera - check permissions and prepare for capture
     */
    fun initializeCamera(): Boolean {
        if (state == CameraState.READY) return false
        if (!requestPermissions()) return false
        state = CameraState.READY
        return true
    }
    
    /**
     * Capture a photo and save to cache
     */
    fun capturePhoto(): Boolean {
        if (state != CameraState.READY) return false
        state = CameraState.CAPTURING
        lastPhotoUri = "file:///camera/photo_${System.currentTimeMillis()}.jpg"
        
        // Add to history (keep last 3 photos)
        photoHistory.add(lastPhotoUri!!)
        if (photoHistory.size > maxPhotos) {
            photoHistory.removeAt(0)
        }
        
        return true
    }
    
    /**
     * Resize image to 512x512 for VLM model
     */
    fun resizeImage(width: Int, height: Int): ImageSize {
        val targetSize = 512
        // If already smaller than target, return as-is
        if (width <= targetSize && height <= targetSize) {
            return ImageSize(width, height)
        }
        // Calculate scale to fit within target size
        val scale = minOf(targetSize.toFloat() / width, targetSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        // Square the result
        val maxSize = maxOf(newWidth, newHeight)
        return ImageSize(maxSize, maxSize)
    }
    
    /**
     * Construct vision query for multimodal VLM
     */
    fun constructVisionQuery(prompt: String?): String? {
        if (lastPhotoUri == null) return null
        val actualPrompt = prompt ?: "Describe this image"
        return "Image: $lastPhotoUri\nQuery: $actualPrompt"
    }
    
    /**
     * Request camera permissions
     */
    fun requestPermissions(): Boolean {
        // In real app, would use ActivityResultContracts.RequestPermission
        // For now, assume permissions granted on API 28+
        hasPermission = true
        return true
    }
    
    /**
     * Clear photo and reset state
     */
    fun clearPhotoUri() {
        lastPhotoUri = null
        state = CameraState.READY
    }
    
    /**
     * Get recent photos for context in future queries
     */
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
