# WPTAccount – Official Theme Specification

This document defines the **complete visual language** of the application.  
Every new screen or component **must** follow these rules.

---

## 1. Brand Colors

| Role                    | Hex Code   | Compose Color                  | Usage                                      |
|-------------------------|------------|--------------------------------|--------------------------------------------|
| **Primary Accent**      | `#7C4DFF`  | `Color(0xFF7C4DFF)`            | Buttons, FABs, active states, accent strips, icons |
| **Navigation BG**       | `#F5F3F8`  | `Color(0xFFF5F3F8)`            | Sidebar / Drawer background                |
| **App Surface**         | `#F8F9FA`  | `Color(0xFFF8F9FA)`            | Main background of screens                 |
| **Card Background**     | `#FFFFFF`  | `Color.White`                  | All cards and elevated surfaces            |
| **Primary Text**        | `#1D1B20`  | `Color(0xFF1D1B20)`            | Titles and important text                  |
| **Secondary Text**      | `#49454F`  | `Color(0xFF49454F)`            | Subtitles, secondary labels                |
| **Divider**             | `#E0E0E0`  | `Color(0xFFE0E0E0)`            | Horizontal dividers                        |
| **Error / Delete**      | `#D32F2F`  | `Color(0xFFD32F2F)`            | Delete actions, error states               |

### Transaction Type Colors (Accent Strips)

| Type       | Color Suggestion     |
|------------|----------------------|
| Sale       | Blue                 |
| Purchase   | Deep Purple          |
| Receipt    | Red                  |
| Contra     | Teal                 |
| Journal    | Amber                |

---

## 2. Typography

Use Material 3 typography scale:

- **Screen Title**: `headlineMedium` + `FontWeight.Bold`
- **Section Header**: `titleMedium` + `FontWeight.Bold`
- **Card Title**: `titleLarge` + `FontWeight.Bold`
- **Body**: `bodyMedium` / `bodyLarge`
- **Labels**: `labelLarge` / `labelMedium`

---

## 3. Core Design Patterns

### 3.1 Cards
- White background
- `RoundedCornerShape(12.dp)`
- Elevation: `1.dp` – `2.dp`
- **Mandatory left vertical accent strip** (5.dp width) in Primary Purple
- Soft shadow / elevation for hierarchy

### 3.2 Navigation
- Desktop: Permanent sidebar (`260.dp`) with Soft Lavender background
- Mobile: Modal drawer with same Soft Lavender background
- Selected item: Solid Purple pill + White text/icon
- Unselected: Dark text + transparent background

### 3.3 Forms (Very Important)
Every form (Create Company, Ledger, Voucher, etc.) **must**:

1. Be centered with `maxWidth = 800.dp` on desktop
2. Use **Card-based sections** instead of plain Column + OutlinedTextField
3. Each logical group (General Info, Contact, Financial, GST...) should be inside its own Card
4. Use consistent spacing (`16.dp` – `24.dp`)
5. Primary action button (Save) must use Purple color
6. Support full keyboard navigation (Enter moves focus)

### 3.4 Buttons & FABs
- Primary Button / FAB → `Color(0xFF7C4DFF)` background + White content
- Secondary / Text buttons → Purple text
- Destructive → Error red

### 3.5 Icons
Prefer Material Icons with corporate feel:
- `CorporateFare` for companies
- `Home`, `BarChart`, `Inventory`, `ShoppingCart`, etc.
- Icon tint should match Primary Purple when interactive

---

## 4. Current Theme Compliance Status

| Screen / Component              | Status          | Notes                                      |
|---------------------------------|-----------------|--------------------------------------------|
| Company List                    | Fully Compliant | Cards + accent strip + purple FAB          |
| Navigation Sidebar              | Fully Compliant | Correct lavender + purple selection        |
| Create / Edit Company Form      | **Out of Theme**| Plain Scaffold + OutlinedTextFields only   |
| Ledger Management               | Partial         | Needs full card review                     |
| Inventory Management            | Partial         | Needs full card review                     |
| Voucher Entry screens           | Partial         | Needs consistency check                    |
| Dashboard                       | Partial         | Needs consistency check                    |
| Login / SignUp / Landing        | Needs review    | Should match brand colors                  |

---

## 5. Rules for Future Development

1. **Never** hardcode random colors. Always use the values defined in this document.
2. Prefer creating a shared `WptTheme` / `WptColors` object in the future for maintainability.
3. Any new form **must** follow the Card-based section pattern.
4. All interactive elements that represent “primary action” must use `#7C4DFF`.

---

*This is the single source of truth for visual design.*
