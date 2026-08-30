# WPTAccount - Kotlin Multiplatform Dashboard

This is a modern **Kotlin Multiplatform (KMP)** application that works on **Android**, **Desktop (JVM)**, and **Web (Wasm)**. It uses **Jetpack Compose** for the UI and **Supabase** as the backend.

## 🚀 Features
- **User Authentication:** Secure email-based signup and login with sanitized error messages to prevent sensitive data leaks.
- **Session Persistence:** Stay logged in across app restarts using `multiplatform-settings`.
- **Company Management:** Create and manage multiple companies with professional, centered form layouts.
- **Automated Accounting Setup:**
    - Automatically creates **34 standard Tally groups** for every company.
    - Automatically initializes default ledgers (**Cash** and **Profit & Loss A/c**).
    - **Smart Check:** Automatically initializes groups/ledgers for older companies when opened.
- **Voucher Management (Sale, Purchase & Accounting):**
    - **Tally-Style Searchable Selectors:** Type names to filter parties, ledgers, or items instantly with full keyboard support (Arrows + Enter).
    - **On-the-fly Creation (Alt+C):** Create missing ledgers or stock items directly from the voucher entry screen using keyboard shortcuts.
    - **Advanced Bill-wise Details:** 
        - **Against Reference Lookup:** Real-time database lookup to select from existing "New Reference" entries when adjusting outstandings.
        - **"On Account" Support:** Flexible reconciliation for non-reference based payments.
        - **Sequential Processing:** Supports multiple bill-by-bill ledgers in a single voucher entry (Sequential Dialogs).
    - **Terminological Alignment:** Context-aware headers using **Invoice No.** and **Invoice Date** for Inventory/Purchase, and unified **Reference No** for outstandings.
    - **Smart Date Validation:** Built-in logic to ensure Invoice/Reference dates are not later than the Voucher Date, maintaining audit consistency.
    - **Flexible Tax Ledgers:** Replaced auto-GST with manual tax ledger selection from "Duties & Taxes", supporting custom rates (e.g., CGST 2.5%, SGST 2.5%) with automatic amount calculation.
    - **Atomic Saving Logic:** Integrated transaction handling that saves the voucher, stock movements, and ledger entries while updating inventory hand-levels in one robust step.
- **Ledger Management:**
    - **Groups & Ledgers:** Full lifecycle management (Add, View, Edit, Delete) of accounting groups and individual ledgers with aggregated totals.
    - **Smart Adaptive Form:** Intelligent ledger creation/editing dialog that dynamically shows/hides sections based on the chosen accounting group (Bank, Loans, Revenue, Assets, etc.).
    - **Automated Entry:** Intelligent auto-filling of mailing names from ledger names with optional manual override.
- **Inventory Management:**
    - **Units & Groups:** Full lifecycle management (Add, View, Edit, Delete) with aggregated summaries (Total Qty, Avg Rate, Total Value).
    - **Tally-Style Stock Summary:** Professional table layout displaying Particulars, HSN, and GST (formatted as %).
- **Dynamic Reporting Engine:**
    - **Live Monthly Summaries:** Real-time calculation of Inwards, Outwards, and rolling Closing Balances fetched directly from voucher data.
    - **Accounting Precision:** Rolling balance logic for ledgers that correctly handles Debit (Dr) vs. Credit (Cr) types.
    - **Period Awareness:** Selectable accounting periods (e.g., Financial Year) that automatically calculate "Effective Opening Balances" by summing all historical transactions prior to the period start date.
- **Professional Workflows:**
    - **Standardized Date Format:** Unified `DD/MM/YYYY` format throughout the application for displays and inputs, with seamless background conversion to database standards.
    - **Global Period Context:** Selectable accounting periods available directly on the Company Home page and reports to maintain a consistent view of financial data.
    - **Keyboard Navigation:** Full support for **Arrow keys** to select items and **Enter** to drill down project-wide.
    - **Intentional Interaction:** Implemented a "Tap to Select, Double-tap to Open" model to prevent accidental navigation on mobile and desktop.
    - **Period Selection:** Integrated date-range selection in reports to switch between financial years or custom periods instantly.
    - **System-Based Rotation:** Follows standard Android auto-rotate settings for all reports, giving users control over their viewing orientation.
    - **Robust Data Saving:** Comprehensive error handling (try-catch) on all save operations with detailed feedback to prevent app crashes.
- **Responsive & Modern UI:**
    - **Subtle Integrated Theme:** Modern Material 3 container-based palette that blends perfectly with the application background for a premium feel.
    - **Horizontal Table Scrolling:** Wide financial tables feature smooth horizontal scrolling in portrait mode, ensuring all data is accessible.
    - **Centered Desktop Forms:** All entry forms are centered with a `800.dp` max-width on desktop for a polished, professional experience.
- **Cross-Platform:** 100% shared UI and business logic across Android, Desktop, and Web.
- **Secure Backend:** Uses Supabase Auth and advanced Row Level Security (RLS) policies for data isolation.

## 🛠 Tech Stack
- **UI:** Jetpack Compose / Compose Multiplatform
- **Backend:** Supabase (Auth, Postgrest)
- **Database Logic:** SQL with Supabase RLS
- **Persistence:** Multiplatform Settings
- **Networking:** Ktor
- **Config:** BuildKonfig
- **Logging:** SLF4J with Logback (Desktop)

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
Copy and run the SQL commands from `shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql` in your **Supabase SQL Editor** to create the tables and RLS policies.

### 4. Run the Project
- **Android:** Select the `androidApp` configuration.
- **Desktop:** Select the `desktopApp` configuration.
- **Web:** Run `./gradlew :webApp:wasmJsBrowserRun`.

## 🔒 Security
RLS is strictly enforced on all tables, ensuring users only see data belonging to companies they own. Tokens are stored securely using platform-native storage.
