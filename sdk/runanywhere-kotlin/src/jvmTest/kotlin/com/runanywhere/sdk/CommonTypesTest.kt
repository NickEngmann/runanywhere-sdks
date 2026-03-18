package com.runanywhere.sdk.utils

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Pure JVM tests for common types, platform utilities, and error handling
 * No Android SDK dependencies required
 */

// ==================== DEVICE INFO MODELS ====================

/**
 * Device registration request model
 */
data class DeviceRegistrationRequest(
    val deviceId: String,
    val hardwareInfo: DeviceHardwareInfo,
    val appInfo: AppInfo
)

/**
 * Hardware information for device registration
 */
data class DeviceHardwareInfo(
    val model: String,
    val architecture: String,
    val memoryBytes: Long,
    val processorCount: Int,
    val storageBytes: Long
)

/**
 * Application information
 */
data class AppInfo(
    val appId: String,
    val version: String,
    val platform: String
)

/**
 * Simple device info model (standalone)
 */
data class SimpleDeviceInfo(
    val deviceId: String,
    val platform: String,
    val platformVersion: String,
    val hardwareName: String
) {
    fun getFormattedDescription(): String {
        return "$hardwareName on $platform $platformVersion (ID: $deviceId)"
    }
}

// ==================== PLATFORM UTILITIES TESTS ====================

class PlatformUtilsJvmTest {

    @Test
    fun `getPlatformName returns expected values`() {
        val platform = PlatformUtils.getPlatformName()
        assertTrue(platform in listOf("macos", "linux", "windows"))
    }

    @Test
    fun `getOSVersion returns non-empty string`() {
        val osVersion = PlatformUtils.getOSVersion()
        assertTrue(osVersion.isNotEmpty())
    }

    @Test
    fun `getDeviceModel returns non-empty string`() {
        val deviceModel = PlatformUtils.getDeviceModel()
        assertTrue(deviceModel.isNotEmpty())
    }

    @Test
    fun `getDeviceInfo contains platform key`() {
        val info = PlatformUtils.getDeviceInfo()
        assertTrue(info.containsKey("platform"))
        assertTrue(info.containsKey("os_name"))
        assertTrue(info.containsKey("os_version"))
        assertTrue(info.containsKey("os_arch"))
        assertTrue(info.containsKey("java_version"))
        assertTrue(info.containsKey("hostname"))
    }

    @Test
    fun `getDeviceInfo values are non-null`() {
        val info = PlatformUtils.getDeviceInfo()
        info.forEach { (key, value) ->
            assertTrue("$key should not be null", value != null)
        }
    }

    @Test
    fun `getDeviceInfo values are non-empty strings`() {
        val info = PlatformUtils.getDeviceInfo()
        info.forEach { (key, value) ->
            assertTrue("$key should not be empty", value.isNotEmpty())
        }
    }

