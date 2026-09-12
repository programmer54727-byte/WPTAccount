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
x`
---

## 🌐 Web (WebAssembly)

The web application uses the modern Kotlin/Wasm target.

### Production Distribution
```bash
./gradlew :webApp:wasmJsBrowserDistribution
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

---

## 🔑 Configuration (local.properties)

The application uses **BuildKonfig** to securely inject Supabase credentials into the Kotlin code. These credentials must be provided in a `local.properties` file in the project root.

### Structure
Create a file named `local.properties` in the root folder of the project with the following content:

```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-key-here
```

### How it works
1. **Gradle Loading**: The `shared/build.gradle.kts` file reads this file using the `java.util.Properties` class.
2. **BuildKonfig Injection**: The keys are mapped to `buildConfigField` within the `buildkonfig { ... }` block.
3. **Kotlin Access**: During compilation, a class named `SupabaseConfig` is generated in the `com.wpt.wptaccount` package.
4. **Usage in Code**: The app accesses these values via `SupabaseConfig.URL` and `SupabaseConfig.KEY` when initializing the Supabase client.

> [!CAUTION]
> Never commit `local.properties` to version control (it is already in `.gitignore`). This protects your database credentials from being exposed.
