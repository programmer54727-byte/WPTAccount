# Implementation Plan - Handle System Back Button on Android

The user reported that pressing the system back button on Android closes the app instead of going back to the previous screen. This is because the app uses manual state-based navigation without handling back press events.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [NEW] [BackHandler.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/BackHandler.kt)
- Define an `expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)` to handle back press events in a cross-platform way.

#### [NEW] [BackHandler.android.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/androidMain/kotlin/com/wpt/wptaccount/BackHandler.android.kt)
- Implement `actual fun BackHandler` using `androidx.activity.compose.BackHandler`.

#### [NEW] [BackHandler.jvm.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/jvmMain/kotlin/com/wpt/wptaccount/BackHandler.jvm.kt)
- Implement an empty `actual fun BackHandler` (or optionally handle Escape key).

#### [NEW] [BackHandler.js.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/jsMain/kotlin/com/wpt/wptaccount/BackHandler.js.kt)
- Implement an empty `actual fun BackHandler`.

#### [NEW] [BackHandler.ios.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/iosMain/kotlin/com/wpt/wptaccount/BackHandler.ios.kt)
- Implement an empty `actual fun BackHandler`.

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Add `BackHandler` logic to navigate back based on the `currentScreen` state.
- Define a navigation hierarchy (e.g., `inventory_management` -> `company_home` -> `company_list`).

## User Review Required

> [!NOTE]
> I will implement a basic "back" logic where each screen knows its parent. For example, if you are in "Inventory", pressing back will take you to the "Company Home". If you are on the "Company List", pressing back will exit the app (normal Android behavior).

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify code structure.

### Manual Verification
1.  Deploy the app to an Android device.
2.  Navigate into a company and then into Inventory.
3.  Press the system back button.
4.  **Expected Result**: The app should go back to the Company Home screen instead of closing.
5.  Press back again.
6.  **Expected Result**: The app should go back to the Company List screen.
