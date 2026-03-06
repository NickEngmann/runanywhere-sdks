package com.runanywhere.runanywherewatch

/**
 * Enum representing the SDK status
 */
enum class SDKStatus {
    NOT_LOADED,
    READY,
    THINKING
}

/**
 * Helper function to get SDK status description
 */
fun SDKStatus.getDescription(): String {
    return when (this) {
        SDKStatus.NOT_LOADED -> "Not Loaded"
        SDKStatus.READY -> "Ready"
        SDKStatus.THINKING -> "Thinking"
    }
}