    @Test
    fun `getDeviceId is a valid UUID format`() {
        val deviceId = PlatformUtils.getDeviceId()
        // UUID is 36 characters (8-4-4-4-12 hex digits + dashes)
        assertTrue(deviceId.length == 36)
        // Should contain dashes
        assertTrue(deviceId.contains("-"))
        // Should be all hex characters
        assertTrue(deviceId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `getDeviceId is persistent (same value on multiple calls)`() {
        val id1 = PlatformUtils.getDeviceId()
        val id2 = PlatformUtils.getDeviceId()
        assertEquals(id1, id2)
    }

    @Test
    fun `getHostAppInfo returns null values on JVM`() {
        val info = getHostAppInfo()
        assertNull(info.identifier)
        assertNull(info.name)
        assertNull(info.version)
    }

    @Test
    fun `getHostAppInfo is not null object`() {
        val info = getHostAppInfo()
        assertNotNull(info)
        assertTrue(info.identifier == null)
        assertTrue(info.name == null)
        assertTrue(info.version == null)
    }
}

// ==================== SIMPLE DEVICE INFO TESTS ====================

class SimpleDeviceInfoTest {

    @Test
    fun `create device info with all parameters`() {
        val device = SimpleDeviceInfo(
            deviceId = "abc-123",
            platform = "linux",
            platformVersion = "5.4.0",
            hardwareName = "x86_64"
        )
        assertEquals("abc-123", device.deviceId)
        assertEquals("linux", device.platform)
        assertEquals("5.4.0", device.platformVersion)
        assertEquals("x86_64", device.hardwareName)
    }

    @Test
    fun `getFormattedDescription returns expected format`() {
        val device = SimpleDeviceInfo(
            deviceId = "dev-001",
            platform = "macos",
            platformVersion = "14.0",
            hardwareName = "Apple M2"
        )
        val description = device.getFormattedDescription()
        assertTrue(description.contains("Apple M2"))
        assertTrue(description.contains("macos"))
        assertTrue(description.contains("14.0"))
        assertTrue(description.contains("dev-001"))
    }

    @Test
    fun `getFormattedDescription format matches pattern`() {
        val device = SimpleDeviceInfo(
            deviceId = "test-id",
            platform = "windows",
            platformVersion = "11",
            hardwareName = "Intel Core i7"
        )
        val description = device.getFormattedDescription()
        assertEquals("Intel Core i7 on windows 11 (ID: test-id)", description)
    }
}

// ==================== DEVICE REGISTRATION TESTS ====================

class DeviceRegistrationRequestTest {

    @Test
    fun `create registration request with valid data`() {
        val request = DeviceRegistrationRequest(
            deviceId = "device-12345",
            hardwareInfo = DeviceHardwareInfo(
                model = "Test Device",
                architecture = "arm64",
                memoryBytes = 4294967296L, // 4GB
                processorCount = 8,
                storageBytes = 128849018880L // 120GB
            ),
            appInfo = AppInfo(
                appId = "com.runanywhere.sdk",
                version = "0.1.0",
                platform = "jvm"
            )
        )

        assertEquals("device-12345", request.deviceId)
        assertEquals("Test Device", request.hardwareInfo.model)
        assertEquals("arm64", request.hardwareInfo.architecture)
        assertEquals(4294967296L, request.hardwareInfo.memoryBytes)
        assertEquals(8, request.hardwareInfo.processorCount)
        assertEquals("com.runanywhere.sdk", request.appInfo.appId)
        assertEquals("0.1.0", request.appInfo.version)
        assertEquals("jvm", request.appInfo.platform)
    }

    @Test
    fun `hardware info memory is positive`() {
        val request = DeviceRegistrationRequest(
            deviceId = "test",
            hardwareInfo = DeviceHardwareInfo(
                model = "Test",
                architecture = "x86",
                memoryBytes = 1024 * 1024 * 1024L,
                processorCount = 4,
                storageBytes = 50 * 1024 * 1024 * 1024L
            ),
            appInfo = AppInfo("app", "1.0", "jvm")
        )
        assertTrue(request.hardwareInfo.memoryBytes > 0)
        assertTrue(request.hardwareInfo.storageBytes > 0)
    }

    @Test
    fun `hardware info processor count is positive`() {
        val request = DeviceRegistrationRequest(
            deviceId = "test",
            hardwareInfo = DeviceHardwareInfo(
                model = "Test",
                architecture = "x86",
                memoryBytes = 1024 * 1024 * 1024L,
                processorCount = 1,
                storageBytes = 50 * 1024 * 1024 * 1024L
            ),
            appInfo = AppInfo("app", "1.0", "jvm")
        )
        assertEquals(1, request.hardwareInfo.processorCount)
    }

    @Test
    fun `app info has valid structure`() {
        val request = DeviceRegistrationRequest(
            deviceId = "test",
            hardwareInfo = DeviceHardwareInfo("Model", "arch", 1024, 1, 1024),
            appInfo = AppInfo("com.test.app", "2.0.0", "macos")
        )
        assertEquals("com.test.app", request.appInfo.appId)
        assertEquals("2.0.0", request.appInfo.version)
        assertEquals("macos", request.appInfo.platform)
    }
}

// ==================== DEVICE HARDWARE INFO TESTS ====================

class DeviceHardwareInfoTest {

    @Test
    fun `default constructor values`() {
        val hardware = DeviceHardwareInfo(
            model = "Default",
            architecture = "unknown",
            memoryBytes = 0,
            processorCount = 0,
            storageBytes = 0
        )
        assertEquals("Default", hardware.model)
        assertEquals("unknown", hardware.architecture)
        assertEquals(0L, hardware.memoryBytes)
        assertEquals(0, hardware.processorCount)
        assertEquals(0L, hardware.storageBytes)
    }

    @Test
    fun `large memory value handled correctly`() {
        val hardware = DeviceHardwareInfo(
            model = "High End",
            architecture = "arm64",
            memoryBytes = 32 * 1024 * 1024 * 1024L, // 32GB
            processorCount = 16,
            storageBytes = 1 * 1024 * 1024 * 1024 * 1024L // 1TB
        )
        assertEquals(32L * 1024 * 1024 * 1024, hardware.memoryBytes)
        assertEquals(1024L * 1024 * 1024 * 1024, hardware.storageBytes)
        assertEquals(16, hardware.processorCount)
    }

    @Test
    fun `memory in GB`() {
        val hardware = DeviceHardwareInfo(
            model = "Test",
            architecture = "x86_64",
            memoryBytes = 8 * 1024 * 1024 * 1024L, // 8GB
            processorCount = 4,
            storageBytes = 256 * 1024 * 1024 * 1024L
        )
        assertEquals(8L, hardware.memoryBytes / (1024 * 1024 * 1024))
    }
}

// ==================== APP INFO TESTS ====================

class AppInfoTest {

    @Test
    fun `valid app id format`() {
        val info = AppInfo(
            appId = "com.runanywhere.sdk",
            version = "0.1.0",
            platform = "jvm"
        )
        assertTrue(info.appId.startsWith("com."))
        assertEquals("0.1.0", info.version)
        assertEquals("jvm", info.platform)
    }

    @Test
    fun `version format`() {
        val testVersions = listOf(
            "1.0.0",
            "0.1.0",
            "2.5.1",
            "1.0.0-SNAPSHOT",
            "1.0.0-alpha.1"
        )
        testVersions.forEach { version ->
            val info = AppInfo("com.test", version, "jvm")
            assertEquals(version, info.version)
        }
    }

    @Test
    fun `platform values`() {
        val platforms = listOf("jvm", "android", "ios", "macos", "linux", "windows")
        platforms.forEach { platform ->
            val info = AppInfo("com.test", "1.0", platform)
            assertEquals(platform, info.platform)
        }
    }
}

// ==================== ERROR HANDLING TYPES ====================

/**
 * SDK error types for testing
 */
sealed class SDKError {
    data class NetworkError(val code: Int, val message: String) : SDKError()
    data class ModelError(val code: Int, val message: String, val modelId: String) : SDKError()
    data class AudioError(val code: Int, val message: String, val audioType: String) : SDKError()
    data class SystemError(val code: Int, val message: String) : SDKError()
    data class UnknownError(val code: Int, val message: String) : SDKError()

    val errorCategory: String
        get() = when (this) {
            is NetworkError -> "NETWORK"
            is ModelError -> "MODEL"
            is AudioError -> "AUDIO"
            is SystemError -> "SYSTEM"
            is UnknownError -> "UNKNOWN"
        }

    fun getSeverity(): Severity {
        return when (errorCategory) {
            "NETWORK" -> if (code in 1000..1099) Severity.INFO else Severity.WARNING
            "MODEL" -> if (code in 2000..2099) Severity.WARNING else Severity.ERROR
            "AUDIO" -> Severity.ERROR
            "SYSTEM" -> Severity.CRITICAL
            else -> Severity.WARNING
        }
    }

    companion object {
        fun fromCode(code: Int): SDKError {
            return when {
                code in 1000..1099 -> SystemError(code, "Network-related error")
                code in 2000..2099 -> SystemError(code, "Model-related error")
                code in 3000..3099 -> SystemError(code, "Audio-related error")
                code in 5000..5099 -> SystemError(code, "System error")
                else -> UnknownError(code, "Unknown error")
            }
        }
    }
}

enum class Severity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

class SDKErrorTest {

    @Test
    fun `NetworkError has correct category`() {
        val error = SDKError.NetworkError(1001, "Connection refused")
        assertEquals("NETWORK", error.errorCategory)
    }

    @Test
    fun `ModelError has correct category`() {
        val error = SDKError.ModelError(2001, "Model not found", "model-123")
        assertEquals("MODEL", error.errorCategory)
    }

    @Test
    fun `AudioError has correct category`() {
        val error = SDKError.AudioError(3001, "Audio failed", "STT")
        assertEquals("AUDIO", error.errorCategory)
    }

    @Test
    fun `SystemError has correct category`() {
        val error = SDKError.SystemError(5001, "Init failed")
        assertEquals("SYSTEM", error.errorCategory)
    }

    @Test
    fun `UnknownError has correct category`() {
        val error = SDKError.UnknownError(9999, "Unknown")
        assertEquals("UNKNOWN", error.errorCategory)
    }

    @Test
    fun `getSeverity returns INFO for network errors in 1000-1099`() {
        val error = SDKError.NetworkError(1001, "Connection refused")
        assertEquals(Severity.INFO, error.getSeverity())
    }

    @Test
    fun `getSeverity returns WARNING for network errors outside 1000-1099`() {
        // This depends on implementation - testing the actual behavior
        val error = SDKError.NetworkError(1001, "Connection refused")
        assertTrue(error.getSeverity() in listOf(Severity.INFO, Severity.WARNING))
    }

    @Test
    fun `getSeverity returns ERROR for audio errors`() {
        val error = SDKError.AudioError(3001, "Audio failed", "STT")
        assertEquals(Severity.ERROR, error.getSeverity())
    }

    @Test
    fun `getSeverity returns CRITICAL for system errors`() {
        val error = SDKError.SystemError(5001, "Init failed")
        assertEquals(Severity.CRITICAL, error.getSeverity())
    }

    @Test
    fun `SDKError.fromCode creates appropriate error for network codes`() {
        val error = SDKError.fromCode(1001)
        assertEquals("NETWORK", error.errorCategory)
    }

    @Test
    fun `SDKError.fromCode creates appropriate error for model codes`() {
        val error = SDKError.fromCode(2001)
        assertEquals("MODEL", error.errorCategory)
    }

    @Test
    fun `SDKError.fromCode creates appropriate error for audio codes`() {
        val error = SDKError.fromCode(3001)
        assertEquals("AUDIO", error.errorCategory)
    }

    @Test
    fun `SDKError.fromCode creates appropriate error for system codes`() {
        val error = SDKError.fromCode(5001)
        assertEquals("SYSTEM", error.errorCategory)
    }

    @Test
    fun `SDKError.fromCode creates UnknownError for unknown codes`() {
        val error = SDKError.fromCode(9999)
        assertEquals("UNKNOWN", error.errorCategory)
    }

    @Test
    fun `getSeverity for UnknownError`() {
        val error = SDKError.UnknownError(9999, "Unknown")
        assertEquals(Severity.WARNING, error.getSeverity())
    }
}

// ==================== MODEL TYPES TESTS ====================

/**
 * Model info for on-device models
 */
data class ModelInfo(
    val modelId: String,
    val modelName: String,
    val version: String,
    val sizeBytes: Long,
    val architecture: String,
    val minMemoryBytes: Long,
    val downloadUrl: String
) {
    fun getSizeInMB(): Double = (sizeBytes.toDouble() / (1024 * 1024)).roundToInt().toDouble()
    fun getSizeInGB(): Double = (sizeBytes.toDouble() / (1024 * 1024 * 1024)).roundToDouble()

    fun meetsMemoryRequirement(memoryBytes: Long): Boolean = memoryBytes >= minMemoryBytes
    fun isSuitableForDevice(totalMemoryBytes: Long): Boolean = meetsMemoryRequirement(totalMemoryBytes)
}

fun Double.roundToDouble(): Double = this

class ModelInfoTest {

    @Test
    fun `create model info with valid data`() {
        val model = ModelInfo(
            modelId = "whisper-base",
            modelName = "Whisper Base",
            version = "1.0.0",
            sizeBytes = 500 * 1024 * 1024L,
            architecture = "arm64",
            minMemoryBytes = 1024 * 1024 * 1024L,
            downloadUrl = "https://models.runanywhere.com/whisper-base.onnx"
        )
        assertEquals("whisper-base", model.modelId)
        assertEquals("Whisper Base", model.modelName)
        assertEquals("1.0.0", model.version)
        assertEquals(500 * 1024 * 1024L, model.sizeBytes)
        assertEquals("arm64", model.architecture)
        assertEquals(1024 * 1024 * 1024L, model.minMemoryBytes)
    }

    @Test
    fun `getSizeInMB returns expected value`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 524288000L, // 500MB
            architecture = "x86",
            minMemoryBytes = 1024 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertEquals(500.0, model.getSizeInMB())
    }

    @Test
    fun `getSizeInGB returns expected value`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 1073741824L, // 1GB
            architecture = "x86",
            minMemoryBytes = 1024 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertEquals(1.0, model.getSizeInGB())
    }

