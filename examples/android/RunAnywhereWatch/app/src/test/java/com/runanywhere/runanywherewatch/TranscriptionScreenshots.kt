package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for TranscriptionScreen using Paparazzi.
 * Renders actual Compose UI to PNG — no emulator or device needed.
 */
class TranscriptionScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun transcriptionScreen_empty() {
        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    isListening = false
                )
            }
        }
    }

    @Test
    fun transcriptionScreen_withEntries() {
        val entries = listOf(
            TranscriptionItem(
                id = 1,
                text = "Hey, check out the new RunAnywhere SDK — it supports on-device Qwen3.5 inference now.",
                timestamp = 1741500000000L,
                source = "voice",
                confidence = 0.95f
            ),
            TranscriptionItem(
                id = 2,
                text = "Running speech-to-text locally on the watch with Whisper.",
                timestamp = 1741500060000L,
                source = "voice",
                confidence = 0.88f
            ),
            TranscriptionItem(
                id = 3,
                text = "System initialized MCP server on port 8400",
                timestamp = 1741500120000L,
                source = "system",
                confidence = 1.0f
            )
        )

        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = entries,
                    isListening = true
                )
            }
        }
    }

    @Test
    fun transcriptionScreen_lowConfidence() {
        val entries = listOf(
            TranscriptionItem(
                id = 1,
                text = "This is a low confidence transcription that might need review",
                timestamp = 1741500000000L,
                source = "voice",
                confidence = 0.65f
            ),
            TranscriptionItem(
                id = 2,
                text = "Keyboard input is always high confidence",
                timestamp = 1741500060000L,
                source = "keyboard",
                confidence = 1.0f
            )
        )

        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = entries,
                    isListening = false
                )
            }
        }
    }
}
