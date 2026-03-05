# MARISOL.md — Pipeline Context for runanywhere-sdks

## Project Overview
Multi-platform AI SDK with Kotlin/Android for on-device inference.



## Build & Run
- **Language**: kotlin
- **Framework**: android
- **Docker image**: eclipse-temurin:17-jdk
- **Install deps**: `export ANDROID_HOME=/opt/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH && mkdir -p $ANDROID_HOME/cmdline-tools && cd /tmp && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdtools.zip && unzip -q cmdtools.zip && mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest && yes | sdkmanager --licenses 2>/dev/null || true; export ANDROID_HOME=/opt/android-sdk && export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH && sdkmanager "platforms;android-28" "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1 | tail -5; echo "sdk.dir=/opt/android-sdk" > /workspace/repo/Playground/android-use-agent/local.properties 2>/dev/null; echo "sdk.dir=/opt/android-sdk" > /workspace/repo/local.properties 2>/dev/null; true`
- **Run**: (see source code)

## Testing
- **Test framework**: junit
- **Test command**: `./gradlew test`
- **Hardware mocks needed**: no
- **Last result**: 0/0 passed

