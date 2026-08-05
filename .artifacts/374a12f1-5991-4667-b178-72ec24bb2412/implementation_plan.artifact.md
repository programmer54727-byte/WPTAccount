# Implementation Plan - Mobile UI Responsiveness and List Density

The user wants the inventory list screens to be more responsive on mobile, displaying more items at once. Currently, only about 3 items are visible; the goal is to see at least 4 or more by optimizing padding and layout weights.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)

- **Global Adjustments**:
    - Reduce vertical padding in `Surface` and `Row` for list items from `4.dp` or `2.dp` to `1.dp`.
    - Use `BoxWithConstraints` to detect screen width and apply conditional styling.

- **`UnitsTab` & `StockGroupsTab`**:
    - Reduce header heights.
    - Scale column widths (Quantity, Rate, Value) based on available width. On narrow mobile screens, prioritize Quantity and Value.
    - Reduce font sizes for supporting text if necessary.

- **`StockItemsTab`**:
    - Optimize the "Closing Balance" sub-columns. Instead of fixed `80.dp` or `100.dp`, use smaller fixed widths for mobile (e.g., `60.dp`, `80.dp`).
    - Adjust the `Particulars` weight to give more room to the numeric data on small screens.
    - Remove or shrink the spacer for the delete button on mobile to save horizontal space.

## User Review Required

> [!NOTE]
> Reducing font size to `MaterialTheme.typography.bodySmall` globally for the list rows will help fit more items vertically.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify compilation.

### Manual Verification
1.  Launch the app on an Android emulator or device.
2.  Navigate to **Inventory > Items**.
3.  **Expected Result**: At least 4-5 items should be visible on the screen without scrolling.
4.  Verify that text remains readable and columns don't overlap.
5.  Check both Portrait and Landscape (Landscape was forced for some screens, verify it still looks good).
