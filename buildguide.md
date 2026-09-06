# Build Guide - WPTAccount

This guide provides step-by-step instructions on how to build the WPTAccount application for all supported platforms: Android, iOS, Windows (Desktop), and Web.

## 🛠 Prerequisites

Before you begin, ensure you have the following installed:
- **JDK 17 or higher** (Required for Compose Multiplatform).
- **Android Studio** (Latest version recommended).
- **Xcode** (Required for building the iOS application, macOS only).
- **Gradle** (Included in the project via Wrapper).

---

## 📱 Android

To build the Android application:

### Debug Build (APK)
```bash
./gradlew :androidApp:assembleDebug
```
The generated APK will be located in:
`androidApp/build/outputs/apk/debug/`

### Release Build (Bundle)
```bash
./gradlew :androidApp:bundleRelease
```
The generated AAB will be located in:
`androidApp/build/outputs/bundle/release/`

---

## 🍏 iOS (macOS Required)

Building for iOS requires a macOS machine with Xcode installed.

### Build and Run from Android Studio
1. Select the `iosApp` configuration in the run configurations dropdown.
2. Click the **Run** button.

### Build via Command Line
```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```
Then, open the Xcode project located in the `iosApp/` directory and build/archive as usual.

---

## 💻 Windows (Desktop)

To package the desktop application for distribution on Windows:

### Standalone Executable/Installer
```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```
The output (MSI, EXE, etc.) will be found in:
`desktopApp/build/compose/binaries/main/`

### Run locally
```bash
./gradlew :desktopApp:run
```

---

## 🌐 Web (WebAssembly)

The web application uses the modern Kotlin/Wasm target.

### Production Distribution
```bash
./gradlew :webApp:wasmJsBrowserProductionExecutableDistribution
```
The production-ready files (HTML, CSS, Wasm) will be located in:
`webApp/build/dist/wasmJs/productionExecutable/`

### Run locally for development
```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

---

## 🔧 Common Troubleshooting

- **Supabase Configuration**: Ensure you have created a `local.properties` file in the root directory with your `supabase.url` and `supabase.key` before building, otherwise the app will not be able to connect to the backend.
- **Gradle Sync**: If you encounter issues with dependencies, run **File > Sync Project with Gradle Files** in Android Studio.
- **Clean Build**: If the build fails unexpectedly, try cleaning the project:
  ```bash
  ./gradlew clean
  ```
