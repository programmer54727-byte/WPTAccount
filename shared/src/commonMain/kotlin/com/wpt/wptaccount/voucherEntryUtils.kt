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
    onSuccess: () -> Unit,
    voucherIdToEdit: String? = null
) {
    scope.launch {
        try {
            setSaving(true)
            setError(null)

            withContext(NonCancellable) {
                val dbDate = date.toDbDate()
                val dbInvoiceDate = invoiceDate.toDbDate()

                if (voucherIdToEdit != null) {
                    deleteVoucherData(voucherIdToEdit, voucherType)
                }

                // 1. Create/Update Voucher
                val voucher = Voucher(
                    id = voucherIdToEdit,
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
                
                val voucherId = if (voucherIdToEdit != null) {
                    supabase.from("vouchers").update(voucher) {
                        filter { eq("id", voucherIdToEdit) }
                    }
                    voucherIdToEdit
                } else {
                    val savedVoucher = supabase.from("vouchers").insert(voucher) {
                        select()
                    }.decodeSingle<Voucher>()
                    savedVoucher.id!!
                }

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
                                amount = row.amount.toDoubleOrNull() ?: 0.0,
                                hsn_code = row.hsnCode.ifEmpty { null },
                                gst_rate = row.gstRate
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

suspend fun deleteVoucherData(voucherId: String, voucherType: String) {
    // 1. Fetch entries to undo balance adjustments
    val entries = supabase.from("voucher_entries").select { filter { eq("voucher_id", voucherId) } }.decodeList<VoucherEntry>()
    entries.forEach {
        val undoEntryType = if (it.entry_type == "Debit") "Credit" else "Debit"
        updateLedgerBalanceInternal(it.ledger_id, it.amount, undoEntryType)
    }

    // 2. Fetch stock items to undo quantity adjustments
    val vStockItems = supabase.from("voucher_stock_items").select { filter { eq("voucher_id", voucherId) } }.decodeList<VoucherStockItem>()
    vStockItems.forEach { vsi ->
        val stockItem = supabase.from("stock_items").select { filter { eq("id", vsi.stock_item_id) } }.decodeSingle<StockItem>()
        val undoAdjustment = if (voucherType == "Purchase") -vsi.quantity else vsi.quantity
        val newQty = stockItem.current_quantity + undoAdjustment
        supabase.from("stock_items").update(buildJsonObject { put("current_quantity", newQty) }) { filter { eq("id", stockItem.id!!) } }
    }

    // 3. Delete related records
    supabase.from("voucher_entries").delete { filter { eq("voucher_id", voucherId) } }
    supabase.from("voucher_stock_items").delete { filter { eq("voucher_id", voucherId) } }
    supabase.from("voucher_references").delete { filter { eq("voucher_id", voucherId) } }
}
