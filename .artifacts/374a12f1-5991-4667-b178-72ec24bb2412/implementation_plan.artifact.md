# Implementation Plan - Screen Rotation Control for Android

The user wants the app to follow system auto-rotate settings normally, but specifically force landscape orientation when navigating to the **Inventory Management** (Stock) screen, even if the device's auto-rotate is disabled.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [NEW] [ScreenOrientation.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ScreenOrientation.kt)
- Define `enum class ScreenOrientation` with values: `UNSPECIFIED`, `LANDSCAPE`, `PORTRAIT`, `SENSOR`.
- Define `val LocalScreenOrientation = staticCompositionLocalOf<(ScreenOrientation) -> Unit>` to provide orientation control to Composables.

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Add a parameter `onOrientationRequest: (ScreenOrientation) -> Unit` to the `App` Composable.
- Wrap the app content in `CompositionLocalProvider(LocalScreenOrientation provides onOrientationRequest)`.
- Use `LaunchedEffect` monitoring `currentScreen`:
    - If `currentScreen == "inventory_management"`, request `LANDSCAPE`.
    - Otherwise, request `UNSPECIFIED` (to return to normal system behavior).

### [androidApp](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/androidApp)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/androidApp/src/main/kotlin/com/wpt/wptaccount/MainActivity.kt)
- Update `setContent { App(...) }` to pass a lambda that calls `setRequestedOrientation`.
- Map `ScreenOrientation` enum values to `ActivityInfo.SCREEN_ORIENTATION_*` constants.

## User Review Required

> [!IMPORTANT]
> Forcing landscape orientation can be jarring if the user doesn't expect it. This will only apply to the Android version of the app. Desktop and other platforms will ignore these orientation requests.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify common code.
- Run `./gradlew :androidApp:assembleDebug` to verify Android implementation.

### Manual Verification
1.  Launch the app on an Android device.
2.  Navigate between Landing, Login, and Company List. Verify it follows system rotate settings.
3.  Enter **Inventory Management** (Stock). Verify the screen immediately rotates to landscape even if auto-rotate is OFF.
4.  Navigate back. Verify it returns to portrait (or system default).
