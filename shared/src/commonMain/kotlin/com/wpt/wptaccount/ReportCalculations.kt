package com.wpt.wptaccount

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

data class GroupSummary(
    val group: AccountingGroup,
    var closingBalance: Double = 0.0,
    val childGroups: MutableList<GroupSummary> = mutableListOf(),
    val ledgers: MutableList<LedgerSummary> = mutableListOf()
)

data class LedgerSummary(
    val ledger: Ledger,
    var closingBalance: Double = 0.0
)

object ReportEngine {

    suspend fun getFinancialData(
        companyId: String,
        period: AccountPeriod
    ): Map<String, Double> {
        // 1. Fetch All Master Data
        val groups = supabase.from("groups").select {
            filter { eq("company_id", companyId) }
        }.decodeList<AccountingGroup>()

        val ledgers = supabase.from("ledgers").select {
            filter { eq("company_id", companyId) }
        }.decodeList<Ledger>()

        // 2. Fetch All Transactions for the Company
        val allEntries = supabase.from("voucher_entries").select(Columns.raw("ledger_id, amount, entry_type, vouchers(date, company_id)")) {
            filter { eq("vouchers.company_id", companyId) }
        }.decodeList<VoucherEntryWithVoucher>()

        // 3. Calculate Ledger Balances as of period.endDate
        val ledgerBalances = ledgers.associate { ledger ->
            var bal = ledger.opening_balance
            if (ledger.opening_balance_type == "Cr") bal = -bal

            allEntries.filter { it.ledger_id == ledger.id }.forEach { entry ->
                val sign = if (entry.entry_type == "Debit") 1.0 else -1.0
                // For Balance Sheet/Closing balances, we take ALL transactions up to end date
                if (entry.vouchers.date <= period.endDate) {
                    bal += entry.amount * sign
                }
            }
            ledger.id!! to bal
        }

        return ledgerBalances
    }

    /**
     * Logic to roll up ledger balances into their respective groups recursively.
     */
    fun calculateGroupTotals(
        groups: List<AccountingGroup>,
        ledgerBalances: Map<String, Double>,
        ledgers: List<Ledger>
    ): Map<String, Double> {
        val groupTotals = mutableMapOf<String, Double>()
        
        fun getGroupTotal(groupId: String): Double {
            if (groupTotals.containsKey(groupId)) return groupTotals[groupId]!!
            
            var total = 0.0
            
            // Add balances of ledgers directly under this group
            ledgers.filter { it.group_id == groupId }.forEach { ledger ->
                total += ledgerBalances[ledger.id] ?: 0.0
            }
            
            // Add balances of child groups
            groups.filter { it.parent_group_id == groupId }.forEach { child ->
                total += getGroupTotal(child.id!!)
            }
            
            groupTotals[groupId] = total
            return total
        }
        
        groups.forEach { getGroupTotal(it.id!!) }
        return groupTotals
    }
    
    /**
     * Specialized stock valuation logic for P&L / Balance Sheet.
     */
    suspend fun calculateStockValue(companyId: String, endDate: String): Double {
        try {
            val baseItems = supabase.from("stock_items").select {
                filter { eq("company_id", companyId) }
            }.decodeList<StockItem>()

            val allTxns = supabase.from("voucher_stock_items").select(Columns.raw("stock_item_id, quantity, rate, amount, vouchers!inner(date, company_id, voucher_type)")) {
                filter { 
                    eq("vouchers.company_id", companyId)
                    lte("vouchers.date", endDate)
                }
            }.decodeList<VoucherStockItemWithVoucher>()

            var totalStockValue = 0.0

            baseItems.forEach { item ->
                var totalInQty = item.opening_quantity
                var totalInValue = item.opening_quantity * item.opening_rate
                var totalOutQty = 0.0

                allTxns.filter { it.stock_item_id == item.id }.forEach { txn ->
                    when (txn.vouchers.voucher_type) {
                        "Purchase" -> {
                            totalInQty += txn.quantity
                            totalInValue += txn.amount
                        }
                        "Sale" -> {
                            totalOutQty += txn.quantity
                        }
                    }
                }
                
                val currentQty = totalInQty - totalOutQty
                val avgRate = if (totalInQty != 0.0) totalInValue / totalInQty else item.opening_rate
                
                if (currentQty > 0) {
                    totalStockValue += (currentQty * avgRate)
                }
            }
            return totalStockValue
        } catch (e: Exception) {
            return 0.0
        }
    }

    /**
     * Logic for Cash Flow - fetches monthly aggregated movements for all Cash and Bank ledgers.
     */
    suspend fun getCashFlowData(
        companyId: String,
        period: AccountPeriod
    ): Map<Int, MonthlyLedgerData> {
        val ledgers = supabase.from("ledgers").select {
            filter { eq("company_id", companyId) }
        }.decodeList<Ledger>()

        val groups = supabase.from("groups").select {
            filter { eq("company_id", companyId) }
        }.decodeList<AccountingGroup>()

        // Identify Cash and Bank ledgers
        val cashBankLedgerIds = ledgers.filter { ledger ->
            val group = groups.find { it.id == ledger.group_id }
            val gName = group?.group_name ?: ""
            gName.contains("Cash", true) || gName.contains("Bank", true)
        }.map { it.id!! }

        val entries = supabase.from("voucher_entries").select(Columns.raw("amount, entry_type, vouchers(date, company_id)")) {
            filter { 
                eq("vouchers.company_id", companyId)
                isIn("ledger_id", cashBankLedgerIds)
            }
        }.decodeList<VoucherEntryWithVoucher>()

        val monthSequence = listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3)
        val months = listOf("April", "May", "June", "July", "August", "September", "October", "November", "December", "January", "February", "March")
        val dataMap = monthSequence.associateWith { m -> 
            MonthlyLedgerData(months[monthSequence.indexOf(m)]) 
        }.toMutableMap()

        entries.forEach { entry ->
            if (entry.vouchers.date >= period.startDate && entry.vouchers.date <= period.endDate) {
                val dateParts = entry.vouchers.date.split("-")
                if (dateParts.size == 3) {
                    val mInt = dateParts[1].toInt()
                    val monthData = dataMap[mInt]
                    if (monthData != null) {
                        // For Cash Flow: 
                        // Debit in Cash/Bank is Inflow (Receipt)
                        // Credit in Cash/Bank is Outflow (Payment)
                        if (entry.entry_type == "Debit") monthData.debit += entry.amount
                        else monthData.credit += entry.amount
                    }
                }
            }
        }

        return dataMap
    }
}
