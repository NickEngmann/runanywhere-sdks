package com.runanywhere.sdk.utils

import com.runanywhere.sdk.utils.SSDKConstants.*
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import kotlinx.serialization.json.Json

/**
 * Pure JVM tests for SDK Constants management, configuration loading,
 * and feature flag validation. No Android dependencies required.
 */

// ==================== SDK CONFIGURATION ====================

@kotlinx.serialization.Serializable
data class SDKConfig(
    val environment: SDKConstants.Environment = SDKConstants.Environment.DEVELOPMENT,
    val apiBaseUrl: String = "",
    val cdnBaseUrl: String = "",
    val telemetryUrl: String = "",
    val analyticsUrl: String = "",
    val defaultApiKey: String = "",
    val enableVerboseLogging: Boolean = false,
    val enableMockServices: Boolean = false,
    val modelUrls: ModelUrlConfig = ModelUrlConfig(),
    val features: FeatureConfig = FeatureConfig(),
)

@kotlinx.serialization.Serializable
data class ModelUrlConfig(
    val whisperBase: String = "",
)

@kotlinx.serialization.Serializable
data class FeatureConfig(
    val onDeviceInference: Boolean = true,
    val cloudFallback: Boolean = true,
    val telemetry: Boolean = true,
    val analytics: Boolean = true,
    val debugLogging: Boolean = false,
    val performanceMonitoring: Boolean = true,
    val crashReporting: Boolean = false,
    val vad: Boolean = true,
    val sttAnalytics: Boolean = true,
    val realTimeStt: Boolean = true,
    val sttConfidenceScoring: Boolean = true,
)

class SDKConfigurationManager {
    private var config: SDKConfig = SDKConfig()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadConfiguration(configJson: String): Result<SDKConfig> = runCatching {
        config = json.decodeFromString(configJson)
        config
    }

    fun loadDefaultConfiguration() {
        config = SDKConfig()
    }

    fun getCurrentConfig(): SDKConfig = config
    fun getEnvironment(): SDKConstants.Environment = config.environment
    fun getBaseUrl(): String = config.apiBaseUrl
    fun isDevelopmentMode(): Boolean = config.environment == SDKConstants.Environment.DEVELOPMENT
}

// ==================== FEATURE FLAGS MANAGER ====================

class FeatureFlagsManager {
    private var flags = mutableMapOf<String, Boolean>()

    fun enableFlag(name: String, enabled: Boolean = true) {
        flags[name] = enabled
    }

    fun isFlagEnabled(name: String): Boolean = flags[name] ?: false
    fun disableFlag(name: String) = enableFlag(name, false)
    fun getAllFlags(): Map<String, Boolean> = flags.toMap()
    fun resetFlags() = flags.clear()
}

// ==================== ERROR CODE MANAGER ====================

class ErrorCodeManager {
    companion object {
        const val NETWORK_UNAVAILABLE = 1001
        const val REQUEST_TIMEOUT = 1002
        const val AUTHENTICATION_FAILED = 1003
        const val INVALID_API_KEY = 1004
        const val MODEL_NOT_FOUND = 2001
        const val MODEL_DOWNLOAD_FAILED = 2002
        const val MODEL_LOAD_FAILED = 2003
        const val INSUFFICIENT_MEMORY = 2004
        const val STT_INITIALIZATION_FAILED = 3001
        const val STT_PROCESSING_FAILED = 3002
        const val AUDIO_RECORDING_FAILED = 3003
        const val VAD_INITIALIZATION_FAILED = 3004
        const val INITIALIZATION_FAILED = 5001
        const val CONFIGURATION_INVALID = 5002
        const val PERMISSION_DENIED = 5003
        const val STORAGE_UNAVAILABLE = 5004
    }

    fun getCategory(code: Int): String {
        return when {
            code in 1000..1999 -> "NETWORK"
            code in 2000..2999 -> "MODEL"
            code in 3000..3999 -> "AUDIO"
            code in 5000..5999 -> "SYSTEM"
            else -> "UNKNOWN"
        }
    }

