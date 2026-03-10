package com.runanywhere.runanywherewatch

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests that verify the app actually runs.
 * These launch the real MainActivity and test that screens render,
 * buttons respond to clicks, and the app doesn't crash through
 * various lifecycle states.
 *
 * Unlike instrumented tests (androidTest/), these run on JVM —
 * no emulator or device needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // ── App Launch ──

    @Test
    fun `app launches without crashing`() {
        // If we get here, the Activity created successfully
        assertNotNull(composeRule.activity)
    }

    @Test
    fun `activity is in resumed state after launch`() {
        assertEquals(
            Lifecycle.State.RESUMED,
            composeRule.activity.lifecycle.currentState
        )
    }

    // ── WatchFace Screen ──

    @Test
    fun `watchface displays time`() {
        // Time is rendered in HH:mm format — check the colon separator exists
        composeRule.onNodeWithText(":", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `watchface displays MIC button`() {
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `watchface displays CAM button`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `watchface displays battery level`() {
        composeRule.onNodeWithText("Battery:", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    // ── Camera Overlay ──

    @Test
    fun `tapping CAM opens camera overlay`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        // Camera overlay should show "Camera" text in the preview area
        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `camera overlay has close button`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        // Close button renders unicode ✕
        composeRule.onNodeWithText("\u2715", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `camera close button dismisses overlay`() {
        // Open camera
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertIsDisplayed()

        // Close it
        composeRule.onNodeWithText("\u2715", useUnmergedTree = true)
            .performClick()

        // MIC button should be visible again (back to watchface)
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `camera overlay has capture button`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        // Quick Ask button with "?" text
        composeRule.onNodeWithText("?", useUnmergedTree = true)
            .assertExists()
    }

    // ── Lifecycle ──

    @Test
    fun `app survives configuration change`() {
        // Verify initial state
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertIsDisplayed()

        // Simulate rotation
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()

        // App should still be running
        assertNotNull(composeRule.activity)
    }

    @Test
    fun `app survives pause and resume`() {
        val scenario = composeRule.activityRule.scenario

        scenario.moveToState(Lifecycle.State.STARTED) // pause
        scenario.moveToState(Lifecycle.State.RESUMED)  // resume

        // Should still render
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `app survives stop and restart`() {
        val scenario = composeRule.activityRule.scenario

        scenario.moveToState(Lifecycle.State.CREATED) // stop
        scenario.moveToState(Lifecycle.State.RESUMED)  // restart

        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ── SDK Status ──

    @Test
    fun `SDKStatus enum has correct values`() {
        assertEquals(3, SDKStatus.values().size)
        assertNotNull(SDKStatus.NOT_LOADED)
        assertNotNull(SDKStatus.READY)
        assertNotNull(SDKStatus.THINKING)
    }

    // ── CameraManager ──

    @Test
    fun `CameraManager initializes correctly`() {
        val manager = CameraManager()
        assertEquals(CameraState.NOT_INITIALIZED, manager.state)
        assertTrue(manager.initializeCamera())
        assertEquals(CameraState.READY, manager.state)
    }

    @Test
    fun `CameraManager captures photo`() {
        val manager = CameraManager()
        manager.initializeCamera()
        assertTrue(manager.capturePhoto())
        assertNotNull(manager.lastPhotoUri)
    }

    @Test
    fun `CameraManager constructs vision query`() {
        val manager = CameraManager()
        manager.initializeCamera()
        manager.capturePhoto()
        val query = manager.constructVisionQuery("What is this?")
        assertNotNull(query)
        assertTrue(query!!.contains("What is this?"))
    }

    @Test
    fun `CameraManager resize keeps small images`() {
        val manager = CameraManager()
        val size = manager.resizeImage(200, 200)
        assertEquals(200, size.width)
        assertEquals(200, size.height)
    }

    @Test
    fun `CameraManager resize scales large images`() {
        val manager = CameraManager()
        val size = manager.resizeImage(1920, 1080)
        assertTrue(size.width <= 512)
        assertTrue(size.height <= 512)
    }
}
