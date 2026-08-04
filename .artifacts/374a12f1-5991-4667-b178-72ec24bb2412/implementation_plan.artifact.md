# Implementation Plan - Summary Values for Units and Groups

The user wants to see summary information (Total Quantity, Average Rate, and Total Value) in the **Units** and **Groups** tabs, similar to how individual items are displayed.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)

- **UnitsTab Enhancement**:
    - Fetch all `StockItems` along with the units.
    - For each unit row, calculate:
        - **Total Quantity**: Sum of `current_quantity` of all items using this unit.
        - **Total Value**: Sum of (`current_quantity` * `opening_rate`) for all items.
        - **Average Rate**: `Total Value / Total Quantity`.
    - Update the UI to show these values in a table-like format.

- **StockGroupsTab Enhancement**:
    - Fetch all `StockItems` and `UnitOfMeasure` list.
    - For each group row, identify the items belonging to it.
    - **Logic**:
        - If all items in the group use the **same unit**: Show Total Quantity, Average Rate, and Total Value.
        - If items in the group use **different units**: Show **ONLY** the Total Value.
    - Update the UI to match the table format.

- **Visual Consistency**:
    - Use the same highlighted row design and column headers as the `StockItemsTab` for a professional, uniform look.

## User Review Required

> [!NOTE]
> For groups with different units (e.g., some items in "Kg" and some in "Pcs"), adding quantities together doesn't make sense, so only the total monetary value will be displayed in those cases.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify compilation.

### Manual Verification
1.  Navigate to **Inventory > Units**.
    - Verify that each unit row shows the combined quantity and value of all items assigned to it.
2.  Navigate to **Inventory > Groups**.
    - Create a group with items of the same unit. Verify it shows Qty, Rate, and Value.
    - Create a group with items of different units. Verify it shows only the total Value.