    fun getErrorCategory(code: Int): ErrorCategory {
        return when {
            code in 1000..1999 -> ErrorCategory.NETWORK
            code in 2000..2999 -> ErrorCategory.MODEL
            code in 3000..3999 -> ErrorCategory.AUDIO
            code in 5000..5999 -> ErrorCategory.SYSTEM
            else -> ErrorCategory.UNKNOWN
        }
    }

    fun getErrorMessage(code: Int): String {
        return when (code) {
            NETWORK_UNAVAILABLE -> "Network is not available"
            REQUEST_TIMEOUT -> "Request timed out"
            AUTHENTICATION_FAILED -> "Authentication failed"
            INVALID_API_KEY -> "Invalid API key"
            MODEL_NOT_FOUND -> "Model not found"
            MODEL_DOWNLOAD_FAILED -> "Model download failed"
            MODEL_LOAD_FAILED -> "Model load failed"
            INSUFFICIENT_MEMORY -> "Insufficient memory"
            STT_INITIALIZATION_FAILED -> "STT initialization failed"
            STT_PROCESSING_FAILED -> "STT processing failed"
            AUDIO_RECORDING_FAILED -> "Audio recording failed"
            VAD_INITIALIZATION_FAILED -> "VAD initialization failed"
            INITIALIZATION_FAILED -> "SDK initialization failed"
            CONFIGURATION_INVALID -> "Configuration invalid"
            PERMISSION_DENIED -> "Permission denied"
            STORAGE_UNAVAILABLE -> "Storage unavailable"
            else -> "Unknown error"
        }
    }
}

enum class ErrorCategory {
    NETWORK,
    MODEL,
    AUDIO,
    SYSTEM,
    UNKNOWN
}

// ==================== TESTS =========================

class SDKConfigurationManagerTest {

    private lateinit var manager: SDKConfigurationManager
    private lateinit var json: Json

