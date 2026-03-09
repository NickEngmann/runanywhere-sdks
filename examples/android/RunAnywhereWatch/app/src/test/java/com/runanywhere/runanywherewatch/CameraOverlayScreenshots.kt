package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class CameraWatchScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun camera_watch_viewfinder() {
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
}

class CameraPhoneScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun camera_phone_viewfinder() {
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
}
