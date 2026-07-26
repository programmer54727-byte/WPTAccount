# Implementation Plan - Inventory Management (Stock Setup)

To enable "Sale" and "Purchase" vouchers, we first need to implement Inventory Management. Similar to Ledgers needing Groups, Stock Items need Units of Measure (e.g., Pcs, Kg) and Stock Groups.

## User Review Required

> [!IMPORTANT]
> I will implement a complete Inventory setup consisting of:
> 1.  **Units of Measure**: To define how items are counted (e.g., Nos, Box, Kgs).
> 2.  **Stock Groups**: To categorize items (e.g., Electronics, Raw Materials).
> 3.  **Stock Items**: The actual products you will buy and sell.
>
> This is a foundational step before we can build the Sale Voucher.

## Proposed Changes

### 1. Database Schema Update

#### [MODIFY] [supabasetableandpolicy.sql](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql)
- **New Table `units`**: To store units like Pcs, Kgs, etc.
- **New Table `stock_groups`**: To categorize stock.
- **New Table `stock_items`**: To store products with their opening quantity and rate.
- Add RLS policies for all new tables.

### 2. Data Models (Kotlin)

#### [NEW] [InventoryModels.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/InventoryModels.kt)
- Define `@Serializable` data classes: `UnitOfMeasure`, `StockGroup`, and `StockItem`.

### 3. Inventory Management UI

#### [NEW] [inventoryManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/inventoryManagement.kt)
- Create a screen to:
    - View and create **Units**.
    - View and create **Stock Groups**.
    - View and create **Stock Items**.

### 4. Navigation Integration

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Add navigation routes for inventory management.

#### [MODIFY] [userHome.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/userHome.kt)
- Link the "Stock Summary" icon to the new Inventory screen.

## Verification Plan

### Manual Verification
1.  Run the updated SQL in Supabase.
2.  Navigate to "Stock Summary" from the app.
3.  Create a Unit (e.g., "Pcs").
4.  Create a Stock Group (e.g., "General").
5.  Create a Stock Item (e.g., "Keyboard") linked to the Unit and Group.
6.  Verify in Supabase that the item is saved correctly.