    @Before
    fun setUp() {
        manager = SDKConfigurationManager()
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Test
    fun `default configuration has development environment`() {
        manager.loadDefaultConfiguration()
        assertEquals(SDKConstants.Environment.DEVELOPMENT, manager.getEnvironment())
        assertTrue(manager.isDevelopmentMode())
    }

    @Test
    fun `load valid development configuration`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.DEVELOPMENT,
                apiBaseUrl = "http://localhost:8080",
                enableVerboseLogging = true,
                enableMockServices = true
            )
        )
        val result = manager.loadConfiguration(configJson)
        assertTrue(result.isSuccess)
        assertEquals(SDKConstants.Environment.DEVELOPMENT, result.get().environment)
        assertEquals("http://localhost:8080", result.get().apiBaseUrl)
        assertTrue(result.get().enableVerboseLogging)
        assertTrue(result.get().enableMockServices)
    }

    @Test
    fun `load valid production configuration`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.PRODUCTION,
                apiBaseUrl = "https://api.runanywhere.com",
                cdnBaseUrl = "https://cdn.runanywhere.com",
                telemetryUrl = "https://telemetry.runanywhere.com",
                analyticsUrl = "https://analytics.runanywhere.com",
                enableVerboseLogging = false,
                enableMockServices = false
            )
        )
        val result = manager.loadConfiguration(configJson)
        assertTrue(result.isSuccess)
        val config = result.get()
        assertEquals(SDKConstants.Environment.PRODUCTION, config.environment)
        assertEquals("https://api.runanywhere.com", config.apiBaseUrl)
        assertEquals("https://cdn.runanywhere.com", config.cdnBaseUrl)
        assertEquals("https://telemetry.runanywhere.com", config.telemetryUrl)
        assertEquals("https://analytics.runanywhere.com", config.analyticsUrl)
    }

    @Test
    fun `load configuration with custom model URLs`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.STAGING,
                apiBaseUrl = "https://staging-api.runanywhere.com",
                modelUrls = ModelUrlConfig(whisperBase = "https://models.runanywhere.com/whisper-base.onnx")
            )
        )
        val result = manager.loadConfiguration(configJson)
        assertTrue(result.isSuccess)
        val config = result.get()
        assertEquals(SDKConstants.Environment.STAGING, config.environment)
        assertEquals("https://models.runanywhere.com/whisper-base.onnx", config.modelUrls.whisperBase)
    }

    @Test
    fun `load configuration with feature flags`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.DEVELOPMENT,
                features = FeatureConfig(
                    onDeviceInference = true,
                    cloudFallback = true,
                    telemetry = false,
                    analytics = false,
                    debugLogging = true,
                    performanceMonitoring = true,
                    crashReporting = true,
                    vad = true,
                    sttAnalytics = true,
                    realTimeStt = true,
                    sttConfidenceScoring = true
                )
            )
        )
        val result = manager.loadConfiguration(configJson)
        assertTrue(result.isSuccess)
        val config = result.get()
        assertTrue(config.features.onDeviceInference)
        assertTrue(config.features.cloudFallback)
        assertFalse(config.features.telemetry)
        assertFalse(config.features.analytics)
        assertTrue(config.features.debugLogging)
    }

    @Test
    fun `load invalid JSON configuration returns failure`() {
        val invalidJson = "not valid json {{{"
        val result = manager.loadConfiguration(invalidJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun `load configuration with empty strings uses defaults`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.DEVELOPMENT,
                apiBaseUrl = "",
                cdnBaseUrl = ""
            )
        )
        val result = manager.loadConfiguration(configJson)
        assertTrue(result.isSuccess)
        val config = result.get()
        assertEquals("", config.apiBaseUrl)
        assertEquals("", config.cdnBaseUrl)
    }

    @Test
    fun `getCurrentConfig returns loaded configuration`() {
        manager.loadDefaultConfiguration()
        val config = manager.getCurrentConfig()
        assertEquals(SDKConstants.Environment.DEVELOPMENT, config.environment)
    }

    @Test
    fun `getBaseUrl returns API base URL from configuration`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                apiBaseUrl = "https://example.api.com/v1"
            )
        )
        manager.loadConfiguration(configJson)
        assertEquals("https://example.api.com/v1", manager.getBaseUrl())
    }

    @Test
    fun `isDevelopmentMode returns true for development environment`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.DEVELOPMENT
            )
        )
        manager.loadConfiguration(configJson)
        assertTrue(manager.isDevelopmentMode())
    }

    @Test
    fun `isDevelopmentMode returns false for production environment`() {
        val configJson = json.encodeToString(
            kotlinx.serialization.modules.SerializersModule(),
            SDKConfig(
                environment = SDKConstants.Environment.PRODUCTION
            )
        )
        manager.loadConfiguration(configJson)
        assertFalse(manager.isDevelopmentMode())
    }
}

class FeatureFlagsManagerTest {

    private lateinit var manager: FeatureFlagsManager

    @Before
    fun setUp() {
        manager = FeatureFlagsManager()
    }

    @Test
    fun `initially all flags are disabled`() {
        assertTrue(!manager.isFlagEnabled("test_flag"))
    }

    @Test
    fun `enable flag sets it to true`() {
        manager.enableFlag("camera_feature", true)
        assertTrue(manager.isFlagEnabled("camera_feature"))
    }

    @Test
    fun `enable flag with default enabled set to true`() {
        manager.enableFlag("audio_feature")
        assertTrue(manager.isFlagEnabled("audio_feature"))
    }

    @Test
    fun `disable flag sets it to false`() {
        manager.enableFlag("experimental_feature", true)
        assertTrue(manager.isFlagEnabled("experimental_feature"))
        manager.disableFlag("experimental_feature")
        assertTrue(!manager.isFlagEnabled("experimental_feature"))
    }

    @Test
    fun `getAllFlags returns all enabled flags`() {
        manager.enableFlag("flag1", true)
        manager.enableFlag("flag2", true)
        manager.enableFlag("flag3", false)

        val allFlags = manager.getAllFlags()
        assertEquals(3, allFlags.size)
        assertTrue(allFlags["flag1"] == true)
        assertTrue(allFlags["flag2"] == true)
        assertTrue(allFlags["flag3"] == false)
    }

