/**
 * RunAnywhere Pairing Module
 *
 * This module provides secure device pairing functionality with:
 * - QR code generation and parsing for device identification
 * - ECDH key exchange for secure session establishment
 * - BLE pairing protocol implementation
 * - Pairing state persistence
 *
 * Architecture (mirrors iOS pairing architecture):
 *   iOS:     Pairing.swift -> PairingManager.swift -> QRCodeParser.swift
 *   Android: PairingModule.kt -> PairingStateManager.kt -> PairingQrCode.kt
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    `maven-publish`
    signing
}

val testLocal: Boolean =
    rootProject.findProperty("runanywhere.testLocal")?.toString()?.toBoolean()
        ?: project.findProperty("runanywhere.testLocal")?.toString()?.toBoolean()
        ?: false

logger.lifecycle("Pairing Module: testLocal=$testLocal")

// Detekt
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("../../detekt.yml"))
    source.setFrom(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/jvmAndroidMain/kotlin",
        "src/androidMain/kotlin",
    )
}

// ktlint
ktlint {
    version.set("1.5.0")
    android.set(true)
    verbose.set(true)
    outputToConsole.set(true)
    enableExperimentalRules.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

kotlin {
    jvm {
        compilations.all {
            compilerOptions {
                jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")

        mavenPublication {
            artifactId = "runanywhere-pairing-android"
        }

        compilations.all {
            compilerOptions {
                jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core SDK
                api(
                    rootProject.allprojects.firstOrNull {
                        it.projectDir.canonicalPath == projectDir.resolve("../..").canonicalPath
                    } ?: error("Cannot find core SDK project at ${projectDir.resolve("../..")}"),
                )
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvmAndroidMain by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(jvmAndroidMain)
        }

        val androidMain by getting {
            dependsOn(jvmAndroidMain)
        }

        val jvmAndroidTest by creating {
            dependsOn(commonTest)
        }

        val jvmTest by getting {
            dependsOn(jvmAndroidTest)
        }

        val androidUnitTest by getting {
            dependsOn(jvmAndroidTest)
        }
    }
}

android {
    namespace = "com.runanywhere.sdk.pairing"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Maven publishing configuration
val mavenCentralUsername: String? = System.getenv("MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword: String? = System.getenv("MAVEN_CENTRAL_PASSWORD")
val signingKeyId: String? = System.getenv("GPG_KEY_ID")
val signingPassword: String? = System.getenv("GPG_PASSWORD")
val signingKey: String? =
    System.getenv("GPG_SIGNING_KEY")
        ?: project.findProperty("signing.key") as String?

publishing {
    publications.withType<MavenPublication> {
        artifactId =
            when (name) {
                "kotlinMultiplatform" -> "runanywhere-pairing"
                "androidRelease" -> "runanywhere-pairing-android"
                "jvm" -> "runanywhere-pairing-jvm"
                else -> "runanywhere-pairing-$name"
            }

        pom {
            name.set("RunAnywhere Pairing Module")
            description.set("Secure device pairing with QR code exchange and ECDH key negotiation for RunAnywhere SDK.")
            url.set("https://runanywhere.ai")
            inceptionYear.set("2024")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("runanywhere")
                    name.set("RunAnywhere Team")
                    email.set("founders@runanywhere.ai")
                    organization.set("RunAnywhere AI")
                    organizationUrl.set("https://runanywhere.ai")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/RunanywhereAI/runanywhere-sdks.git")
                developerConnection.set("scm:git:ssh://github.com/RunanywhereAI/runanywhere-sdks.git")
                url.set("https://github.com/RunanywhereAI/runanywhere-sdks")
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/RunanywhereAI/runanywhere-sdks")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    if (signingKey != null && signingKey.contains("BEGIN PGP")) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        project.hasProperty("signing.gnupg.keyName") || signingKey != null
    }
}

// Only publish Android release and metadata (skip JVM and debug)
tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication.name !in listOf("jvm", "androidDebug") }
}
