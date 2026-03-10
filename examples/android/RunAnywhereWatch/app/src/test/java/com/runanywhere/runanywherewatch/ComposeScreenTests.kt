package com.runanywhere.runanywherewatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests that verify individual screens render correctly
 * and respond to interactions. Uses Robolectric — no device needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeScreenTests {

    @get:Rule
    val composeRule = createComposeRule()

    // ── TranscriptionScreen ──

    @Test
    fun `transcription screen renders empty state`() {
        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList()
                )
            }
        }

        composeRule.onNodeWithText("No transcriptions yet", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Tap the mic to start", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `transcription screen renders entries`() {
        val items = listOf(
            TranscriptionItem(1, "Hello world", System.currentTimeMillis(), "voice", 0.95f),
            TranscriptionItem(2, "Test message", System.currentTimeMillis(), "keyboard", 1.0f)
        )

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(transcriptions = items)
            }
        }

        composeRule.onNodeWithText("Hello world", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Test message", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `transcription screen shows source badges`() {
        val items = listOf(
            TranscriptionItem(1, "Voice input", System.currentTimeMillis(), "voice"),
            TranscriptionItem(2, "Typed input", System.currentTimeMillis(), "keyboard")
        )

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(transcriptions = items)
            }
        }

        composeRule.onNodeWithText("voice", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("keyboard", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `transcription screen shows low confidence indicator`() {
        val items = listOf(
            TranscriptionItem(1, "Uncertain text", System.currentTimeMillis(), "voice", 0.65f)
        )

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(transcriptions = items)
            }
        }

        composeRule.onNodeWithText("65%", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `transcription screen has back button`() {
        var backClicked = false

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    onBack = { backClicked = true }
                )
            }
        }

        // Back arrow unicode
        composeRule.onNodeWithText("\u2190", useUnmergedTree = true)
            .performClick()

        assertTrue(backClicked)
    }

    @Test
    fun `transcription screen clear button works`() {
        var cleared = false
        val items = listOf(
            TranscriptionItem(1, "Test", System.currentTimeMillis())
        )

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = items,
                    onClearAll = { cleared = true }
                )
            }
        }

        composeRule.onNodeWithText("Clear", substring = true, useUnmergedTree = true)
            .performClick()

        assertTrue(cleared)
    }

    @Test
    fun `transcription screen shows listening indicator`() {
        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    isListening = true
                )
            }
        }

        // Listening indicator is a red dot — screen should render without crash
        // (dot has no text, but we verify the screen renders with isListening=true)
        composeRule.onNodeWithText("No transcriptions yet", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ── CameraOverlay ──

    @Test
    fun `camera overlay renders when showing`() {
        composeRule.setContent {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = {},
                    onAskAboutPhoto = {},
                    onCloseCamera = {},
                    isShowing = true
                )
            }
        }

        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `camera overlay hidden when not showing`() {
        composeRule.setContent {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = {},
                    onAskAboutPhoto = {},
                    onCloseCamera = {},
                    isShowing = false
                )
            }
        }

        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `camera overlay close button calls callback`() {
        var closed = false

        composeRule.setContent {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = {},
                    onAskAboutPhoto = {},
                    onCloseCamera = { closed = true },
                    isShowing = true
                )
            }
        }

        composeRule.onNodeWithText("\u2715", useUnmergedTree = true)
            .performClick()

        assertTrue(closed)
    }

    @Test
    fun `camera overlay quick ask triggers callbacks`() {
        var photoCaptured = false
        var askedAboutPhoto = false

        composeRule.setContent {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = { photoCaptured = true },
                    onAskAboutPhoto = { askedAboutPhoto = true },
                    onCloseCamera = {},
                    isShowing = true
                )
            }
        }

        composeRule.onNodeWithText("?", useUnmergedTree = true)
            .performClick()

        assertTrue(photoCaptured)
        assertTrue(askedAboutPhoto)
    }

    // ── WatchFaceScreen ──

    @Test
    fun `watchface screen renders in theme`() {
        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }

        // Should show time with colon
        composeRule.onNodeWithText(":", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `watchface mic click triggers callback`() {
        var micClicked = false

        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen(onMicClick = { micClicked = true })
            }
        }

        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .performClick()

        assertTrue(micClicked)
    }

    @Test
    fun `watchface camera click triggers callback`() {
        var camClicked = false

        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen(onCameraClick = { camClicked = true })
            }
        }

        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        assertTrue(camClicked)
    }
}
