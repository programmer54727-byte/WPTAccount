# WPTAccount – Completion Roadmap (A to Z)

This document lists **everything that still needs to be completed** or improved, ordered by priority.

---

## Phase 1: Theme Consistency (Highest Priority)

The official corporate theme is already defined and applied in some places.  
Several screens still break the visual language.

### 1.1 Create / Edit Company Form (Critical)
**File**: `shared/.../createCompany.kt`

**Current Problem**:
- Uses plain Material3 Scaffold + basic OutlinedTextFields
- No cards, no left accent strips, no purple styling
- Feels completely different from Company List and rest of the app

**Required Changes**:
- [ ] Convert the form into card-based sections (General Info, Contact Details, Financial Details, Security, GST/HSN)
- [ ] Each section inside a white Card with left purple accent strip
- [ ] Use Primary Purple (`#7C4DFF`) for Save button and important actions
- [ ] Match spacing, typography and max-width (800.dp) used elsewhere
- [ ] Keep full keyboard navigation (Enter moves focus)
- [ ] Make the TopAppBar consistent with other screens

### 1.2 Other Forms to Audit & Fix
- [ ] Ledger Create / Edit form
- [ ] Inventory / Stock Item forms
- [ ] GST Details form
- [ ] Voucher Entry screens (Sale, Purchase, Accounting vouchers)
- [ ] Login & SignUp screens (apply brand colors)

### 1.3 Shared Theme Extraction (Recommended)
- [ ] Create `WptColors` object with all official colors
- [ ] Create a simple `WptTheme` / `WptCard` reusable component
- [ ] Replace all hardcoded `Color(0xFF7C4DFF)` etc. with the shared object

---

## Phase 2: Missing / Incomplete Features

### 2.1 Reports (Currently placeholders in navigation)
- [ ] Balance Sheet
- [ ] Profit & Loss
- [ ] Cash Flow
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
- [ ] Add a short `docs/README.md` that links all documents
- [ ] Keep README.md in sync with major feature changes

---

## Priority Order (Recommended Work Sequence)

1. **Fix Create Company form theme** (biggest visual inconsistency)
2. Extract shared `WptColors` + `WptCard` component
3. Apply the same card pattern to Ledger & Inventory forms
4. Audit and fix Voucher entry screens
5. Implement missing reports (Balance Sheet, P&L, Cash Flow)
6. Add Credit Note / Debit Note
7. Final UX polish + packaging

---

## Definition of Done

The project can be considered fully complete when:

- Every screen uses the official purple corporate theme
- Create/Edit Company form looks as polished as Company List
- All major accounting features work reliably
- Keyboard-first workflow is excellent on Desktop
- App can be cleanly distributed on Windows and Android

---

*Last updated: 20 September 2026*
