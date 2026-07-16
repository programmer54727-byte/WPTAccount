# WPTAccount - Kotlin Multiplatform Dashboard

This is a modern **Kotlin Multiplatform (KMP)** application that works on both **Android** and **Desktop (JVM)**. It uses **Jetpack Compose** for the UI and **Supabase** as the backend.

## 🚀 Features
- **User Authentication:** Sign up with email, OTP verification, and Login.
- **Company Management:** Create and view your own companies.
- **Cross-Platform:** Shared UI and logic across Android and Desktop.
- **Secure Backend:** Uses Supabase Auth and Row Level Security (RLS) to keep user data private.

## 🛠 Tech Stack
- **UI:** Jetpack Compose / Compose Multiplatform
- **Backend:** Supabase (Auth, Postgrest)
- **Networking:** Ktor
- **Config:** BuildKonfig (for environment variables)

## ⚙️ Setup Instructions

### 1. Prerequisites
- Android Studio (latest version)
- A Supabase account and project

### 2. Configure Local Properties
This project uses `local.properties` to keep sensitive API keys out of version control. Create a file named `local.properties` in the root directory and add your Supabase credentials:

```properties
supabase.url=YOUR_SUPABASE_PROJECT_URL
supabase.key=YOUR_SUPABASE_ANON_KEY
```

### 3. Database Schema
Run the SQL found in `shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql` inside your Supabase SQL Editor to set up the necessary tables and Row Level Security policies.

### 4. Run the Project
- **Android:** Select the `androidApp` configuration and click Run.
- **Desktop:** Select the `desktopApp` configuration and click Run.

## 🔒 Security
Sensitive keys are stored in `local.properties`, which is excluded from Git via `.gitignore`. Row Level Security (RLS) is enabled on all tables to ensure users can only access their own data.
