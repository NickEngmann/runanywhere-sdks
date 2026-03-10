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
 * Compose UI tests that verify individual screens render and respond
 * to interactions. Uses Robolectric — no device needed.
 *
 * Note: Robolectric uses a phone-sized default viewport (> 230dp),
 * so AdaptiveLayout renders the phone branch. Watch-specific tests
 * are in the Paparazzi screenshot tests.
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
                TranscriptionScreen(transcriptions = emptyList())
            }
        }

        composeRule.onNodeWithText("No transcriptions yet", useUnmergedTree = true)
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
            .assertExists()
        composeRule.onNodeWithText("Test message", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `transcription screen shows source badges`() {
        val items = listOf(
            TranscriptionItem(1, "Voice input", System.currentTimeMillis(), "voice")
        )

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(transcriptions = items)
            }
        }

        composeRule.onNodeWithText("voice", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `transcription screen shows low confidence`() {
        val items = listOf(
            TranscriptionItem(1, "Uncertain", System.currentTimeMillis(), "voice", 0.65f)
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
    fun `transcription back button calls callback`() {
        var backClicked = false

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    onBack = { backClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("\u2190", useUnmergedTree = true)
            .performClick()

        assertTrue(backClicked)
    }

    @Test
    fun `transcription clear button calls callback`() {
        var cleared = false

        composeRule.setContent {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = listOf(
                        TranscriptionItem(1, "Test", System.currentTimeMillis())
                    ),
                    onClearAll = { cleared = true }
                )
            }
        }

        composeRule.onNodeWithText("Clear", substring = true, useUnmergedTree = true)
            .performClick()

        assertTrue(cleared)
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
            .assertExists()
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
    fun `camera close button calls callback`() {
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
    fun `camera quick ask triggers both callbacks`() {
        var photoCaptured = false
        var asked = false

        composeRule.setContent {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = { photoCaptured = true },
                    onAskAboutPhoto = { asked = true },
                    onCloseCamera = {},
                    isShowing = true
                )
            }
        }

        // Phone layout has Quick Ask button with "?" text
        composeRule.onNodeWithText("?", useUnmergedTree = true)
            .performClick()

        assertTrue("Photo should be captured", photoCaptured)
        assertTrue("Should ask about photo", asked)
    }

    // ── WatchFaceScreen ──

    @Test
    fun `watchface renders without crash`() {
        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }

        // Phone layout shows MIC button
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `watchface mic click triggers callback`() {
        var clicked = false

        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen(onMicClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun `watchface camera click triggers callback`() {
        var clicked = false

        composeRule.setContent {
            WatchFaceTheme {
                WatchFaceScreen(onCameraClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        assertTrue(clicked)
    }
}
