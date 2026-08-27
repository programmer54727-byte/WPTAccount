package com.wpt.wptaccount

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun performSave(
    scope: kotlinx.coroutines.CoroutineScope,
    company: Company,
    voucherType: String,
    voucherNo: String,
    invoiceNo: String,
    invoiceDate: String,
    selectedPartyId: String?,
    selectedLedgerId: String?,
    date: String,
    narration: String,
    grandTotal: Double,
    itemSubTotal: Double,
    items: List<ItemRow>,
    taxEntries: List<TaxRow>,
    stockItems: List<StockItem>,
    ledgers: List<Ledger>,
    partyReferences: List<VoucherReference>,
    setSaving: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch {
        try {
            setSaving(true)
            setError(null)

            withContext(NonCancellable) {
                // Safe date conversion: DD-MM-YYYY -> YYYY-MM-DD
                fun toDbDate(d: String): String {
                    return try {
                        val parts = d.split("-")
                        if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else d
                    } catch (e: Exception) {
                        d
                    }
                }

                val dbDate = toDbDate(date)
                val dbInvoiceDate = toDbDate(invoiceDate)

                // 1. Create Voucher
                val voucher = Voucher(
                    company_id = company.id!!,
                    voucher_type = voucherType,
                    voucher_number = voucherNo.ifEmpty { null },
                    invoice_no = invoiceNo.ifEmpty { null },
                    invoice_date = dbInvoiceDate,
                    party_ledger_id = selectedPartyId,
                    date = dbDate,
                    narration = narration,
                    total_amount = grandTotal
                )
                val savedVoucher = supabase.from("vouchers").insert(voucher) {
                    select()
                }.decodeSingle<Voucher>()

                val voucherId = savedVoucher.id!!

                // 2. Save Stock Items & Update Quantities
                items.forEach { row ->
                    if (row.stockItemId.isNotEmpty()) {
                        val qtyVal = row.qty.toDoubleOrNull() ?: 0.0
                        supabase.from("voucher_stock_items").insert(
                            VoucherStockItem(
                                voucher_id = voucherId,
                                stock_item_id = row.stockItemId,
                                quantity = qtyVal,
                                rate = row.rate.toDoubleOrNull() ?: 0.0,
                                amount = row.amount.toDoubleOrNull() ?: 0.0
                            )
                        )

                        // Update actual stock
                        val stockItem = stockItems.find { it.id == row.stockItemId }
                        if (stockItem != null) {
                            val adjustment = if (voucherType == "Purchase") qtyVal else -qtyVal
                            val newQty = stockItem.current_quantity + adjustment
                            supabase.from("stock_items").update(buildJsonObject {
                                put("current_quantity", newQty)
                            }) { filter { eq("id", stockItem.id!!) } }
                        }
                    }
                }

                // 3. Save Accounting Entries & Update Balances
                // Party Entry
                val partyEntryType = if (voucherType == "Sale") "Debit" else "Credit"
                supabase.from("voucher_entries").insert(
                    VoucherEntry(
                        voucher_id = voucherId,
                        ledger_id = selectedPartyId!!,
                        amount = grandTotal,
                        entry_type = partyEntryType
                    )
                )
                updateLedgerBalanceInternal(selectedPartyId!!, grandTotal, partyEntryType)

                // Sales/Purchase Entry
                val ledgerEntryType = if (voucherType == "Sale") "Credit" else "Debit"
                supabase.from("voucher_entries").insert(
                    VoucherEntry(
                        voucher_id = voucherId,
                        ledger_id = selectedLedgerId!!,
                        amount = itemSubTotal,
                        entry_type = ledgerEntryType
                    )
                )
                updateLedgerBalanceInternal(selectedLedgerId!!, itemSubTotal, ledgerEntryType)

                // Tax Ledger Entries
                taxEntries.forEach { tax ->
                    if (tax.ledgerId.isNotEmpty()) {
                        supabase.from("voucher_entries").insert(
                            VoucherEntry(
                                voucher_id = voucherId,
                                ledger_id = tax.ledgerId,
                                amount = tax.amount,
                                entry_type = ledgerEntryType
                            )
                        )
                        updateLedgerBalanceInternal(tax.ledgerId, tax.amount, ledgerEntryType)
                    }
                }

                // 4. Save References
                partyReferences.forEach { ref ->
                    supabase.from("voucher_references").insert(ref.copy(
                        voucher_id = voucherId,
                        ledger_id = selectedPartyId // Use the primary party ledger
                    ))
                }
            }
            onSuccess()
        } catch (e: Exception) {
            println("Save error details: ${e.message}")
            setError("Failed to save: ${e.message?.take(100) ?: "Unknown error"}")
        } finally {
            setSaving(false)
        }
    }
}

suspend fun updateLedgerBalanceInternal(ledgerId: String, amount: Double, entryType: String) {
    val ledger = supabase.from("ledgers").select { filter { eq("id", ledgerId) } }.decodeSingle<Ledger>()
    // Adjustment: Debit adds (+), Credit subtracts (-)
    val adjustment = if (entryType == "Debit") amount else -amount
    val newBalance = ledger.current_balance + adjustment
    supabase.from("ledgers").update(buildJsonObject { put("current_balance", newBalance) }) { filter { eq("id", ledgerId) } }
}
