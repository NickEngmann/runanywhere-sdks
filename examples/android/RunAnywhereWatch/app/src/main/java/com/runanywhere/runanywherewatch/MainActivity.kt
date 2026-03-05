package com.runanywhere.runanywherewatch

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.sdk.RunAnywhereSDK
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchFaceTheme {
                WatchFaceScreen(
                    onMicClick = {
                        // Handle mic click - voice input
                    },
                    onCameraClick = {
                        // Handle camera click
                    }
                )
            }
        }
    }
}

@Composable
fun WatchFaceScreen(
    onMicClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val context = LocalContext.current
    var sdkStatus by remember { mutableStateOf(SDKStatus.NOT_LOADED) }
    var batteryLevel by remember { mutableStateOf(100) }
    
    // Initialize SDK
    LaunchedEffect(Unit) {
        try {
            RunAnywhereSDK.initialize(context)
            sdkStatus = SDKStatus.READY
        } catch (e: Exception) {
            sdkStatus = SDKStatus.NOT_LOADED
        }
    }
    
    // Simulate battery level (in real app, use BatteryManager)
    LaunchedEffect(Unit) {
        // Update battery periodically
    }
    
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
                    onClick = onCameraClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                ) {
                    Text(
                        "📷",
                        fontSize = 24.sp
                    )
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
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(2.dp, Color(0xFF00E5FF), CircleShape)
                ) {
                    Text(
                        "🎤",
                        fontSize = 32.sp
                    )
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
