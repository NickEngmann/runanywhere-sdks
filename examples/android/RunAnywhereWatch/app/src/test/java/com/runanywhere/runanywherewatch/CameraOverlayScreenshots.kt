package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for CameraOverlay using Paparazzi.
 * Renders actual Compose UI to PNG — no emulator or device needed.
 */
class CameraOverlayScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun cameraOverlay_viewfinder() {
        paparazzi.snapshot {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = {},
                    onAskAboutPhoto = {},
                    onCloseCamera = {},
                    isShowing = true
                )
            }
        }
    }

    @Test
    fun cameraOverlay_hidden() {
        paparazzi.snapshot {
            WatchFaceTheme {
                CameraOverlay(
                    onCapturePhoto = {},
                    onAskAboutPhoto = {},
                    onCloseCamera = {},
                    isShowing = false
                )
            }
        }
    }
}
