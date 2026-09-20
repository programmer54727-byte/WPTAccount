# WPTAccount – Completion Roadmap (A to Z)

This document lists **everything that still needs to be completed** or improved, ordered by priority.

**Last major update**: 20 September 2026 (evening)

---

## Phase 1: Theme Consistency

### 1.1 Create / Edit Company Form
**File**: `shared/.../createCompany.kt`

- [x] Converted into card-based sections (General Info, Contact Details, Financial Details, Security, GST/HSN)
- [x] Each section uses `FormSectionCard` with left purple accent strip
- [x] Uses `WptColors` for all colors
- [x] Consistent spacing, typography and max-width (800.dp)
- [x] Keyboard navigation preserved
- [x] TopAppBar updated to match corporate style

**Status**: Completed

### 1.2 Shared Theme Extraction
- [x] Created `WptColors` object (`ThemeComponents.kt`)
- [x] Created reusable `FormSectionCard` component
- [ ] Replace remaining hardcoded colors across other screens with `WptColors`

### 1.3 Other Forms Still to Audit & Fix
- [ ] Ledger Create / Edit form
- [ ] Inventory / Stock Item forms
- [ ] GST Details form
- [ ] Voucher Entry screens (Sale, Purchase, Accounting vouchers)
- [ ] Login & SignUp screens (apply brand colors fully)

---

## Phase 2: Reports & Features

### 2.1 Reports
- [x] **Balance Sheet** (`BalanceSheetScreen.kt`) – hierarchical group totals implemented
- [x] **Profit & Loss** (`ProfitAndLossScreen.kt`) – hierarchical group totals implemented
- [x] **Cash Flow** (`CashFlowScreen.kt`) – Monthly Inflow / Outflow / Net Flow implemented
- [ ] Day Book (partially exists via voucher lists – needs polish)

### 2.2 Voucher Types
- [ ] Credit Note
- [ ] Debit Note

### 2.3 Company Home & Dashboard
- [ ] Ensure all cards and charts fully follow the corporate theme
- [ ] Verify period selection works correctly everywhere

### 2.4 Inventory & Ledgers
- [ ] Final polish of monthly summary cards
- [ ] Ensure Weighted Average Cost calculations are fully accurate
- [ ] On-the-fly creation (Alt+C) consistency across all screens

---

## Phase 3: Quality & Polish

### 3.1 Keyboard & UX
- [ ] Full Enter-as-Tab behavior on every form
- [ ] Consistent focus order
- [ ] Smart date field used everywhere dates are entered
- [ ] Searchable dropdowns behave identically across the app

### 3.2 Error Handling
- [ ] All user-facing errors are sanitized (no technical SQL/Ktor messages)
- [ ] Consistent error UI (snackbars or inline messages)

### 3.3 Responsive Behavior
- [ ] Forms remain usable on narrow screens
- [ ] Tables support horizontal scroll where needed
- [ ] Sidebar / Drawer transition is smooth

### 3.4 Packaging & Distribution
- [ ] Windows MSI / EXE packaging verified
- [ ] Android release build with proper versioning
- [ ] Icons and branding consistent on all platforms

---

## Phase 4: Documentation & Maintainability

- [x] VISION.md
- [x] THEME.md
- [x] DESIGN_STRUCTURE.md
- [x] COMPLETION_ROADMAP.md (this file)
- [x] docs/README.md
- [ ] Keep main README.md in sync with major feature changes

---

## Current Priority Order (Updated)

1. ~~Fix Create Company form theme~~ → **Done**
2. ~~Extract shared `WptColors` + `FormSectionCard`~~ → **Done**
3. ~~Implement Balance Sheet, P&L, Cash Flow~~ → **Done**
4. Apply theme consistently to Ledger, Inventory, Voucher & Auth screens
5. Add Credit Note / Debit Note
6. Polish Day Book & remaining UX details
7. Final packaging & distribution

---

## Definition of Done

The project can be considered fully complete when:

- Every screen uses the official purple corporate theme
- Create/Edit Company form looks as polished as Company List → **Achieved**
- All major accounting features work reliably
- Keyboard-first workflow is excellent on Desktop
- App can be cleanly distributed on Windows and Android

---

*Last updated: 20 September 2026 (evening)*
