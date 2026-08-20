# Implementation Plan - Fix Voucher Saving and Ledger Updates

This plan fixes the "Failed to save" error and ensures that ledger balances are accurately updated. It addresses the issues with Supabase update syntax and improves error visibility.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [voucherEntry.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/voucherEntry.kt)
- **Fix Update Syntax**:
    - Replace the problematic `set("column", value)` syntax with a safer map-based update: `update(mapOf("current_balance" to newBalance))`.
    - Apply this to both `updateLedgerBalance` and the stock quantity update loop.
- **Robust Nature Detection**:
    - Improve `updateLedgerBalance` to fetch the ledger and its group's nature directly from the database if the local `groups` list is empty or out of sync.
- **Safe Date Handling**:
    - Ensure the `dbDate` conversion handles potential formatting errors gracefully.
- **Detailed Error Reporting**:
    - Update the UI to show the full error message from Supabase so we can see if it's a "Missing Column" or "Constraint Violation" error.

## User Review Required

> [!IMPORTANT]
> **SQL Confirmation**: Please make sure you have run the latest SQL script (adding `reference_no` and `party_ledger_id` to `vouchers`) in your Supabase Dashboard. If these columns are missing, the save will fail.

> [!TIP]
> Once this fix is applied, if a save fails, the red text at the bottom will tell you **exactly** what went wrong (e.g., "column 'reference_no' does not exist").

## Verification Plan

### Manual Verification
1.  Open **Purchase Creation**.
2.  Add items and tax ledgers.
3.  Click **Save**.
4.  **Expected Result**:
    - If it saves: The app returns home, and the ledger balances are updated.
    - If it fails: A red message appears with a specific reason (not just "Unknown error").
