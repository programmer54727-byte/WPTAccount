# WPTAccount – Project Vision

## 1. What is WPTAccount?

WPTAccount is a modern **Kotlin Multiplatform (KMP)** accounting application designed as a professional alternative to traditional desktop accounting software (especially Tally-style workflows).

It runs on:
- **Android**
- **Desktop (Windows / JVM)**
- **Web (Wasm)**

The entire UI and business logic is shared using **Compose Multiplatform** + **Supabase** backend.

---

## 2. Core Goal

Create a fast, keyboard-friendly, professional accounting system that feels familiar to Tally users while offering a modern corporate visual design and cloud-based multi-device access.

---

## 3. Target Users

- Small & medium businesses
- Accountants and bookkeepers
- Business owners who need multi-company accounting
- Users who prefer Tally-style data entry speed but want modern UI + cloud sync

---

## 4. What We Want to Achieve

### Primary Goals
- Full company lifecycle management (Create, Edit, Delete)
- Complete voucher system (Sale, Purchase, Payment, Receipt, Contra, Journal)
- Ledger & Group management with monthly summaries and drill-down
- Inventory / Stock management with Weighted Average Cost valuation
- Real-time dashboard with Sales vs Purchases analytics
- Period-aware reporting (Financial Year selection)
- Strong data consistency using Supabase RPC (atomic transactions)
- Excellent keyboard navigation (Enter = Tab, Arrow keys, Alt+C for quick create)

### Secondary Goals
- Consistent high-fidelity corporate theme across **all** screens
- Responsive design (mobile drawer + desktop permanent sidebar)
- Secure multi-user support with Row Level Security (RLS)
- Native packaging (Windows MSI/EXE, Android APK)

---

## 5. Design Philosophy

1. **Speed first** – Data entry must be faster than traditional software
2. **Visual clarity** – Corporate purple theme + clear hierarchy
3. **Consistency** – Same visual language and interaction patterns everywhere
4. **Reliability** – Atomic saves, proper error sanitization, no data loss
5. **Cross-platform** – One codebase, three platforms

---

## 6. Success Criteria

The project will be considered complete when:

- [ ] Every screen follows the official corporate theme
- [ ] Create/Edit Company form matches the rest of the app design
- [ ] All major voucher types work with bill-wise details
- [ ] Ledger, Inventory, and Dashboard reports are accurate
- [ ] Keyboard-first workflow is polished on Desktop
- [ ] App can be packaged and distributed on Windows & Android

---

*Last updated: September 2026*
