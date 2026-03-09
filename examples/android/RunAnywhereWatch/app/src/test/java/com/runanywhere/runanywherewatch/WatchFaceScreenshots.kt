package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for WatchFaceScreen using Paparazzi.
 * Renders actual Compose UI to PNG — no emulator or device needed.
 *
 * Run: ./gradlew :app:recordPaparazziDebug
 * Verify: ./gradlew :app:verifyPaparazziDebug
 */
class WatchFaceScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun watchFace_defaultState() {
        paparazzi.snapshot {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }
    }

    @Test
    fun watchFace_sdkReady() {
        paparazzi.snapshot {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }
    }

    @Test
    fun watchFace_phoneLayout() {
        paparazzi.snapshot {
            WatchFaceTheme {
                WatchFaceScreen(
                    onMicClick = {},
                    onCameraClick = {},
                    onVisionQuery = {},
                    onPhotoCaptured = {}
                )
            }
        }
    }
}
