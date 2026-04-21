# Doc Vault (Android)

Doc Vault is a secure Android app to store and manage personal images and documents in one place.

## Features

- Save images and documents into a private in-app vault
- Multi-select import support (images, documents, or both)
- Share directly into Doc Vault from other apps (`Share` / `Share multiple`)
- Duplicate detection using SHA-256 hash
- App lock with PIN and optional biometric unlock
- File source labels, sorting, filtering, and preview support

## Tech Stack

- Kotlin
- Jetpack Compose
- Room (SQLite)
- AndroidX Security + Biometric APIs

## Project Structure

- `app/src/main/java/com/docvault/app` - app logic, UI, and view models
- `app/src/main/java/com/docvault/app/data` - Room entities, DAO, and database
- `app/src/main/java/com/docvault/app/repository` - file import/storage orchestration
- `app/src/main/java/com/docvault/app/security` - hashing, PIN, biometric helpers

## Build

### Debug

```powershell
.\gradlew.bat assembleDebug
```

### Release

```powershell
.\gradlew.bat assembleRelease
```

Release APK output:

- `app/build/outputs/apk/release/app-release.apk`

## Requirements

- Android Studio (latest stable recommended)
- Android SDK + platform tools
- A connected Android device or emulator for install/testing

## Install on Device (ADB)

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

## License

This project is provided for personal and educational use.