    @Test
    fun `resetFlags clears all flags`() {
        manager.enableFlag("flag1", true)
        manager.enableFlag("flag2", true)

        manager.resetFlags()

        assertTrue(manager.getAllFlags().isEmpty())
        assertTrue(!manager.isFlagEnabled("flag1"))
        assertTrue(!manager.isFlagEnabled("flag2"))
    }

    @Test
    fun `enable flag multiple times updates state`() {
        manager.enableFlag("toggled_feature", true)
        assertTrue(manager.isFlagEnabled("toggled_feature"))

        manager.enableFlag("toggled_feature", false)
        assertTrue(!manager.isFlagEnabled("toggled_feature"))

        manager.enableFlag("toggled_feature", true)
        assertTrue(manager.isFlagEnabled("toggled_feature"))
    }
}

class ErrorCodeManagerTest {

    private lateinit var errorCodeManager: ErrorCodeManager

    @Before
    fun setUp() {
        errorCodeManager = ErrorCodeManager()
    }

    @Test
    fun `getCategory returns NETWORK for network errors`() {
        assertEquals("NETWORK", errorCodeManager.getCategory(ErrorCodeManager.NETWORK_UNAVAILABLE))
        assertEquals("NETWORK", errorCodeManager.getCategory(ErrorCodeManager.REQUEST_TIMEOUT))
        assertEquals("NETWORK", errorCodeManager.getCategory(ErrorCodeManager.AUTHENTICATION_FAILED))
        assertEquals("NETWORK", errorCodeManager.getCategory(ErrorCodeManager.INVALID_API_KEY))
    }

    @Test
    fun `getCategory returns MODEL for model errors`() {
        assertEquals("MODEL", errorCodeManager.getCategory(ErrorCodeManager.MODEL_NOT_FOUND))
        assertEquals("MODEL", errorCodeManager.getCategory(ErrorCodeManager.MODEL_DOWNLOAD_FAILED))
        assertEquals("MODEL", errorCodeManager.getCategory(ErrorCodeManager.MODEL_LOAD_FAILED))
        assertEquals("MODEL", errorCodeManager.getCategory(ErrorCodeManager.INSUFFICIENT_MEMORY))
    }

    @Test
    fun `getCategory returns AUDIO for audio errors`() {
        assertEquals("AUDIO", errorCodeManager.getCategory(ErrorCodeManager.STT_INITIALIZATION_FAILED))
        assertEquals("AUDIO", errorCodeManager.getCategory(ErrorCodeManager.STT_PROCESSING_FAILED))
        assertEquals("AUDIO", errorCodeManager.getCategory(ErrorCodeManager.AUDIO_RECORDING_FAILED))
        assertEquals("AUDIO", errorCodeManager.getCategory(ErrorCodeManager.VAD_INITIALIZATION_FAILED))
    }

    @Test
    fun `getCategory returns SYSTEM for system errors`() {
        assertEquals("SYSTEM", errorCodeManager.getCategory(ErrorCodeManager.INITIALIZATION_FAILED))
        assertEquals("SYSTEM", errorCodeManager.getCategory(ErrorCodeManager.CONFIGURATION_INVALID))
        assertEquals("SYSTEM", errorCodeManager.getCategory(ErrorCodeManager.PERMISSION_DENIED))
        assertEquals("SYSTEM", errorCodeManager.getCategory(ErrorCodeManager.STORAGE_UNAVAILABLE))
    }

    @Test
    fun `getCategory returns UNKNOWN for other codes`() {
        assertEquals("UNKNOWN", errorCodeManager.getCategory(9999))
        assertEquals("UNKNOWN", errorCodeManager.getCategory(10000))
        assertEquals("UNKNOWN", errorCodeManager.getCategory(0))
    }

    @Test
    fun `getErrorCategory returns correct enum for network errors`() {
        assertEquals(ErrorCategory.NETWORK, errorCodeManager.getErrorCategory(ErrorCodeManager.NETWORK_UNAVAILABLE))
    }

