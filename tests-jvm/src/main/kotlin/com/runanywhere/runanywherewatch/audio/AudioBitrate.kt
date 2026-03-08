package com.runanywhere.runanywherewatch.audio

/**
 * Audio bitrate configuration for BLE transport.
 */
enum class AudioBitrate(val kbps: Int, val bytesPerSec: Int) {
    LOW(8, 1000),
    MEDIUM(16, 2000),
    HIGH(32, 4000)
}
