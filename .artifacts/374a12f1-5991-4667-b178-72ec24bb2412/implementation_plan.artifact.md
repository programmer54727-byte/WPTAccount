# Implementation Plan - Professional Signed Balance Standard

This plan implements the industry-standard "Signed Balance" method for tracking accounting ledgers. By treating **Debit as Positive (+)** and **Credit as Negative (-)**, we ensure 100% accurate mathematical tracking of all transactions across all types of accounts (Assets, Liabilities, Income, Expenses).

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- **Voucher Save Fix**: Update the "Save" logic to initialize the `current_balance` as a signed number.
    - If `opening_balance_type` is **Cr**, save `current_balance` as a **negative** value.
    - This ensures the starting point for all mathematical updates is correct.

#### [MODIFY] [voucherEntry.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/voucherEntry.kt)
- **Universal Update Logic**: Simplify the `updateLedgerBalance` function to use the universal accounting rule:
    - **Debit Entry**: Always `current_balance + amount`.
    - **Credit Entry**: Always `current_balance - amount`.
- Remove the "Nature" check during saving, as signed numbers handle this automatically.

#### [MODIFY] [Utils.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/Utils.kt)
- Update `formatWithSign` to correctly interpret the signed numbers for the UI:
    - Positive Value -> Display as "**Dr**".
    - Negative Value -> Display as "**Cr**" (absolute value).
- This ensures the user sees professional Tally-style labels while the database performs fast, accurate math.

## User Review Required

> [!IMPORTANT]
> **Data Cleanup**: To make this work for your existing data, you **MUST** run the SQL update provided below in your Supabase Editor. This will convert your existing balances into signed numbers.

### Step 1: SQL Update (Zaroori)
Run this in Supabase SQL Editor:
```sql
-- Convert existing current_balances to signed numbers
UPDATE public.ledgers
SET current_balance = CASE
    WHEN opening_balance_type = 'Cr' THEN -ABS(opening_balance)
    ELSE ABS(opening_balance)
END;
```

## Verification Plan

### Manual Verification
1.  **Creation Test**: Create a ledger with **1000 Cr** opening balance.
2.  **Display Check**: Verify the list shows "**1000.00 Cr**".
3.  **Transaction Test**:
    - Create a **Sale** bill.
    - Add **CGST 9%** (Credit ledger) with amount **100**.
4.  **Result Check**:
    - The balance should update to **-1100** in DB.
    - The UI should show "**1100.00 Cr**".
    - This confirms the bill correctly "added" to the credit liability.
