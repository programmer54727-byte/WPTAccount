-- 1. Create the companies table
create table if not exists public.companies (
  id uuid primary key default gen_random_uuid(),
  company_name text not null,
  mailing_name text,
  address text,
  state text,
  country text,
  pincode text,
  telephone text,
  mobile text,
  fax text,
  email text,
  website text,
  financial_year_beginning date,
  books_beginning date,
  tally_vault_password_enabled text,
  control_user_access_enabled text,
  base_currency_symbol text,
  formal_name text,
  gst_applicability text default 'Applicable',
  hsn_sac_details text default 'Specify Details Here',
  hsn_sac_number text,
  hsn_description text,
  gst_rate_details text default 'Specify Details Here',
  taxability_type text default 'Taxable',
  gst_rate decimal default 0,
  type_of_supply text default 'Goods',
  owner_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamp with time zone default now() not null
);

-- 2. Enable Row Level Security (RLS) for companies
alter table public.companies enable row level security;

-- 3. Create Security Policies for companies
create policy "Users can see only their own companies"
on public.companies
for select
using (auth.uid() = owner_id);

create policy "Users can create their own companies"
on public.companies
for insert
with check (auth.uid() = owner_id);

create policy "Users can update their own companies"
on public.companies
for update
using (auth.uid() = owner_id);

create policy "Users can delete their own companies"
on public.companies
for delete
using (auth.uid() = owner_id);

-- 4. Create the groups table
create table if not exists public.groups (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  group_name text not null,
  parent_group_id uuid references public.groups(id) on delete cascade,
  nature text, -- Asset, Liability, Income, Expense
  created_at timestamp with time zone default now() not null
);

-- 5. Enable Row Level Security (RLS) for groups
alter table public.groups enable row level security;

-- 6. Create Security Policies for groups
create policy "Users can see groups of their own companies"
on public.groups for select
using (exists (select 1 from public.companies where id = groups.company_id and owner_id = auth.uid()));

create policy "Users can create groups in their own companies"
on public.groups for insert
with check (exists (select 1 from public.companies where id = groups.company_id and owner_id = auth.uid()));

create policy "Users can update groups of their own companies"
on public.groups for update
using (exists (select 1 from public.companies where id = groups.company_id and owner_id = auth.uid()));

create policy "Users can delete groups of their own companies"
on public.groups for delete
using (exists (select 1 from public.companies where id = groups.company_id and owner_id = auth.uid()));

-- 7. Create the ledgers table
create table if not exists public.ledgers (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  group_id uuid not null references public.groups(id) on delete cascade,
  ledger_name text not null,
  alias text,
  mailing_name text,
  address text,
  state text,
  country text,
  pincode text,
  pan_it_number text,
  gst_registration_type text, -- Regular, Composition, Consumer, Unregistered
  gstin_uin text,

  -- Bank Details
  bank_acc_no text,
  bank_ifsc text,
  bank_name text,
  bank_branch text,
  bank_swift text,

  -- Party Details
  bill_by_bill boolean default false,
  credit_period integer,
  credit_limit decimal,

  -- Tax Details
  duty_tax_type text, -- GST, TDS, Others
  gst_tax_sub_type text, -- Central Tax, State Tax, Integrated Tax, Cess
  tax_rate decimal,

  -- Revenue/Expense Details
  inventory_affected boolean default false,
  cost_centres_applicable boolean default false,
  gst_applicable_type text, -- Applicable, Not Applicable, Undefined
  supply_type text, -- Goods, Services, Capital Goods
  hsn_sac_code text,
  hsn_sac_desc text,

  opening_balance decimal default 0,
  opening_balance_type text default 'Dr', -- Dr, Cr
  current_balance decimal default 0,
  created_at timestamp with time zone default now() not null
);

-- 8. Enable Row Level Security (RLS) for ledgers
alter table public.ledgers enable row level security;

-- 9. Create Security Policies for ledgers
create policy "Users can see ledgers of their own companies"
on public.ledgers for select
using (exists (select 1 from public.companies where id = ledgers.company_id and owner_id = auth.uid()));

create policy "Users can create ledgers in their own companies"
on public.ledgers for insert
with check (exists (select 1 from public.companies where id = ledgers.company_id and owner_id = auth.uid()));

create policy "Users can update ledgers of their own companies"
on public.ledgers for update
using (exists (select 1 from public.companies where id = ledgers.company_id and owner_id = auth.uid()));

create policy "Users can delete ledgers of their own companies"
on public.ledgers for delete
using (exists (select 1 from public.companies where id = ledgers.company_id and owner_id = auth.uid()));

