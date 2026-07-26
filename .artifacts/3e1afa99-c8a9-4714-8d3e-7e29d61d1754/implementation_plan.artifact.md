# Implementation Plan - Setup for Existing Companies

This plan addresses the need to initialize existing companies (those created before the automatic setup logic) with the 34 standard groups and default ledgers.

## User Review Required

> [!IMPORTANT]
> I will implement a "Smart Check" feature. When you select an existing company from your list, the app will automatically check if the accounting groups are present. If they are missing, it will set them up for you instantly without you having to do anything manually.

## Proposed Changes

### 1. Shared Setup Logic

#### [NEW] [companySetupHelper.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/companySetupHelper.kt)
- Create a shared function `initializeCompanySetup(companyId: String)` that contains the bulk insert logic for 34 groups and default ledgers (Cash, P&L).

### 2. Integration into Company Creation

#### [MODIFY] [createCompany.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/createCompany.kt)
- Replace the inline setup logic with a call to `initializeCompanySetup`.

### 3. Automatic Setup for Existing Companies

#### [MODIFY] [userHome.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/userHome.kt)
- Add a `LaunchedEffect` that checks the `groups` table for the current `company_id`.
- If no groups are found:
    - Show an "Initializing Company Setup..." loading indicator.
    - Call `initializeCompanySetup`.
    - Refresh the view once complete.

## Verification Plan

### Manual Verification
1.  **Old Company Test**: Select a company that was created previously (which doesn't have groups yet).
2.  **Verify**: You should see a brief loading message, and then the home screen should appear as normal.
3.  **Database Check**: Check the Supabase dashboard to confirm that the groups and ledgers have been added to that specific old company.
4.  **New Company Test**: Create a completely new company and verify it still works correctly.
