# WPTAccount - Kotlin Multiplatform Dashboard

This is a modern **Kotlin Multiplatform (KMP)** application that works on **Android**, **Desktop (JVM)**, and **Web (Wasm)**. It uses **Jetpack Compose** for the UI and **Supabase** as the backend.

## 🚀 Features
- **User Authentication:** Sign up with email, optional OTP verification, and secure Login.
- **Session Persistence:** Stay logged in across app restarts using `multiplatform-settings`.
- **Company Management:** Create and manage multiple companies.
- **Automated Accounting Setup:**
    - Automatically creates **34 standard Tally groups** for every company.
    - Automatically initializes default ledgers (**Cash** and **Profit & Loss A/c**).
    - **Smart Check:** Automatically initializes groups/ledgers for older companies when opened.
- **Inventory Management:**
    - **Units & Groups:** Full lifecycle management for Units of Measure and Stock Groups.
    - **Tally-Style Stock Summary:** Professional table layout displaying Particulars, HSN, GST, and Closing Balances.
    - **Monthly Summary Drill-down:** Detailed monthly breakdown of Inwards, Outwards, and rolling Closing Balances.
    - **Smart Deletion:** Safe deletion logic with dependency checks (prevents deleting units or groups currently in use).
- **Professional Workflows:**
    - **Keyboard Navigation:** Full support for **Arrow keys** to select items and **Enter** to drill down.
    - **Android Rotation Control:** Automatically switches to **Landscape** mode for Inventory reports to ensure high readability on mobile.
- **Responsive UI:**
    - **Desktop:** Permanent navigation sidebar for quick access.
    - **Mobile:** Adaptive navigation drawer and screen-specific orientation handling.
- **Cross-Platform:** 100% shared UI and business logic across Android, Desktop, and Web.
- **Secure Backend:** Uses Supabase Auth and advanced Row Level Security (RLS) policies for data isolation.

## 🛠 Tech Stack
- **UI:** Jetpack Compose / Compose Multiplatform
- **Backend:** Supabase (Auth, Postgrest)
- **Database Logic:** SQL with Supabase RLS
- **Persistence:** Multiplatform Settings
- **Networking:** Ktor
- **Config:** BuildKonfig

## ⚙️ Setup Instructions

### 1. Prerequisites
- Android Studio (latest version)
- A Supabase account and project

### 2. Configure Local Properties
Create a `local.properties` file in the root directory:
```properties
supabase.url=YOUR_SUPABASE_PROJECT_URL
supabase.key=YOUR_SUPABASE_ANON_KEY
```

### 3. Database Schema
Copy and run the SQL commands from `shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql` in your **Supabase SQL Editor** to create the tables (`companies`, `groups`, `ledgers`, `vouchers`, `voucher_entries`, `units`, `stock_groups`, `stock_items`) and RLS policies.

### 4. Run the Project
- **Android:** Select the `androidApp` configuration.
- **Desktop:** Select the `desktopApp` configuration.
- **Web:** Run `./gradlew :webApp:wasmJsBrowserRun`.

## 🔒 Security
RLS is strictly enforced on all tables, ensuring users only see data belonging to companies they own. Tokens are stored securely using platform-native storage.
