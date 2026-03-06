package com.runanywhere.runanywherewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val transcriptionViewModel by lazy { TranscriptionViewModel() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchFaceTheme {
                WatchFaceScreen(
                    onMicClick = { handleMicClick() },
                    onCameraClick = { handleCameraClick() },
                    onVisionQuery = { query -> handleVisionQuery(query) },
                    onPhotoCaptured = { photoFile -> handlePhotoCaptured(photoFile) },
                    onTranscriptionClick = { /* Handled internally */ },
                    onTranscriptionDismiss = { /* Handled internally */ },
                    transcriptionViewModel = transcriptionViewModel
                )
            }
        }
    }
    
    private fun handleMicClick() {
        // Initialize camera manager if needed
        val cameraManager = CameraManager()
        if (cameraManager.initializeCamera()) {
            // Start voice input flow
            // In real app: start STT, capture audio, send to LLM
            showQueryInput("Ask about this...")
            
            // Simulate transcription
            transcriptionViewModel.addTranscription("Voice input started...")
            transcriptionViewModel.updateConnectionStatus(ConnectionStatus.CONNECTED)
        }
    }
    
    private fun handleCameraClick() {
        // Initialize camera overlay
        val cameraManager = CameraManager()
        if (cameraManager.initializeCamera()) {
            // Show camera overlay
            showCameraOverlay(cameraManager)
        }
    }
    
    private fun handleVisionQuery(query: String) {
        // Send query to multimodal VLM
        // In real app: call RunAnywhereSDK with vision query
        showResponse("Processing: $query")
    }
    
    private fun handlePhotoCaptured(photoFile: java.io.File) {
        // Photo captured, ready for vision query
        showResponse("Photo captured: ${photoFile.name}")
    }
    
    private fun showQueryInput(prompt: String) {
        // Show query input UI
        // In real app: display input field or start STT
    }
    
    private fun showCameraOverlay(cameraManager: CameraManager) {
        // Show camera overlay UI
        // In real app: would use CameraX for preview
    }
    
    private fun showResponse(message: String) {
        // Show response UI
        // In real app: display LLM response
    }
}

@Composable
fun WatchFaceScreen(
    onMicClick: (Context) -> Unit,
    onCameraClick: (Context) -> Unit,
    onVisionQuery: (String) -> Unit,
    onPhotoCaptured: (java.io.File) -> Unit,
    onTranscriptionClick: () -> Unit,
    onTranscriptionDismiss: () -> Unit,
    transcriptionViewModel: TranscriptionViewModel
) {
    val context = LocalContext.current
    var sdkStatus by remember { mutableStateOf(SDKStatus.NOT_LOADED) }
    var batteryLevel by remember { mutableStateOf(100) }
    var showCameraOverlay by remember { mutableStateOf(false) }
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var visionResponse by remember { mutableStateOf<String?>(null) }
    var showTranscriptionScreen by remember { mutableStateOf(false) }
    
    // Simulate incoming transcriptions from STT pipeline
    LaunchedEffect(Unit) {
        // In real app: subscribe to STT pipeline transcription events
        // For now, we'll add sample transcriptions when mic is clicked
    }
    
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
                    onClick = { 
                        showCameraOverlay = true
                        cameraManager = CameraManager(context)
                        onCameraClick(context)
                    },
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
                    onClick = { 
                        cameraManager = CameraManager(context)
                        onMicClick(context)
                    },
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
            
            // Transcription button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { showTranscriptionScreen = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        "📝 Transcriptions",
                        fontSize = 14.sp
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
        
        // Transcription Screen Overlay
        if (showTranscriptionScreen) {
            TranscriptionScreenOverlay(
                viewModel = transcriptionViewModel,
                onDismiss = { showTranscriptionScreen = false }
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

/**
 * Overlay composable for displaying transcription screen
 */
@Composable
fun TranscriptionScreenOverlay(
    viewModel: TranscriptionViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Transcriptions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                
                IconButton(onClick = onDismiss) {
                    Text(
                        text = "✕",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Connection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ConnectionStatusIndicator(
                    status = viewModel.connectionStatus,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transcription list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.transcriptions.size) { index ->
                    val entry = viewModel.transcriptions[index]
                    TranscriptionEntryItem(
                        entry = entry,
                        modifier = Modifier.animateItem()
                    )
                }
                
                if (viewModel.transcriptions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transcriptions yet",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
            
            // Clear button
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearTranscriptions() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        text = "Clear Transcriptions",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
