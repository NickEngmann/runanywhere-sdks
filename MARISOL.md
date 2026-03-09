# MARISOL.md — Pipeline Context for runanywhere-sdks

## Project Overview

| Field | Value |
|-------|-------|
| **Name** | RunAnywhere SDKs — Multi-platform AI inference SDK |
| **Architecture** | Kotlin Multiplatform (KMP) — commonMain, jvmMain, androidMain, jvmAndroidMain |
| **Build** | Gradle 8.11.1, Kotlin 2.1.21, compileSdk 35/36, JVM target 17 |
| **AI Backends** | llama.cpp (LLM/VLM), ONNX (STT/TTS/VAD) |
| **Models** | Qwen3.5-0.8B/2B/4B (GGUF Q4_K_M), Whisper, Piper TTS |
| **Platforms** | Android phone, Android watch (Wear OS), iOS, Web, IntelliJ plugin |

## Build & Run

### Watch JVM Tests (pure JUnit, no Android SDK needed)
```bash
cd examples/android/RunAnywhereWatch
./gradlew :tests-jvm:test --no-daemon
```

### SDK JVM Tests (needs Android SDK for configuration)
```bash
./gradlew :runanywhere-kotlin:jvmTest --no-daemon
```

### App Compilation (needs Android SDK)
```bash
cd examples/android/RunAnywhereWatch
./gradlew :app:compileDebugKotlin --no-daemon
```

## Testing

### Test Suites

| Suite | Location | Tests | Coverage |
|-------|----------|-------|----------|
| Watch App | `examples/android/RunAnywhereWatch/tests-jvm/` | 334 | Controllers, managers, sensors, API compat, UI, MCP server |
| SDK Audio Streaming | `sdk/runanywhere-kotlin/src/jvmTest/` | 53 | CRC32, chunk encode/decode, BLE streamer, extensions |

### Key Test Patterns
- **tests-jvm module**: Pure JVM (no AAPT2/Android SDK). Uses `kotlin("jvm")` plugin
- **Source mirroring**: App classes tested in tests-jvm are duplicated in `tests-jvm/src/main/kotlin/`
- **SDK jvmTest**: KMP source set, needs Android SDK configured but runs on JVM
- **Coroutines**: Tests use `kotlinx.coroutines.test`, `runBlocking`, `coroutineScope { async {} }`

### Docker Environment
- **Image**: `lotus-android-sdk:latest` or golden `lotus-golden:kotlin-android-*`
- **JDK**: Temurin 17 (required by Gradle toolchain)
- **Android SDK**: `/opt/android-sdk` (platforms, build-tools, cmdline-tools)
- **Resources**: 8192MB memory, 4 CPUs (Gradle is memory-hungry)

## Adaptive UI

All screens use `ScreenConfig.kt` with `AdaptiveLayout` composable — auto-detects watch (<230dp) vs phone via `BoxWithConstraints`.

| Property | Watch | Phone |
|----------|-------|-------|
| Time font | 34sp | 64sp |
| Body font | 11sp | 14sp |
| Primary button | 36dp | 64dp |
| Edge padding | 8dp | 16dp |
| Camera preview | 120dp | 280dp |
| Camera button | Top bar | Bottom bar (beside mic) |
| Transcription lines | 2 max | 4 max |

Watch screens use 20dp horizontal padding for round safe area. Camera's Quick Ask button is inline with capture on watch.

## Screenshot Testing

Paparazzi renders actual Compose UI to PNG via Android's `layoutlib` — no emulator needed.

```bash
cd examples/android/RunAnywhereWatch
./gradlew :app:recordPaparazziDebug --no-daemon   # generate screenshots
./gradlew :app:verifyPaparazziDebug --no-daemon    # verify against golden files
```

### Watch Screenshots (WEAR_OS_SMALL_ROUND)
| Screenshot | Description |
|-----------|-------------|
| `watchFace_watch_default` | Time (34sp), date, seconds, MIC + C buttons, AI status dot |
| `transcription_watch_empty` | Empty state — "Tap the mic to start" |
| `transcription_watch_withEntries` | Cards with timestamps, source badges within round safe area |
| `transcription_watch_lowConfidence` | 65% confidence indicator on voice transcription |
| `camera_watch_viewfinder` | 120dp preview, ? + capture buttons at bottom, X close |

### Phone Screenshots (PIXEL_5)
| Screenshot | Description |
|-----------|-------------|
| `watchFace_phone_default` | Full layout — 64sp time, CAM top-right, MIC bottom-center |
| `transcription_phone_empty` | Empty state with full header |
| `transcription_phone_withEntries` | 4 cards — voice, system source badges, 72% confidence |
| `camera_phone_viewfinder` | 280dp preview, Quick Ask sidebar, large capture button |

