# Implementation Plan - Database Schema for Vouchers and Ledgers

To support the 8 voucher types (Sale, Purchase, Payment, etc.), we need a robust database structure. This plan adds the necessary tables and security policies to `supabasetableandpolicy.sql`.

## User Review Required

> [!IMPORTANT]
> I am adding three main tables:
> 1.  **`ledgers`**: To store account heads (e.g., Cash, Bank, Sales A/c, Customer names).
> 2.  **`vouchers`**: To store the main header of every transaction (Voucher No, Date, Type).
> 3.  **`voucher_entries`**: To store the specific Debit (Dr) and Credit (Cr) amounts for each ledger in a voucher.
>
> You will need to run these SQL commands in your Supabase SQL Editor once added.

## Proposed Changes

### [MODIFY] [supabasetableandpolicy.sql](file:///C:/Users/sayye/AndroidStudioProjects/WPTAccount/shared/src/commonMain/kotlin/com/wpt/wptaccount/supabasetableandpolicy.sql)

- Add **`ledgers`** table:
    - Links to a specific company.
    - Stores ledger name and opening balance.
- Add **`vouchers`** table:
    - Stores `voucher_type` (Sale, Purchase, etc.).
    - Stores `voucher_number` and `date`.
- Add **`voucher_entries`** table:
    - Links multiple ledgers to a single voucher.
    - Stores `amount` and `entry_type` (Debit or Credit).
- Add **RLS Policies**:
    - Ensures users can only see/edit data belonging to companies they own.

## Verification Plan

### Manual Verification
- After adding the code, you can copy-paste the SQL into the Supabase Dashboard.
- Verify that the tables are created with correct relationships.
- Verify that RLS (Row Level Security) is active.
