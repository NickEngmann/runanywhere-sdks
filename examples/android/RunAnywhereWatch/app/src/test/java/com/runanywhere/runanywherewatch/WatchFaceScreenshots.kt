package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class WatchFaceScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun watchFace_watch_default() {
        paparazzi.snapshot {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }
    }
}

class WatchFacePhoneScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun watchFace_phone_default() {
        paparazzi.snapshot {
            WatchFaceTheme {
                WatchFaceScreen()
            }
        }
    }
}
