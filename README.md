# WhatsApStatusSaver

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)

An Android application for viewing, saving, and sharing WhatsApp status media files.

## Overview

WhatsApp Status Saver allows users to access and manage WhatsApp status content stored locally on their Android devices. The application operates entirely offline without transmitting data to external servers.

## Technology Stack

- Kotlin
- Jetpack Compose
- Kotlin Coroutines
- Coil 3 with VideoFrameDecoder
- MVVM
- Material 3 glassmorphism UI
- JDK 21

## Building

```bash
git clone https://github.com/vinithreddybanda/WhatsapStatusSaver.git
cd WhatsapStatusSaver
./gradlew assembleGithubDebug
```

The GitHub Actions workflow builds the `githubDebug` APK with JDK 21 and publishes the APK as a workflow artifact.

## Permissions

Android 11 and above requires `MANAGE_EXTERNAL_STORAGE` for the local WhatsApp status directory. Older Android versions use `READ_EXTERNAL_STORAGE`.

## Project Structure

```text
app/src/main/java/com/vinithreddybanda/whatsapstatus/
├── MainActivity.kt
├── MainViewModel.kt
├── data/
│   └── StatusRepository.kt
└── model/
    └── Status.kt
```

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).

This application is not affiliated with, endorsed by, or sponsored by WhatsApp LLC. WhatsApp is a registered trademark of WhatsApp LLC.
