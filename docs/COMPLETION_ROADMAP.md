# WPTAccount – Completion Roadmap (A to Z)

This document lists **everything that still needs to be completed** or improved, ordered by priority.

**Last major code update**: 9 October 2026  
**Roadmap last reviewed**: 9 October 2026

**Overall Progress**: ~80% of planned features complete.

---

## Phase 1: Theme Consistency

### 1.1 Create / Edit Company Form
**File**: `shared/.../createCompany.kt`

- [x] Converted into card-based sections
- [x] `FormSectionCard` + `WptColors`
- [x] Keyboard navigation + TopAppBar

**Status**: Completed

### 1.2 Shared Theme Extraction
- [x] `WptColors` + `FormSectionCard` (`ThemeComponents.kt`)
- [ ] Replace remaining hardcoded colors across other screens

### 1.3 Other Forms Still to Audit & Fix
- [ ] Ledger Create / Edit form
- [ ] Inventory / Stock Item forms
- [ ] GST Details form
- [ ] Voucher Entry screens theme polish
- [ ] Login & SignUp screens (full brand colors)

---

## Phase 2: Reports & Features

### 2.1 Reports
- [x] **Balance Sheet**
- [x] **Profit & Loss**
- [x] **Cash Flow**
- [ ] Day Book polish

### 2.2 Voucher Types
- [x] **Credit Note** – routes + `VoucherEntryScreen` + correct entry_type (Party Credit / Sales Debit) + RPC stock IN
- [x] **Debit Note** – routes + `VoucherEntryScreen` + correct entry_type (Party Debit / Purchase Credit) + RPC stock OUT
- [ ] Optional UI polish (ScreenType highlight / Sales-Purchase ledger labels in entry screen)
- [ ] Clear remaining `/* TODO */` navigation stubs on secondary screens

### 2.3 Company Home & Dashboard
- [ ] Full corporate theme on all cards/charts
- [ ] Period selection verification everywhere

### 2.4 Inventory & Ledgers
- [ ] Monthly summary polish
- [ ] Weighted Average Cost verification
- [ ] Alt+C consistency

---

## Phase 3: Quality & Polish

### 3.1 Keyboard & UX
- [ ] Enter-as-Tab on every form
- [ ] Consistent focus order
- [ ] Smart date field everywhere

### 3.2 Error Handling
- [ ] Sanitized user-facing errors everywhere
- [ ] Consistent error UI

### 3.3 Responsive Behavior
- [ ] Narrow screens + table scroll

### 3.4 Packaging & Distribution
- [ ] Windows MSI/EXE verified
- [ ] Android release build

---

## Phase 4: Documentation

- [x] VISION / THEME / DESIGN_STRUCTURE / COMPLETION_ROADMAP / docs README
- [ ] Main README sync with new features

---

## Important – Supabase RPC

After pulling latest code, **re-run** `voucher_management_rpc.sql` in Supabase SQL Editor so Credit Note / Debit Note stock direction works:

- `Purchase` + `Credit Note` → stock **IN**
- `Sale` + `Debit Note` → stock **OUT**

---

## Current Priority Order

1. ~~Create Company theme~~ Done
2. ~~WptColors + FormSectionCard~~ Done
3. ~~Balance Sheet, P&L, Cash Flow~~ Done
4. ~~Credit Note & Debit Note accounting + RPC~~ Done (9 Oct 2026)
5. Theme polish on remaining forms
6. Day Book + UX polish
7. Packaging

---

*Last updated: 9 October 2026*
