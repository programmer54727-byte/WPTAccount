# Implementation Plan - Support Current Assets Sub-groups

This plan updates the Ledger creation form to support the specific requirements of the **Current Assets** group and its sub-groups like **Loans & Advances (Asset)** and **Deposits (Asset)**.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- Update the conditional logic for the **Credit Control Details** section.
- It will now display if the selected group is:
    - **"Sundry Debtors"** or **"Sundry Creditors"**
    - **"Branch / Divisions"**
    - **"Loans & Advances (Asset)"**
    - **"Current Assets"**
- This enables **Bill-by-bill tracking** for loans and advances, allowing you to track individual repayment installments.
- Ensure **Mailing Details** and **Tax Registration** sections remain visible for these groups, as they are needed for party-related assets and tax-receivable ledgers (like TDS).

## User Review Required

> [!NOTE]
> Enabling "Bill-by-bill" for **Loans & Advances** is a professional accounting standard that helps in tracking which specific loan or advance is being settled during a transaction.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify UI logic.

### Manual Verification
1.  Open **Add Ledger**.
2.  Select **Loans & Advances (Asset)** from the "Under" dropdown.
3.  **Expected Result**: The "Credit Control Details" section should appear.
4.  Select **Current Assets**.
5.  **Expected Result**: The "Credit Control Details" section should remain visible.
6.  Select **Deposits (Asset)**.
7.  **Expected Result**: Only General, Mailing, and Tax sections should be visible (Credit Control hidden).
