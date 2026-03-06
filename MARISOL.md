# MARISOL.md — Pipeline Context for runanywhere-sdks

## Project Overview
Multi-platform AI SDK with Kotlin/Android for on-device inference.

















## Build & Run
- **Language**: kotlin
- **Framework**: android
- **Docker image**: eclipse-temurin:17-jdk
- **Install deps**: `export ANDROID_HOME=/opt/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH && mkdir -p $ANDROID_HOME/cmdline-tools && cd /tmp && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdtools.zip && unzip -q cmdtools.zip && mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest && yes | sdkmanager --licenses 2>/dev/null || true; export ANDROID_HOME=/opt/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH && sdkmanager "platforms;android-28" "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1 | tail -5; echo "sdk.dir=/opt/android-sdk" > /workspace/repo/Playground/android-use-agent/local.properties 2>/dev/null; echo "sdk.dir=/opt/android-sdk" > /workspace/repo/local.properties 2>/dev/null; echo "sdk.dir=/opt/android-sdk" > /workspace/repo/examples/android/RunAnywhereWatch/local.properties 2>/dev/null; true; chmod +x /workspace/repo/examples/android/RunAnywhereWatch/gradlew 2>/dev/null; chmod +x /workspace/repo/Playground/android-use-agent/gradlew 2>/dev/null; true; cd /workspace/repo/examples/android/RunAnywhereWatch && if [ ! -f gradlew ]; then cp /workspace/repo/examples/android/RunAnywhereAI/gradlew . 2>/dev/null; cp /workspace/repo/examples/android/RunAnywhereAI/gradlew.bat . 2>/dev/null; mkdir -p gradle/wrapper && cp /workspace/repo/examples/android/RunAnywhereAI/gradle/wrapper/* gradle/wrapper/ 2>/dev/null; chmod +x gradlew; fi && cd /workspace/repo`
- **Run**: (see source code)












## Testing
- **Test framework**: junit
- **Test command**: `./gradlew test`
- **Hardware mocks needed**: no
- **Last result**: 187/191 passed












## Pipeline History
- *2026-03-05* — Implement: Implemented camera integration with multimodal vision for RunAnywhere Watch project. Changes made:
- *2026-03-05* — Implement: 307/308 tests pass (100%)
- *2026-03-05* — Implement: I have successfully implemented the following improvements to the Android project:
- *2026-03-05* — Implement: implemented Qwen3.5-0.8B model integration for the Android RunAnywhere SDK project. Changes made:
- *2026-03-06* — Implement: I have successfully implemented improvements to the Android project for Qwen3.5-0.8B model integrati
- *2026-03-06* — Implement: Summary of changes made to the Android RunAnywhere SDK project:
- *2026-03-06* — Implement: implemented the BLE Audio Stream Receiver module for the RunAnywhere Watch Android application.
- *2026-03-06* — Implement: Improve: local.properties, _exec_writes_
- *2026-03-06* — Implement: I have successfully implemented the STT and Summarization Pipeline integration for the RunAnywhere S
- *2026-03-06* — Implement: implemented the Transcriptions & Status UI feature for the Android app. Created TranscriptionScreen.
- *2026-03-06* — Implement: Improve: build.gradle.kts, settings.gradle.kts, WatchRuntime.kt (+7 more)
- *2026-03-06* — Implement: Implemented improvements to the Android SDK project including updating time formatting code to use m

