# Implementation Plan - Form Navigation and Company Dashboard

This plan addresses two main requirements: improving keyboard navigation in forms and creating a specific dashboard for a selected company.

## User Review Required

> [!IMPORTANT]
> - Forms (Login, Signup, Create Company) will now allow navigating between fields using the "Enter" key.
> - Clicking a company in the list will open a new Dashboard specifically for that company, showing "Sale" and "Purchase" options.

## Proposed Changes

### 1. Keyboard Navigation in Forms

#### [MODIFY] [login.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/login.kt)
- Add `FocusManager` usage.
- Update `OutlinedTextField`s with `KeyboardOptions(imeAction = ImeAction.Next)` and `ImeAction.Done`.
- Implement `KeyboardActions` to move focus or trigger login.

#### [MODIFY] [signUp.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/signUp.kt)
- Similar updates for the Signup form fields.

#### [MODIFY] [createCompany.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/createCompany.kt)
- Similar updates for all fields in the Create Company form.

### 2. Company Dashboard

#### [NEW] [companyDashboard.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/companyDashboard.kt)
- Create `CompanyDashboard` composable.
- Parameters: `company: Company`, `onBack: () -> Unit`.
- UI: Show company name in the top bar.
- Content: Sample buttons/cards for "Sale", "Purchase", "Payment", "Receipt", etc.

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Add `var selectedCompany by remember { mutableStateOf<Company?>(null) }`.
- Add `"company_dashboard"` to the screen navigation.
- Pass the selected company and navigation callbacks.

#### [MODIFY] [userCompany.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/userCompany.kt)
- Update `UserCompany` to accept `onCompanyClick: (Company) -> Unit`.
- Trigger `onCompanyClick` when a company card is clicked in the list.

## Verification Plan

### Manual Verification
1. **Forms**: Open Login, Signup, and Create Company forms. Verify that pressing "Enter" moves the cursor to the next field and submits on the last field.
2. **Dashboard**:
   - Create or select a company from the list.
   - Verify that the app navigates to a new screen showing the company name.
   - Verify that "Sale" and "Purchase" sections/buttons are visible.
