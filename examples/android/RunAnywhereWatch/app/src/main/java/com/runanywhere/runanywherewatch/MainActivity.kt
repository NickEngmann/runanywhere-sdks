package com.runanywhere.runanywherewatch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchFaceTheme {
                WatchFaceScreen(
                    onMicClick = { handleMicClick() },
                    onCameraClick = { handleCameraClick() },
                    onVisionQuery = { query -> handleVisionQuery(query) },
                    onPhotoCaptured = { photoFile -> handlePhotoCaptured(photoFile) }
                )
            }
        }
    }
    
    private fun handleMicClick() {
        val cameraManager = CameraManager()
        if (cameraManager.initializeCamera()) {
            showQueryInput("Ask about this...")
        }
    }
    
    private fun handleCameraClick() {
        val cameraManager = CameraManager()
        if (cameraManager.initializeCamera()) {
            showCameraOverlay(cameraManager)
        }
    }
    
    private fun handleVisionQuery(query: String) {
        showResponse("Processing: $query")
    }
    
    private fun handlePhotoCaptured(photoFile: java.io.File) {
        showResponse("Photo captured: ${photoFile.name}")
    }
    
    private fun showQueryInput(prompt: String) { }
    private fun showCameraOverlay(cameraManager: CameraManager) { }
    private fun showResponse(message: String) { }
}

@Composable
fun WatchFaceScreen(
    onMicClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onVisionQuery: (String) -> Unit = {},
    onPhotoCaptured: (java.io.File) -> Unit = {}
) {
    var sdkStatus by remember { mutableStateOf(SDKStatus.NOT_LOADED) }
    var batteryLevel by remember { mutableStateOf(100) }
    var showCameraOverlay by remember { mutableStateOf(false) }
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var visionResponse by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with camera button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { 
                        showCameraOverlay = true
                        cameraManager = CameraManager()
                        onCameraClick()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                ) {
                    Text("camera", fontSize = 14.sp)
                }
            }
            
            // AI Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when (sdkStatus) {
                                SDKStatus.NOT_LOADED -> Color(0xFF666666)
                                SDKStatus.THINKING -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                                SDKStatus.READY -> Color(0xFF00FF00)
                            }
                        )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Time Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getCurrentTime(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    color = Color(0xFF00E5FF)
                )
                
                Text(
                    text = getCurrentDate(),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Text(
                    text = getCurrentSeconds(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Battery Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Battery: ${batteryLevel}%",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Bottom bar with mic button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { 
                        cameraManager = CameraManager()
                        onMicClick()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(2.dp, Color(0xFF00E5FF), CircleShape)
                ) {
                    Text("mic", fontSize = 14.sp)
                }
            }
        }
        
        // Circular border for round screen
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Camera Overlay
        if (showCameraOverlay && cameraManager != null) {
            CameraOverlay(
                onCapturePhoto = { photoFile ->
                    onPhotoCaptured(photoFile)
                },
                onAskAboutPhoto = { photoFile ->
                    val query = cameraManager?.constructVisionQuery("What is in this image?")
                    query?.let { onVisionQuery(it) }
                },
                onCloseCamera = {
                    showCameraOverlay = false
                    cameraManager?.clearPhotoUri()
                },
                isShowing = showCameraOverlay
            )
        }
        
        // Vision Response Overlay
        visionResponse?.let { response ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Response:",
                        fontSize = 20.sp,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = response,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Tap to close",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun WatchFaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00E5FF),
            background = Color(0xFF0D0D0D),
            surface = Color(0xFF1A1A1A)
        ),
        typography = Typography(
            displayLarge = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    ) {
        content()
    }
}

enum class SDKStatus {
    NOT_LOADED,
    READY,
    THINKING
}

@Composable
fun getCurrentTime(): String {
    val calendar = Calendar.getInstance()
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(calendar.time)
}

@Composable
fun getCurrentDate(): String {
    val calendar = Calendar.getInstance()
    val format = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return format.format(calendar.time)
}

@Composable
fun getCurrentSeconds(): String {
    val calendar = Calendar.getInstance()
    val format = SimpleDateFormat("ss", Locale.getDefault())
    return ":${format.format(calendar.time)}"
}
