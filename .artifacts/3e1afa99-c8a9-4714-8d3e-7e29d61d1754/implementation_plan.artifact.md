# Implementation Plan - GST Details Integration

This plan outlines the addition of a GST Details section for companies, including database schema updates, UI entry points, and a placeholder for future expansion.

## User Review Required

> [!IMPORTANT]
> - **Database Update**: I will add a new `gst_details` table to store all fields shown in your Tally screenshot (Registration Type, GSTIN, e-Way Bill status, etc.).
> - **Entry Point**: A "GST Detail" button will be added to the center of the Top Bar on the Company Home screen.
> - **Future Proofing**: The GST details screen will have a basic layout now, leaving space for more advanced settings as we progress.

## Proposed Changes

### 1. Database Schema (SQL)

#### [MODIFY] [supabasetableandpolicy.sql](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql)
- **New Table `gst_details`**:
    - `id`, `company_id` (foreign key)
    - `registration_status` (Active/Inactive)
    - `state`, `registration_type` (Regular/Composition)
    - `is_other_territory_assessee` (Boolean)
    - `gstin_uin` (Text)
    - `gstr1_periodicity` (Monthly/Quarterly)
    - `gst_username`, `filing_mode`
    - `eway_bill_applicable` (Boolean), `eway_bill_date` (Date), `eway_bill_intrastate` (Boolean)
    - `einvoice_applicable` (Boolean)
    - `registration_name`
- **RLS Policies**: Add policies to restrict access based on company ownership.

### 2. Data Model (Kotlin)

#### [NEW] [GstDetails.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/GstDetails.kt)
- Define `@Serializable` data class `GstDetails`.

### 3. UI and Navigation

#### [NEW] [gstDetailsForm.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/gstDetailsForm.kt)
- Create `GstDetailsScreen` with fields for the details mentioned above.
- Leave extra space as requested for future additions.

#### [MODIFY] [userHome.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/userHome.kt)
- Update the `TopAppBar` to include a centered "GST Detail" button.
- Pass the navigation callback `onGstDetailsClick`.

#### [MODIFY] [App.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/App.kt)
- Add `"gst_details"` screen state and routing logic.

## Verification Plan

### Manual Verification
1.  Run the updated SQL in Supabase.
2.  Open a company from the Company List.
3.  Verify the "GST Detail" button appears in the center of the Top Bar.
4.  Click the button and verify you land on the new (basic) GST details form.
5.  Verify data can be saved and retrieved (if implemented in this phase).