    @Test
    fun `getErrorCategory returns correct enum for model errors`() {
        assertEquals(ErrorCategory.MODEL, errorCodeManager.getErrorCategory(ErrorCodeManager.MODEL_NOT_FOUND))
    }

    @Test
    fun `getErrorCategory returns correct enum for audio errors`() {
        assertEquals(ErrorCategory.AUDIO, errorCodeManager.getErrorCategory(ErrorCodeManager.STT_INITIALIZATION_FAILED))
    }

    @Test
    fun `getErrorCategory returns correct enum for system errors`() {
        assertEquals(ErrorCategory.SYSTEM, errorCodeManager.getErrorCategory(ErrorCodeManager.INITIALIZATION_FAILED))
    }

    @Test
    fun `getErrorCategory returns UNKNOWN for other codes`() {
        assertEquals(ErrorCategory.UNKNOWN, errorCodeManager.getErrorCategory(9999))
    }

    @Test
    fun `getErrorMessage returns human readable message for network errors`() {
        assertEquals("Network is not available", errorCodeManager.getErrorMessage(ErrorCodeManager.NETWORK_UNAVAILABLE))
        assertEquals("Request timed out", errorCodeManager.getErrorMessage(ErrorCodeManager.REQUEST_TIMEOUT))
        assertEquals("Authentication failed", errorCodeManager.getErrorMessage(ErrorCodeManager.AUTHENTICATION_FAILED))
        assertEquals("Invalid API key", errorCodeManager.getErrorMessage(ErrorCodeManager.INVALID_API_KEY))
    }

    @Test
    fun `getErrorMessage returns human readable message for model errors`() {
        assertEquals("Model not found", errorCodeManager.getErrorMessage(ErrorCodeManager.MODEL_NOT_FOUND))
        assertEquals("Model download failed", errorCodeManager.getErrorMessage(ErrorCodeManager.MODEL_DOWNLOAD_FAILED))
        assertEquals("Model load failed", errorCodeManager.getErrorMessage(ErrorCodeManager.MODEL_LOAD_FAILED))
        assertEquals("Insufficient memory", errorCodeManager.getErrorMessage(ErrorCodeManager.INSUFFICIENT_MEMORY))
    }

    @Test
    fun `getErrorMessage returns human readable message for audio errors`() {
        assertEquals("STT initialization failed", errorCodeManager.getErrorMessage(ErrorCodeManager.STT_INITIALIZATION_FAILED))
        assertEquals("STT processing failed", errorCodeManager.getErrorMessage(ErrorCodeManager.STT_PROCESSING_FAILED))
        assertEquals("Audio recording failed", errorCodeManager.getErrorMessage(ErrorCodeManager.AUDIO_RECORDING_FAILED))
        assertEquals("VAD initialization failed", errorCodeManager.getErrorMessage(ErrorCodeManager.VAD_INITIALIZATION_FAILED))
    }

    @Test
    fun `getErrorMessage returns human readable message for system errors`() {
        assertEquals("SDK initialization failed", errorCodeManager.getErrorMessage(ErrorCodeManager.INITIALIZATION_FAILED))
        assertEquals("Configuration invalid", errorCodeManager.getErrorMessage(ErrorCodeManager.CONFIGURATION_INVALID))
        assertEquals("Permission denied", errorCodeManager.getErrorMessage(ErrorCodeManager.PERMISSION_DENIED))
        assertEquals("Storage unavailable", errorCodeManager.getErrorMessage(ErrorCodeManager.STORAGE_UNAVAILABLE))
    }

    @Test
    fun `getErrorMessage returns unknown for unknown codes`() {
        assertEquals("Unknown error", errorCodeManager.getErrorMessage(9999))
    }

    @Test
    fun `getErrorMessage for default constants has valid messages`() {
        assertTrue(ErrorCodeManager.NETWORK_UNAVAILABLE in 1000..1999)
        assertTrue(ErrorCodeManager.MODEL_NOT_FOUND in 2000..2999)
        assertTrue(ErrorCodeManager.STT_INITIALIZATION_FAILED in 3000..3999)
        assertTrue(ErrorCodeManager.INITIALIZATION_FAILED in 5000..5999)
    }
}

