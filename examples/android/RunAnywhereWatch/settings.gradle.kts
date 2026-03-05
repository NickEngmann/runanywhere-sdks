pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") {
            from(files(File(settingsDir, "../../../gradle/libs.versions.toml")))
        }
    }
}

rootProject.name = "RunAnywhereWatch"
include(":app")

// SDK (local project dependency) - EXCLUDED FOR TESTS
// include(":runanywhere-kotlin")
// project(":runanywhere-kotlin").projectDir = file("../../../sdk/runanywhere-kotlin")

// =============================================================================
// Backend Adapter Modules (Pure Kotlin - no native libs) - EXCLUDED FOR TESTS
// =============================================================================
// include(":runanywhere-core-llamacpp")
// project(":runanywhere-core-llamacpp").projectDir =
//     file("../../../sdk/runanywhere-kotlin/modules/runanywhere-core-llamacpp")

// include(":runanywhere-core-onnx")
// project(":runanywhere-core-onnx").projectDir =
//     file("../../../sdk/runanywhere-kotlin/modules/runanywhere-core-onnx")

// include(":runanywhere-core-rag")
// project(":runanywhere-core-rag").projectDir =
//     file("../../../sdk/runanywhere-kotlin/modules/runanywhere-core-rag")

// Pure JVM tests (no Android SDK required)
include(":tests-jvm")

// Android SDK module
include(":sdks:android:runanywhere")
project(":sdks:android:runanywhere").projectDir = file("../../../sdk/android/runanywhere")
