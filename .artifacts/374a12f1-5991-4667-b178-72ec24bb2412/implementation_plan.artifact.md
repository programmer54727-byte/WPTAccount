# Implementation Plan - Secure Error Handling for Authentication

The user reported that failed login attempts display raw Supabase error messages, which include sensitive information such as the Supabase API URL and public API key. This is a security risk. This plan will replace raw error messages with user-friendly, non-sensitive messages across the authentication and company management flows.

## Proposed Changes

### [shared](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared)

#### [MODIFY] [login.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/login.kt)
- Sanitize the error message in the `catch` block.
- If the error contains "invalid_credentials", show "Invalid email or password".
- Otherwise, show a generic "Login failed. Please try again."

#### [MODIFY] [signUp.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/signUp.kt)
- Sanitize error messages for both Signup and Verification flows.
- Map common Supabase Auth errors (like "user_already_exists") to clean messages.
- Hide technical details (URL, headers).

#### [MODIFY] [companyList.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/companyList.kt) and [createCompany.kt](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/createCompany.kt)
- Replace `e.message` usage with generic error messages when displaying to the UI.
- Keep detailed logging in `println` or `Log` (if available) for debugging, but never show it to the user in a `Text` component.

## User Review Required

> [!WARNING]
> By sanitizing these messages, you will no longer see the technical reason for a failure in the UI (like a 404 or specific header issue). For development debugging, I recommend checking the console output (`println`) instead of the app screen.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:compileKotlinJvm` to ensure no syntax errors.

### Manual Verification
1.  Try to log in with a wrong password.
    - **Expected Result**: See "Invalid email or password" instead of the full Supabase URL/API Key.
2.  Try to sign up with an existing email.
    - **Expected Result**: See a clean error message.
3.  Check other screens for any remaining raw error displays.