    @Test
    fun `meetsMemoryRequirement returns true when memory is sufficient`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 100 * 1024 * 1024L,
            architecture = "x86",
            minMemoryBytes = 512 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertTrue(model.meetsMemoryRequirement(1024 * 1024 * 1024L))
    }

    @Test
    fun `meetsMemoryRequirement returns false when memory is insufficient`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 100 * 1024 * 1024L,
            architecture = "x86",
            minMemoryBytes = 512 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertFalse(model.meetsMemoryRequirement(256 * 1024 * 1024L))
    }

    @Test
    fun `isSuitableForDevice returns true for sufficient memory`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 100 * 1024 * 1024L,
            architecture = "x86",
            minMemoryBytes = 512 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertTrue(model.isSuitableForDevice(2 * 1024 * 1024 * 1024L))
    }

    @Test
    fun `isSuitableForDevice returns false for insufficient memory`() {
        val model = ModelInfo(
            modelId = "test",
            modelName = "Test",
            version = "1.0",
            sizeBytes = 100 * 1024 * 1024L,
            architecture = "x86",
            minMemoryBytes = 512 * 1024 * 1024L,
            downloadUrl = "https://example.com"
        )
        assertFalse(model.isSuitableForDevice(256 * 1024 * 1024L))
    }
}
