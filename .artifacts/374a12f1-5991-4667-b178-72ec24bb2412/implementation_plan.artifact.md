# Implementation Plan - Fix Ledger Balance Updates

The user reported that ledger balances (like CGST 9%) are not updating after saving a Sale or Purchase voucher. This is because the app currently creates the accounting entries but does not update the `current_balance` summary column in the `ledgers` table.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [voucherEntry.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/voucherEntry.kt)
- **Implement `updateLedgerBalance` Helper**:
    - This internal function will fetch the latest balance of a ledger, determine its group's nature (Asset, Liability, etc.), and calculate the new balance based on the Debit/Credit entry.
    - **Logic**:
        - **Asset / Expense**: New Balance = `Current + Debit - Credit`.
        - **Liability / Income**: New Balance = `Current - Debit + Credit`.
- **Integrate into Save Logic**:
    - Update the **Party Ledger** balance.
    - Update the **Sales/Purchase Ledger** balance.
    - Update all **Tax Ledger** balances.
- **Atomic Updates**: Similar to the stock item fix, we will fetch the latest balance immediately before updating to ensure accuracy in multi-user environments.

## User Review Required

> [!IMPORTANT]
> **Balance Synchronization**: This change ensures that the "Closing Balance" you see in your Ledger list is always in sync with your actual vouchers.

> [!NOTE]
> For "Duties & Taxes" (like CGST 9%), these are typically Liabilities. A Credit entry (which happens during a Sale) will increase the balance (money owed to the government).

## Verification Plan

### Manual Verification
1.  Check the current balance of "CGST 9%" (e.g., 0.00).
2.  Create a **Sale** voucher.
    - Add an item for 1000.
    - Add "CGST 9%" ledger row (Amount: 90).
3.  Save the voucher.
4.  Go to **Ledgers**.
5.  **Expected Result**: "CGST 9%" should now show a balance of **90.00**.
6.  Repeat for a **Purchase** to verify the balance decreases/increases correctly.
