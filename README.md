# SimpleSprint (Android Only)

This repository is now standardized as an Android-only project.

## Project Structure

- `app/`
  - Android application module (`:app`)
  - Kotlin source, resources, tests, manifest
- `crates/sprint-sync-protocol/`
  - Shared Rust protocol crate
- `crates/sprint-sync-protocol-jni/`
  - Rust JNI bridge used by Android
- `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `build.gradle.kts`
  - Single canonical Gradle root entrypoint

## Standard Commands

- Build JNI libraries:
  - `cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -P 24 -o app/src/main/jniLibs build --release -p sprint-sync-protocol-jni`
- Build debug APK:
  - `gradlew.bat :app:assembleDebug`
- Build release APK:
  - `gradlew.bat :app:assembleRelease`
- Build + install + launch debug:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\BuildInstall-SimpleSprintDebug.ps1`
- Install existing debug APK:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\Install-SimpleSprintDebug.ps1`
- Install existing release APK:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\Install-SimpleSprintRelease.ps1`

## Conventions

- Use root Gradle (`./gradlew` or `gradlew.bat`) only.
- Treat `app` as the only Android module.
- Keep app id aligned to `simple.sprint`.
- Do not reintroduce desktop/Tauri workspace files.
