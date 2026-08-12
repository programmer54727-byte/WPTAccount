# Implementation Plan - Native System Auto-Rotate and Horizontal Scrolling

The user wants to stop forcing landscape orientation and instead let the app follow the system's auto-rotate settings. To ensure usability in portrait mode, wide tables will be updated with horizontal scrolling capability.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Update the `LaunchedEffect(currentScreen)` logic.
- Remove the code that forces `ScreenOrientation.LANDSCAPE` for `inventory_management`.
- Instead, set `onOrientationRequest(ScreenOrientation.UNSPECIFIED)` for all screens. This allows the system's auto-rotate toggle to control the app's orientation.

#### [MODIFY] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)
- **Tables Refactoring**:
    - Wrap the table header `Row` and the `LazyColumn` inside a `Column` that has `horizontalScroll(rememberScrollState())`.
    - Set a minimum width for the table (e.g., `minWidth = 800.dp`) to ensure that columns don't get crushed in portrait mode.
    - This applies to `UnitsTab`, `StockGroupsTab`, `StockItemsTab`, `FilteredStockItemsList`, and `StockItemMonthlySummary`.

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- **Tables Refactoring**:
    - Apply the same `horizontalScroll` and `minWidth` logic to all ledger tables.
    - This applies to `LedgerGroupsTab`, `LedgersTab`, `FilteredLedgersList`, and `LedgerMonthlySummary`.

## User Review Required

> [!NOTE]
> With horizontal scrolling, when you hold your phone in portrait mode, you will see the left part of the table (Particulars) and can swipe right to see Quantity, Rate, and Value. Rotating to landscape will still work as before, showing the full table at once.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify compilation.

### Manual Verification
1.  Launch the Android app.
2.  Open **Inventory > Items**.
3.  **Portrait Mode**: Verify that you can swipe left/right to see all columns. The screen should **not** rotate automatically.
4.  **Auto-Rotate ON**: Rotate the phone. Verify that the UI rotates to landscape and fits the whole table.
5.  Repeat for **Ledger** screens.
