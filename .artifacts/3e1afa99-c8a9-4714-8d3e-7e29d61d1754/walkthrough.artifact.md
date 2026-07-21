# Walkthrough - OTP Verification Added to Signup

I have updated the Signup flow to include a mandatory email verification step using a 6-digit OTP code.

## Changes Made

### 1. Two-Step Signup Flow
- **Step 1 (User Details)**: User enters their Full Name, Email, and Password. Clicking "Sign Up" triggers the Supabase registration and sends an OTP to their email.
- **Step 2 (Verification)**: The UI dynamically switches to a verification screen where the user enters the 6-digit code received.

### 2. UI/UX Enhancements
- **Dynamic Headers**: The screen title and header text change based on whether the user is registering or verifying.
- **OTP Field**: A specialized 6-digit entry field with centered text for better readability.
- **Flexible Navigation**: Added a "Change Email" button that allows users to go back to the first step if they made a typo in their email address.

### 3. Backend Integration
- **Supabase Auth**: Integrated `supabase.auth.verifyEmailOtp` with `OtpType.Email.SIGNUP` to validate the session.
- **Success Handling**: On successful verification, the user is automatically navigated to the Dashboard.

## Verification Results
- **Build**: Successfully built the `:desktopApp` to ensure no syntax or dependency errors.
- **Logic**: The state management correctly handles the transition between the signup form and the verification form.

> [!IMPORTANT]
> **Next Steps**: Please ensure your Supabase dashboard has the "Confirm Email" setting enabled and that the email template contains the `{{ .Token }}` variable.
