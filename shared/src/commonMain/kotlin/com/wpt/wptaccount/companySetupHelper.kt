package com.wpt.wptaccount

import io.github.jan.supabase.postgrest.from

suspend fun initializeCompanySetup(companyId: String) {
    val defaultGroups = listOf(
        "Bank Accounts", "Bank OCC A/c", "Bank OD A/c", "Branch / Divisions", "Capital Account",
        "Cash-in-Hand", "Current Assets", "Current Liabilities", "Deposits (Asset)", "Direct Expenses",
        "Direct Incomes", "Duties & Taxes", "Expenses (Direct)", "Expenses (Indirect)", "Fixed Assets",
        "Income (Direct)", "Income (Indirect)", "Indirect Expenses", "Indirect Incomes", "Investments",
        "Loans & Advances (Asset)", "Loans (Liability)", "Misc. Expenses (ASSET)", "Provisions",
        "Purchase Accounts", "Reserves & Surplus", "Retained Earnings", "Sales Accounts",
        "Secured Loans", "Stock-in-Hand", "Sundry Creditors", "Sundry Debtors", "Suspense A/c",
        "Unsecured Loans"
    )

    println("Initializing setup for company: $companyId")
    
    // 1. Bulk insert groups
    val groupsToInsert = defaultGroups.map { 
        AccountingGroup(company_id = companyId, group_name = it) 
    }
    
    val insertedGroups = supabase.from("groups").insert(groupsToInsert) {
        select()
    }.decodeList<AccountingGroup>()
    
    println("34 Groups created for $companyId. Creating default ledgers...")
    
    // 2. Find correct group IDs for default ledgers
    val cashGroup = insertedGroups.find { it.group_name == "Cash-in-Hand" }
    val pnlGroup = insertedGroups.find { it.group_name == "Retained Earnings" }
    
    if (cashGroup != null) {
        supabase.from("ledgers").insert(
            Ledger(
                company_id = companyId,
                group_id = cashGroup.id!!,
                ledger_name = "Cash"
            )
        )
    }
    
    if (pnlGroup != null) {
        supabase.from("ledgers").insert(
            Ledger(
                company_id = companyId,
                group_id = pnlGroup.id!!,
                ledger_name = "Profit & Loss A/c"
            )
        )
    }
    
    println("Setup complete for $companyId")
}
