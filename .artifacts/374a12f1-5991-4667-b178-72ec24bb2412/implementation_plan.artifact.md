# Implementation Plan - Professional Group-Specific Ledger Logic

This plan implements a comprehensive "Smart Form" system that categorizes all 34 default accounting groups and shows only the relevant professional fields for each. This ensures the app behaves exactly like professional ERP software (TallyPrime).

## Comprehensive Group Categorization

| Category | Included Groups | Visible Sections |
| :--- | :--- | :--- |
| **Banking** | Bank Accounts, Bank OCC A/c, Bank OD A/c | Bank Details, Mailing Details |
| **Parties & Loans** | Sundry Debtors/Creditors, Branch/Divisions, Current Liabilities, Loans & Advances (Asset), Secured/Unsecured Loans, Loans (Liability) | **Credit Control**, Bank Details, Mailing Details, Tax Registration (PAN) |
| **Revenue/Nominal** | Purchase/Sales Accounts, All Direct/Indirect Income & Expenses | **Inventory Affected**, Cost Centres, Statutory Details |
| **Capital** | Capital Account | Bank Details, Mailing Details, Tax Registration (PAN) |
| **Fixed Assets** | Fixed Assets, Investments | Statutory Details (Type: Capital Goods), Mailing Details |
| **Internal/Minimal** | Cash-in-Hand, Provisions, Reserves & Surplus, Retained Earnings, Suspense A/c | General Information, Opening Balance only |

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [ledgerManagement.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/ledgerManagement.kt)
- **Implement Category Logic**: Create helper flags inside the `LedgerCreation` dialog:
    - `isBankRelated`
    - `isPartyOrLoan`
    - `isRevenueRelated`
    - `isAssetRelated`
    - `isInternalOnly` (True for Cash, Provisions, Reserves, etc.)
- **Update Conditional UI**:
    - Use these flags to wrap the `Column` sections for Mailing, Tax, Bank, Credit, and Inventory.
    - **Inventory Affected Toggle**: Specifically show and enable this for Purchase, Sales, and Direct Expenses.
    - **Bank Details**: Enable for Parties and Loans (to store payment/disbursement info).
- **Default State Handling**:
    - If `isRevenueRelated` is true, automatically set `inventoryAffected` to `true` for Purchase/Sales.

## User Review Required

> [!IMPORTANT]
> **Loans & Liabilities**: I am enabling "Credit Control" (Bill-by-bill) and "Bank Details" for all Loan groups. This allows you to track loan disbursements and set repayment periods, which is essential for audit-ready accounting.

> [!TIP]
> After this update, if you create a "Wages" ledger (Direct Expense), you will see "Inventory Affected" and "GST". If you create "Petty Cash", you will only see the "Name".

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to verify UI logic.

### Manual Verification
1.  **Test Loan**: Select "Secured Loans". Verify Bank and Mailing details appear.
2.  **Test Purchase**: Select "Purchase Accounts". Verify "Inventory Affected" appears and defaults to Yes.
3.  **Test Asset**: Select "Fixed Assets". Verify GST details appear.
4.  **Test Internal**: Select "Reserves & Surplus". Verify Mailing/Bank/Tax sections are hidden.
