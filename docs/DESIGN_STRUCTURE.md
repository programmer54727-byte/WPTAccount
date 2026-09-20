# WPTAccount – Design Structure & Architecture

## 1. High-Level Architecture

```
WPTAccount/
├── androidApp/          → Android entry point
├── desktopApp/          → Desktop (JVM) entry point
├── webApp/              → Web (Wasm) entry point
├── iosApp/              → iOS (future / experimental)
├── shared/              → 100% shared business logic + UI
│   └── src/commonMain/kotlin/com/wpt/wptaccount/
│       ├── App.kt                     → Root navigation & state
│       ├── createCompany.kt           → Company Create/Edit form
│       ├── companyList.kt             → Company selection
│       ├── userHome.kt                → Company Home
│       ├── companyDashboard.kt        → Dashboard
│       ├── ledgerManagement.kt        → Ledgers & Groups
│       ├── inventoryManagement.kt     → Stock / Inventory
│       ├── voucherEntry.kt            → Sale / Purchase vouchers
│       ├── AccountingVoucherEntryScreen.kt → Payment/Receipt/Contra/Journal
│       ├── navigationSidebar.kt       → Shared navigation drawer
│       ├── login.kt / signUp.kt       → Authentication
│       └── ... (models, utils, SQL)
└── docs/                → Project documentation (this folder)
```

**Principle**: Almost all UI and logic lives in the `shared` module. Platform modules are thin wrappers.

---

## 2. Navigation Structure

Root state is managed in `App.kt` using simple string-based screen routing + `rememberSaveable`.

### Main Flows

```
Landing → Login / SignUp → Company List
                              ↓
                    Create / Edit Company
                              ↓
                       Company Home
                    ┌─────┬─────┬─────┐
                    ↓     ↓     ↓     ↓
               Dashboard  Ledger  Inventory  Vouchers
```

### Screen Types (from navigationSidebar.kt)

- Home
- Dashboard
- Balance Sheet / Profit & Loss / Cash Flow (planned)
- Stock Summary
- Sale / Purchase / Payment / Receipt / Contra / Journal
- Credit Note / Debit Note (planned)
- Ledger
- Day Book
- GST Details
- Exit Company

---

## 3. Design System Structure

### Shared Components (Recommended to extract)

Currently many patterns are duplicated. Ideal structure:

```
shared/.../ui/
├── theme/
│   ├── WptColors.kt
│   ├── WptTheme.kt
│   └── Type.kt
├── components/
│   ├── WptCard.kt              → Standard card with left accent strip
│   ├── SectionHeader.kt
│   ├── SearchableDropdown.kt   (already exists)
│   ├── TallySearchableInput.kt (already exists)
│   ├── TallyDateField.kt
│   └── PrimaryButton.kt
```

### Current Reusable Pieces
- `SearchableDropdown`
- `TallySearchableInput`
- `AppNavigationDrawer`
- Country / State data from `countries.json`

---

## 4. Data Layer

- **Backend**: Supabase (Auth + PostgREST + RPC)
- **Local Persistence**: multiplatform-settings (session)
- **Atomic Operations**: PostgreSQL functions (`voucher_management_rpc.sql`)
- **Security**: Row Level Security (RLS) – users only see their own companies

Key models:
- `Company`
- `Ledger` / `Group`
- `Voucher` + bill-wise details
- Inventory models (Units, Stock Items, Groups)

---

## 5. UI Layout Rules

| Platform     | Layout Behavior                                      |
|--------------|------------------------------------------------------|
| Desktop      | Permanent left sidebar (260.dp) + centered content (max 800.dp for forms) |
| Mobile       | Modal drawer + full-width content                    |
| Forms        | Always centered with `widthIn(max = 800.dp)`         |
| Lists        | Card-based with left accent strip                    |
| Tables       | Horizontal scroll support on narrow screens          |

---

## 6. Keyboard-First Design

- **Enter** acts like Tab (moves focus to next field)
- Arrow keys for list selection
- `Alt + C` for on-the-fly creation of ledgers / items
- Smart date parsing (`1jun`, `1-6`, `1/6/26` → DD/MM/YYYY)

This must be preserved and extended to every form.

---

## 7. Future Improvements to Structure

1. Extract a proper `WptTheme` object (instead of hardcoded colors)
2. Create reusable `WptSectionCard` component
3. Move all screen routing to a more type-safe navigation system if complexity grows
4. Add proper feature modules if the codebase becomes very large

---

*This document describes the current and intended structure of the application.*