-- 10. Create the vouchers table
create table if not exists public.vouchers (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  voucher_type text not null, -- Sale, Purchase, Payment, Receipt, Contra, Journal, CreditNote, DebitNote
  voucher_number text,
  invoice_no text, -- Supplier Invoice No / Ref No
  invoice_date date,
  party_ledger_id uuid references public.ledgers(id) on delete set null,
  date date default current_date not null,
  narration text,
  total_amount decimal default 0,
  created_at timestamp with time zone default now() not null
);

-- 11. Enable Row Level Security (RLS) for vouchers
alter table public.vouchers enable row level security;

-- 12. Create Security Policies for vouchers
create policy "Users can see vouchers of their own companies"
on public.vouchers for select
using (exists (select 1 from public.companies where id = vouchers.company_id and owner_id = auth.uid()));

create policy "Users can create vouchers in their own companies"
on public.vouchers for insert
with check (exists (select 1 from public.companies where id = vouchers.company_id and owner_id = auth.uid()));

create policy "Users can update vouchers of their own companies"
on public.vouchers for update
using (exists (select 1 from public.companies where id = vouchers.company_id and owner_id = auth.uid()));

create policy "Users can delete vouchers of their own companies"
on public.vouchers for delete
using (exists (select 1 from public.companies where id = vouchers.company_id and owner_id = auth.uid()));

-- 13. Create the voucher_entries table
create table if not exists public.voucher_entries (
  id uuid primary key default gen_random_uuid(),
  voucher_id uuid not null references public.vouchers(id) on delete cascade,
  ledger_id uuid not null references public.ledgers(id) on delete cascade,
  amount decimal not null,
  entry_type text not null, -- Debit, Credit
  created_at timestamp with time zone default now() not null
);

-- 14. Enable Row Level Security (RLS) for voucher_entries
alter table public.voucher_entries enable row level security;

-- 15. Create Security Policies for voucher_entries
create policy "Users can see entries of their vouchers"
on public.voucher_entries for select
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_entries.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can create entries in their vouchers"
on public.voucher_entries for insert
with check (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_entries.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can delete entries of their vouchers"
on public.voucher_entries for delete
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_entries.voucher_id and companies.owner_id = auth.uid()
));

-- 16. Create the voucher_stock_items table
create table if not exists public.voucher_stock_items (
  id uuid primary key default gen_random_uuid(),
  voucher_id uuid not null references public.vouchers(id) on delete cascade,
  stock_item_id uuid not null references public.stock_items(id) on delete cascade,
  quantity decimal not null,
  rate decimal not null,
  amount decimal not null,
  hsn_code text,
  gst_rate decimal default 0,
  created_at timestamp with time zone default now() not null
);

-- 17. Enable Row Level Security (RLS) for voucher_stock_items
alter table public.voucher_stock_items enable row level security;

-- 18. Create Security Policies for voucher_stock_items
create policy "Users can see stock entries of their vouchers"
on public.voucher_stock_items for select
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_stock_items.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can create stock entries in their vouchers"
on public.voucher_stock_items for insert
with check (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_stock_items.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can delete stock entries of their vouchers"
on public.voucher_stock_items for delete
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_stock_items.voucher_id and companies.owner_id = auth.uid()
));

-- 19. Create the gst_details table
create table if not exists public.gst_details (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  registration_status text default 'Active',
  state text,
  registration_type text, -- Regular, Composition
  is_other_territory_assessee boolean default false,
  gstin_uin text,
  gstr1_periodicity text, -- Monthly, Quarterly
  gst_username text,
  filing_mode text,
  eway_bill_applicable boolean default false,
  eway_bill_date date,
  eway_bill_intrastate boolean default false,
  einvoice_applicable boolean default false,
  registration_name text,
  created_at timestamp with time zone default now() not null,
  unique(company_id)
);

-- 17. Enable RLS for gst_details
alter table public.gst_details enable row level security;

-- 18. Create Security Policies for gst_details
create policy "Users can see gst_details of their own companies"
on public.gst_details for select
using (exists (select 1 from public.companies where id = gst_details.company_id and owner_id = auth.uid()));

create policy "Users can create gst_details in their own companies"
on public.gst_details for insert
with check (exists (select 1 from public.companies where id = gst_details.company_id and owner_id = auth.uid()));

create policy "Users can update gst_details of their own companies"
on public.gst_details for update
using (exists (select 1 from public.companies where id = gst_details.company_id and owner_id = auth.uid()));

create policy "Users can delete gst_details of their own companies"
on public.gst_details for delete
using (exists (select 1 from public.companies where id = gst_details.company_id and owner_id = auth.uid()));

