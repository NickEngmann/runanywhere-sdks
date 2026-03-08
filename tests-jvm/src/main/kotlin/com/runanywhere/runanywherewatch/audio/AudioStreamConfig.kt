package com.runanywhere.runanywherewatch.audio

import java.util.UUID

/**
 * Configuration for audio streaming session.
 */
data class AudioStreamConfig(
    val bitrate: AudioBitrate,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val chunkSize: Int = 1024,
    val sessionId: UUID = UUID.randomUUID()
)
