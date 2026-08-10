# Implementation Plan - Support Capital Account Group

This plan updates the Ledger creation form to support the specific requirements of the **Capital Account** group. Based on research, Capital Accounts (like Proprietor's Capital or Partner's Capital) often require **Banking Details** for managing payouts and **PAN/Tax** details for owner identification.

## Research Findings for Capital Account
*   **Nature**: Liabilities (Equity).
*   **Mailing Details**: Used to store the owner's or partner's personal address.
*   **Banking Details**: Optional but frequently used in professional accounting to record the owner's bank account for withdrawals or profit distribution.
*   **Taxation**: PAN number is highly recommended for audit purposes.
*   **Difference from Sundry**: Does NOT typically require Bill-by-Bill tracking or Credit Limits.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- Update the conditional logic for the **Bank Account Details** section.
- It will now display if the selected group name contains **"Bank"** OR is exactly **"Capital Account"**.
- This ensures owners can store their personal banking information directly within their capital ledger.

## User Review Required

> [!TIP]
> Enabling bank details for Capital Accounts is standard practice in Tally and other ERPs to facilitate easy bank transfers to the business owners.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify UI logic.

### Manual Verification
1.  Open **Add Ledger**.
2.  Select **Capital Account** from the "Under" dropdown.
3.  **Expected Result**: The "Bank Account Details" section should appear.
4.  Verify that "Credit Control Details" (Bill-by-bill) remains hidden for this group.