-- 19. Create the units table (Units of Measure)
create table if not exists public.units (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  unit_symbol text not null, -- e.g., Pcs, Kg
  formal_name text,
  created_at timestamp with time zone default now() not null
);

-- 17. Enable RLS for units
alter table public.units enable row level security;

-- 18. Create Security Policies for units
create policy "Users can see units of their own companies"
on public.units for select
using (exists (select 1 from public.companies where id = units.company_id and owner_id = auth.uid()));

create policy "Users can create units in their own companies"
on public.units for insert
with check (exists (select 1 from public.companies where id = units.company_id and owner_id = auth.uid()));

create policy "Users can update units of their own companies"
on public.units for update
using (exists (select 1 from public.companies where id = units.company_id and owner_id = auth.uid()));

create policy "Users can delete units of their own companies"
on public.units for delete
using (exists (select 1 from public.companies where id = units.company_id and owner_id = auth.uid()));

-- 19. Create the stock_groups table
create table if not exists public.stock_groups (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  group_name text not null,
  parent_group_id uuid references public.stock_groups(id) on delete cascade,
  created_at timestamp with time zone default now() not null
);

-- 20. Enable RLS for stock_groups
alter table public.stock_groups enable row level security;

-- 21. Create Security Policies for stock_groups
create policy "Users can see stock_groups of their own companies"
on public.stock_groups for select
using (exists (select 1 from public.companies where id = stock_groups.company_id and owner_id = auth.uid()));

create policy "Users can create stock_groups in their own companies"
on public.stock_groups for insert
with check (exists (select 1 from public.companies where id = stock_groups.company_id and owner_id = auth.uid()));

create policy "Users can delete stock_groups of their own companies"
on public.stock_groups for delete
using (exists (select 1 from public.companies where id = stock_groups.company_id and owner_id = auth.uid()));

-- 22. Create the stock_items table
create table if not exists public.stock_items (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  group_id uuid references public.stock_groups(id) on delete set null,
  unit_id uuid not null references public.units(id) on delete cascade,
  item_name text not null,
  alias text,
  gst_applicability text default 'Applicable', -- Applicable, Not Applicable, Undefined
  hsn_sac_details text default 'As per Company/Stock Group',
  hsn_sac_number text,
  hsn_description text,
  gst_rate_details text default 'As per Company/Stock Group',
  taxability_type text default 'Taxable', -- Taxable, Nil Rated, Exempt
  gst_rate decimal default 0,
  type_of_supply text default 'Goods', -- Goods, Services, Capital Goods
  opening_quantity decimal default 0,
  opening_rate decimal default 0,
  current_quantity decimal default 0,
  created_at timestamp with time zone default now() not null
);

-- 23. Enable RLS for stock_items
alter table public.stock_items enable row level security;

-- 24. Create Security Policies for stock_items
create policy "Users can see stock_items of their own companies"
on public.stock_items for select
using (exists (select 1 from public.companies where id = stock_items.company_id and owner_id = auth.uid()));

create policy "Users can create stock_items in their own companies"
on public.stock_items for insert
with check (exists (select 1 from public.companies where id = stock_items.company_id and owner_id = auth.uid()));

create policy "Users can update stock_items of their own companies"
on public.stock_items for update
using (exists (select 1 from public.companies where id = stock_items.company_id and owner_id = auth.uid()));

create policy "Users can delete stock_items of their own companies"
on public.stock_items for delete
using (exists (select 1 from public.companies where id = stock_items.company_id and owner_id = auth.uid()));

-- 25. Create the voucher_references table (Bill-wise details)
create table if not exists public.voucher_references (
  id uuid primary key default gen_random_uuid(),
  voucher_id uuid not null references public.vouchers(id) on delete cascade,
  reference_type text not null, -- Advance, Against Reference, New Reference
  reference_no text not null,
  amount decimal not null,
  created_at timestamp with time zone default now() not null
);

-- 26. Enable RLS for voucher_references
alter table public.voucher_references enable row level security;

-- 27. Create Security Policies for voucher_references
create policy "Users can see references of their vouchers"
on public.voucher_references for select
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_references.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can insert references for their vouchers"
on public.voucher_references for insert
with check (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_references.voucher_id and companies.owner_id = auth.uid()
));

create policy "Users can delete references of their vouchers"
on public.voucher_references for delete
using (exists (
  select 1 from public.vouchers
  join public.companies on vouchers.company_id = companies.id
  where vouchers.id = voucher_references.voucher_id and companies.owner_id = auth.uid()
));
