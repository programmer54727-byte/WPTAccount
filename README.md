# WPTAccount - Kotlin Multiplatform Dashboard

This is a modern **Kotlin Multiplatform (KMP)** application that works on both **Android** and **Desktop (JVM)**. It uses **Jetpack Compose** for the UI and **Supabase** as the backend.

## 🚀 Features
- **User Authentication:** Sign up with email, optional OTP verification, and secure Login.
- **Session Persistence:** Stay logged in across app restarts using `multiplatform-settings`.
- **Company Management:** Create and manage multiple companies.
- **Automated Accounting Setup:**
    - Automatically creates **34 standard Tally groups** for every company.
    - Automatically initializes default ledgers (**Cash** and **Profit & Loss A/c**).
    - **Smart Check:** Automatically initializes groups/ledgers for older companies when opened.
- **Comprehensive Transaction System:** Support for 8 standard voucher types (Sale, Purchase, Payment, Receipt, Contra, Journal, Credit Note, Debit Note) and Ledger viewing.
- **Responsive UI:**
    - **Desktop:** Permanent navigation sidebar for quick access.
    - **Mobile:** Adaptive navigation drawer with a dedicated menu button.
- **Analytics Dashboard:** Graphical representation of monthly Sales and Purchases using bar charts.
- **Cross-Platform:** 100% shared UI and business logic across Android and Desktop.
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
Copy and run the SQL commands from `shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql` in your **Supabase SQL Editor** to create the tables (`companies`, `groups`, `ledgers`, `vouchers`, `voucher_entries`) and RLS policies.

### 4. Run the Project
- **Android:** Select the `androidApp` configuration.
- **Desktop:** Select the `desktopApp` configuration.

## 🔒 Security
RLS is strictly enforced on all tables, ensuring users only see data belonging to companies they own. Tokens are stored securely using platform-native storage.
