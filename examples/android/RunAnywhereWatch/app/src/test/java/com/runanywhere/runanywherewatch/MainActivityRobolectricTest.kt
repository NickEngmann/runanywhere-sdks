package com.runanywhere.runanywherewatch

import android.content.pm.ActivityInfo
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
 * Launches the real MainActivity and tests screens render,
 * buttons respond to clicks, and the app doesn't crash.
 *
 * Robolectric uses default phone-sized viewport (> 230dp),
 * so the phone layout branch renders (MIC, CAM, Battery: X%).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // ── App Launch ──

    @Test
    fun `app launches without crashing`() {
        assertNotNull(composeRule.activity)
    }

    @Test
    fun `activity is in resumed state`() {
        assertEquals(
            Lifecycle.State.RESUMED,
            composeRule.activity.lifecycle.currentState
        )
    }

    // ── WatchFace Screen (phone layout) ──

    @Test
    fun `watchface displays MIC button`() {
        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `watchface displays CAM button`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `watchface displays battery`() {
        composeRule.onNodeWithText("Battery:", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    // ── Camera Overlay (phone layout) ──

    @Test
    fun `tapping CAM opens camera overlay`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `camera overlay has close button`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText("\u2715", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `camera close button dismisses overlay`() {
        composeRule.onNodeWithText("CAM", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText("Camera", useUnmergedTree = true)
            .assertExists()

        composeRule.onNodeWithText("\u2715", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithText("MIC", useUnmergedTree = true)
            .assertExists()
    }

    // ── Lifecycle Resilience ──

    @Test
    fun `app survives pause and resume`() {
        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertNotNull(composeRule.activity)
    }

    @Test
    fun `app survives stop and restart`() {
        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertNotNull(composeRule.activity)
    }

    // ── Business Logic (pure Kotlin, no Android framework) ──

    @Test
    fun `SDKStatus enum has correct values`() {
        assertEquals(3, SDKStatus.values().size)
        assertEquals("NOT_LOADED", SDKStatus.NOT_LOADED.toString())
        assertEquals("READY", SDKStatus.READY.toString())
        assertEquals("THINKING", SDKStatus.THINKING.toString())
    }

    @Test
    fun `CameraManager initializes and captures`() {
        val mgr = CameraManager()
        assertEquals(CameraState.NOT_INITIALIZED, mgr.state)
        assertTrue(mgr.initializeCamera())
        assertEquals(CameraState.READY, mgr.state)
        assertTrue(mgr.capturePhoto())
        assertNotNull(mgr.lastPhotoUri)
    }

    @Test
    fun `CameraManager vision query`() {
        val mgr = CameraManager()
        mgr.initializeCamera()
        mgr.capturePhoto()
        val q = mgr.constructVisionQuery("What is this?")
        assertNotNull(q)
        assertTrue(q!!.contains("What is this?"))
    }

    @Test
    fun `CameraManager image resize`() {
        val mgr = CameraManager()
        val small = mgr.resizeImage(200, 200)
        assertEquals(200, small.width)
        val big = mgr.resizeImage(1920, 1080)
        assertTrue(big.width <= 512)
        assertTrue(big.height <= 512)
    }

    @Test
    fun `CameraManager photo history and clear`() {
        val mgr = CameraManager()
        mgr.initializeCamera()
        assertTrue(mgr.capturePhoto())  // state → CAPTURING
        assertEquals(1, mgr.getRecentPhotos().size)
        mgr.clearPhotoUri()  // state → READY
        assertNull(mgr.lastPhotoUri)
        assertEquals(CameraState.READY, mgr.state)
        assertTrue(mgr.capturePhoto())  // second capture works after clear
        assertEquals(2, mgr.getRecentPhotos().size)
    }
}
