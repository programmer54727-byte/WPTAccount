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