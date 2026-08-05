# Implementation Plan - Item Drill-down for Units and Groups

The user wants to be able to click on a Unit or a Stock Group and see a filtered list of items belonging to that category.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)

- **`UnitsTab` Update**:
    - Add state `selectedUnitForItems` (UnitOfMeasure?) to track drill-down.
    - When a unit row is clicked, set `selectedUnitForItems`.
    - If `selectedUnitForItems` is not null, display a filtered table of `StockItem`s.
    - Include a "Back" mechanism (button or header click) to return to the units list.

- **`StockGroupsTab` Update**:
    - Add state `selectedGroupForItems` (StockGroup?) to track drill-down.
    - When a group row is clicked, set `selectedGroupForItems`.
    - If `selectedGroupForItems` is not null, display a filtered table of `StockItem`s.
    - Include a "Back" mechanism to return to the groups list.

- **Reusable Component: `FilteredStockItemsList`**:
    - Extract the table logic from `StockItemsTab` into a reusable internal component.
    - This component will take a list of items and a title as parameters.
    - It will maintain the Tally-style table format, highlighting, and numeric formatting.
    - It will support the existing delete functionality.

## User Review Required

> [!NOTE]
> When drilling down, the view will switch from the summary (Totals) to the specific individual items. I will use the same professional table design for consistency.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify compilation.

### Manual Verification
1.  Navigate to **Inventory > Units**.
2.  Click on "Pcs".
3.  **Expected Result**: The view should switch to show only items that use the "Pcs" unit.
4.  Navigate to **Inventory > Groups**.
5.  Click on "Electronics".
6.  **Expected Result**: The view should switch to show only items inside the "Electronics" group.
7.  Verify the "Back" button works to return to the summary lists.
