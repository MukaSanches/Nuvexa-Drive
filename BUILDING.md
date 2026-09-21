# Building Nuvexa Drive

## Requirements
- JDK 21
- Android SDK with API 37 compile platform
- Android build tools
- Gradle wrapper from this repository

## Generic GitHub build

    ./gradlew :app:assembleGenericRelease --stacktrace --no-daemon

The generic release APK produced by Gradle is intentionally unsigned unless a secure release-signing environment is supplied. A private keystore must never be committed to this public repository.

Target SDK is 36.
