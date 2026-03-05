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

// Pure JVM tests (no Android SDK required)
include(":tests-jvm")

// NOTE: SDK modules (runanywhere-kotlin, core-llamacpp, core-onnx, core-rag) are NOT
// included here to avoid Kotlin multiplatform plugin conflicts. When the SDK is needed,
// publish it to mavenLocal and consume via implementation("com.runanywhere:...") instead
// of project(":runanywhere-kotlin"). For local development with the SDK, use the root
// project's settings.gradle.kts which includes RunAnywhereWatch as a composite build.
