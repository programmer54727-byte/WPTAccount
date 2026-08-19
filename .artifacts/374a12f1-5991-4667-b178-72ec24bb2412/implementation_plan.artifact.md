# Implementation Plan - Dynamic Monthly Stock Summary

This plan details how to replace the static monthly stock summary with real data fetched from Sale and Purchase vouchers. This will provide an accurate view of item movements (Inwards/Outwards) and rolling balances month-by-month.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)
- **Data Fetching in `StockItemMonthlySummary`**:
    - Add a `LaunchedEffect` to fetch all `voucher_stock_items` associated with the current `item.id`.
    - Join with the `vouchers` table to get the `date` and `voucher_type`.
- **Processing Logic**:
    - Group the fetched entries by month (April to March).
    - Calculate **Inwards** (Total quantity and value from "Purchase" vouchers).
    - Calculate **Outwards** (Total quantity and value from "Sale" vouchers).
    - Calculate the **Rolling Closing Balance**:
        - `Opening Balance` (April) = `Item's initial opening balance`.
        - `Monthly Closing` = `Previous Closing` + `Monthly Inwards` - `Monthly Outwards`.
- **UI Update**:
    - Replace the empty strings in `SummaryRow` with the calculated monthly values.
    - Update the **Grand Total** row to show the sum of all movements during the financial year.

## User Review Required

> [!NOTE]
> The summary will follow the standard financial year (April to March). If you have made entries in other months, they will appear in their respective rows.

> [!TIP]
> This logic ensures that every time you save a Sale or Purchase, your Monthly Summary is updated instantly, providing a real-time audit trail for your inventory.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify data processing logic.

### Manual Verification
1.  Open **Inventory > Items > [Select an Item] > Monthly Summary**.
2.  **Initial State**: Should show the item's opening balance in the first row.
3.  **Transaction Test**:
    - Go to **Sale** and sell 5 units of this item in the month of August.
    - Go back to the **Monthly Summary**.
    - **Expected Result**: The "August" row should show 5 units in the "Outwards" column and the "Closing Balance" should decrease by 5.
4.  **Purchase Test**:
    - Record a **Purchase** for 10 units in September.
    - **Expected Result**: The "September" row should show 10 units in "Inwards".
