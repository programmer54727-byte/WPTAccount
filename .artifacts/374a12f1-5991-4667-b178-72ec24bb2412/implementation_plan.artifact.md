# Implementation Plan - Professional Windows Installer Configuration

This plan updates the desktop application's distribution settings to ensure the app creates a Start Menu shortcut, appears in Windows Search, and has a professional icon. It also addresses the "Another version already installed" error.

## Problem Analysis

1.  **Start Menu/Search**: The current configuration does not explicitly tell the Windows installer to create a shortcut in the Start Menu.
2.  **Duplicate Installation Error**: Windows prevents installing an MSI if a product with the same ID is already present.
3.  **Icon**: While a `.png` is provided, Windows installers work best with `.ico` files for shortcuts and the Taskbar.

## Proposed Changes

### [desktopApp](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/desktopApp)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/desktopApp/build.gradle.kts)
- Update the `windows` block inside `nativeDistributions` to enable shortcuts and search visibility:
    - Set `menu = true` to add the app to the Start Menu.
    - Set `shortcut = true` to add a shortcut to the Desktop (optional, but common).
    - Set `menuGroup = "WPT Account"` to organize it in the Start Menu.
    - Use `upgradeUuid` (optional but good practice) to handle upgrades better.

#### [NEW] Icon Requirements
- Recommend converting `icon.png` to `icon.ico` (containing multiple sizes like 16, 32, 48, 256).

## Troubleshooting: "Another version already installed"
This error happens because an older version of the app was installed via the `.msi` or `.exe`.
**To fix this:**
1.  Go to **Control Panel > Uninstall a Program**.
2.  Search for "WPT Account".
3.  Right-click and **Uninstall**.
4.  Now run your new installer.

## User Review Required

> [!IMPORTANT]
> To make the app appear in Windows Search correctly, the `menu = true` setting is required. I will apply this change to your build script.

> [!TIP]
> For the best visual result, you should replace the `icon.png` with an `icon.ico` file. If you don't have one, the build system will try to convert it, but it's always better to provide a high-quality `.ico`.

## Verification Plan

### Automated Tests
- Run `./gradlew :desktopApp:package` to verify the installer builds without errors.

### Manual Verification
1.  **Uninstall** any previous version of "WPT Account" from your PC.
2.  Run the new `.exe` or `.msi` installer.
3.  Open the **Start Menu** and type "WPT Account".
4.  **Expected Result**: The app should appear in the search results with the correct name and icon.