// ==================== CONSTANTS TESTS ====================

class SDKConstantsTest {

    @Test
    fun `VERSION constant is set`() {
        assertEquals("0.1.0", SDKConstants.VERSION)
    }

    @Test
    fun `SDK_VERSION is same as VERSION`() {
        assertEquals(SDKConstants.VERSION, SDKConstants.SDK_VERSION)
    }

    @Test
    fun `SDK_NAME is set correctly`() {
        assertEquals("runanywhere-kotlin", SDKConstants.SDK_NAME)
    }

    @Test
    fun `USER_AGENT includes version`() {
        assertTrue(SDKConstants.USER_AGENT.startsWith("RunAnywhere-Kotlin-SDK/"))
        assertTrue(SDKConstants.USER_AGENT.contains(SDKConstants.VERSION))
    }

    @Test
    fun `platform constant is set`() {
        assertNotNull(SDKConstants.platform)
        assertTrue(SDKConstants.platform.isNotEmpty())
    }

    @Test
    fun `DEFAULT_API_KEY default value is empty string`() {
        assertEquals("", SDKConstants.DEFAULT_API_KEY)
    }

    @Test
    fun `API constants are non-empty strings`() {
        assertTrue(SDKConstants.API.AUTHENTICATE.isNotEmpty())
        assertTrue(SDKConstants.API.REFRESH_TOKEN.isNotEmpty())
        assertTrue(SDKConstants.API.LOGOUT.isNotEmpty())
        assertTrue(SDKConstants.API.CONFIGURATION.isNotEmpty())
        assertTrue(SDKConstants.API.MODELS.isNotEmpty())
    }

    @Test
    fun `API endpoints follow expected patterns`() {
        assertEquals("/v1/auth/token", SDKConstants.API.AUTHENTICATE)
        assertEquals("/v1/auth/refresh", SDKConstants.API.REFRESH_TOKEN)
        assertEquals("/v1/models", SDKConstants.API.MODELS)
        assertEquals("/v1/health", SDKConstants.API.HEALTH_CHECK)
    }

    @Test
    fun `Storage constants follow expected structure`() {
        assertEquals("runanywhere", SDKConstants.Storage.BASE_DIRECTORY)
        assertEquals("runanywhere/models", SDKConstants.Storage.MODELS_DIRECTORY)
        assertEquals("runanywhere/cache", SDKConstants.Storage.CACHE_DIRECTORY)
        assertEquals("runanywhere/logs", SDKConstants.Storage.LOGS_DIRECTORY)
    }

    @Test
    fun `Storage subdirectories are correctly named`() {
        assertEquals("runanywhere/models/language", SDKConstants.Storage.LANGUAGE_MODELS_DIR)
        assertEquals("runanywhere/models/speech", SDKConstants.Storage.SPEECH_MODELS_DIR)
        assertEquals("runanywhere/models/vision", SDKConstants.Storage.VISION_MODELS_DIR)
    }

    @Test
    fun `Secure storage keys are properly defined`() {
        assertEquals("runanywhere_sdk_keystore", SDKConstants.SecureStorage.KEYSTORE_ALIAS)
        assertEquals("runanywhere_secure_prefs", SDKConstants.SecureStorage.SHARED_PREFS_NAME)
        assertEquals("access_token", SDKConstants.SecureStorage.ACCESS_TOKEN_KEY)
        assertEquals("refresh_token", SDKConstants.SecureStorage.REFRESH_TOKEN_KEY)
        assertEquals("api_key", SDKConstants.SecureStorage.API_KEY_KEY)
        assertEquals("device_id", SDKConstants.SecureStorage.DEVICE_ID_KEY)
    }

    @Test
    fun `Development constants have expected defaults`() {
        assertEquals("dev-device-", SDKConstants.Development.MOCK_DEVICE_ID_PREFIX)
        assertEquals("dev-session-", SDKConstants.Development.MOCK_SESSION_ID_PREFIX)
        assertEquals("dev-user-", SDKConstants.Development.MOCK_USER_ID_PREFIX)
    }
}