Golden files: `app/src/test/snapshots/images/`

**Note**: Paparazzi uses x86_64 `layoutlib` — runs in CI (ubuntu-latest) but NOT on ARM64 (Jetson)

## CI/CD

GitHub Actions — 3 workflows:

### Tests (`.github/workflows/watch-tests.yml`) — 2 jobs:
1. **Watch App JVM Tests** — `./gradlew :tests-jvm:test`, 334 tests
2. **SDK JVM Tests** — `./gradlew :runanywhere-kotlin:jvmTest`, 53 tests (needs Android SDK setup)

### Screenshots (`.github/workflows/screenshots.yml`) — 1 job:
1. **Generate Screenshots** — `./gradlew :app:recordPaparazziDebug`, 9 screenshots (5 watch + 4 phone) uploaded as artifacts

## Known Issues

- **AAPT2 x86-only**: `assembleDebug` crashes on ARM64 (Jetson Thor). Use tests-jvm module instead
- **Pre-existing SDKTest.kt**: `testSDKInitialization()` fails without native C++ libs (expected in JVM-only env)
- **CRC32 Kotlin gotcha**: `0xFFFFFFFF` and `0xEDB88320` are Long literals — must use `.toInt()` for Int context
- **KMP source set naming**: `jvmTest` is the correct source set name, NOT `test-jvm` (which is ignored)
- **Coroutine scope**: `async` must be called inside `coroutineScope {}` or `runBlocking {}`, not top-level
- **Getter naming**: Kotlin auto-generates `getX()` for `val x` — test helper methods must use different names to avoid "accidental override"
- **Paparazzi x86-only**: Uses Android `layoutlib` (x86_64 native). Cannot run on ARM64 — CI only
- **gradle.properties gitignored**: Root `.gitignore` blocks `gradle.properties`. Use `git add -f` for project-level properties
- **Adaptive UI threshold**: `BoxWithConstraints` < 230dp = watch. Wear OS Small Round = ~192dp, phones = 360dp+

## Qwen3.5-0.8B On-Device LLM

Fully implemented via llama.cpp C++ backend:
- **Model registration**: `ModelList.kt` — `qwen3.5-0.8b-q4_k_m` (600MB), `qwen3.5-2b-q4_k_m` (1.5GB), `qwen3.5-4b-q4_k_m` (2.8GB)
- **C++ inference**: `rac_vlm_llamacpp.cpp` — GGUF load, tokenize, generate, streaming callbacks
- **JNI bridge**: `LlamaCPPBridge.kt` → `librac_backend_llamacpp_jni.so`
- **Chat template**: Auto-detected from model metadata (supports Qwen, SmolVLM, LLaVA, chatml)
- **Playground**: `android-use-agent/` uses Qwen3-4B for agentic loops with tool calling

## Pipeline History

| Date | Phase | Result |
|------|-------|--------|
| 2026-03-05 | Initial tests | 307/308 watch tests pass |
| 2026-03-08 | PR #26 (audio) | Audio streaming source added, tests broken (wrong source set, API mismatch) |
| 2026-03-08 | PR #27 (watch) | MCP server, transcription UI, build fixes. 334 tests pass |
| 2026-03-09 | PR #26 fix | Tests rewritten for actual API, CRC32 Int/Long fix, SDKTest fix. 53 SDK + 334 watch = 387 total |
| 2026-03-09 | CI expanded | Added SDK JVM test job alongside watch tests |
| 2026-03-09 | PRs #26 + #27 merged | Both PRs merged to main, all CI green |
| 2026-03-09 | PR #28 (screenshots) | Paparazzi screenshot tests — 8 real UI renders of all watch screens |
| 2026-03-09 | PR #29 (responsive UI) | Adaptive watch/phone layouts, ScreenConfig.kt, 9 screenshots (5 watch + 4 phone) |

## Notes

- Kotlin Multiplatform: `commonMain` → shared, `jvmAndroidMain` → JVM+Android shared, `androidMain` → Android-only
- `runanywhere-commons/` is C++ (llama.cpp, ONNX backends) compiled via CMake + JNI
- Watch app uses Wear OS Compose + Jetpack Compose Material3
- MCP Server: JSON-RPC 2.0 over TCP (port 8400), 4 tools, zero external dependencies
- `repo_cache` directory should NOT be committed (pipeline artifact, gitignored)
