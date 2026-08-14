# Implementation Plan - Support Income and Expense Groups

This plan updates the Ledger creation system to handle **Direct/Indirect Expenses** and **Direct/Indirect Income** with professional fields such as "Inventory Affected" and detailed GST configurations.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [Ledger.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/Ledger.kt)
- Add new fields to support revenue/expense accounting:
    - `inventory_affected`: Boolean (default false)
    - `cost_centres_applicable`: Boolean (default false)
    - `gst_applicable_type`: String (e.g., "Applicable", "Not Applicable")
    - `supply_type`: String (e.g., "Goods", "Services")
    - `hsn_sac_code`: String?
    - `hsn_sac_desc`: String?

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- **Smart Group Logic**:
    - Identify "Income" and "Expense" groups (Direct and Indirect).
    - **Hide**: Mailing, Bank, and Credit sections for these groups to keep the form clean.
    - **Show**:
        - **Inventory & Costing Section**: Includes toggles for "Inventory Affected" and "Cost Centres".
        - **GST Configuration Section**: Shows if GST is "Applicable". Includes HSN/SAC, Tax Rate, and Supply Type.
- **Improved Validation**: Ensure that if "Inventory Affected" is selected, the user is aware it will impact stock costs.

#### [MODIFY] [supabasetableandpolicy.sql](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql)
- Provide a migration script to add these new columns to the `ledgers` table.

## User Review Required

> [!TIP]
> This "Smart Form" approach ensures that if you are creating a "Salary Expense", you only see relevant options like "Cost Centres", without being bothered by "A/c Number" or "Mailing Address" fields.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify data model and UI logic.

### Manual Verification
1.  **Expense Creation**: Select "Indirect Expenses".
    - **Expected**: Mailing/Bank sections hide. "Inventory Affected" and GST sections appear.
2.  **Income Creation**: Select "Direct Income".
    - **Expected**: Similar behavior to Expenses.
3.  **Data Persistence**: Save an expense with HSN details and verify it stores correctly.
