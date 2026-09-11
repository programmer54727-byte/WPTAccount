package com.wpt.wptaccount

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
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

                val entriesPayload = buildJsonArray {
                    // Party Entry
                    add(buildJsonObject {
                        put("ledger_id", selectedPartyId!!)
                        put("amount", grandTotal)
                        put("entry_type", if (voucherType == "Sale") "Debit" else "Credit")
                    })
                    // Sales/Purchase Entry
                    add(buildJsonObject {
                        put("ledger_id", selectedLedgerId!!)
                        put("amount", itemSubTotal)
                        put("entry_type", if (voucherType == "Sale") "Credit" else "Debit")
                    })
                    // Tax Entries
                    taxEntries.forEach { tax ->
                        if (tax.ledgerId.isNotEmpty()) {
                            add(buildJsonObject {
                                put("ledger_id", tax.ledgerId)
                                put("amount", tax.amount)
                                put("entry_type", if (voucherType == "Sale") "Credit" else "Debit")
                            })
                        }
                    }
                }

                val stockItemsPayload = buildJsonArray {
                    items.forEach { row ->
                        if (row.stockItemId.isNotEmpty()) {
                            add(buildJsonObject {
                                put("stock_item_id", row.stockItemId)
                                put("quantity", row.qty.toDoubleOrNull() ?: 0.0)
                                put("rate", row.rate.toDoubleOrNull() ?: 0.0)
                                put("amount", row.amount.toDoubleOrNull() ?: 0.0)
                                put("hsn_code", row.hsnCode.ifEmpty { null })
                                put("gst_rate", row.gstRate)
                            })
                        }
                    }
                }

                val referencesPayload = buildJsonArray {
                    partyReferences.forEach { ref ->
                        add(buildJsonObject {
                            put("ledger_id", selectedPartyId)
                            put("reference_type", ref.reference_type)
                            put("reference_no", ref.reference_no)
                            put("amount", ref.amount)
                        })
                    }
                }

                val params = buildJsonObject {
                    put("p_voucher_id", voucherIdToEdit)
                    put("p_company_id", company.id!!)
                    put("p_voucher_type", voucherType)
                    put("p_voucher_number", voucherNo.ifEmpty { null })
                    put("p_invoice_no", invoiceNo.ifEmpty { null })
                    put("p_invoice_date", dbInvoiceDate)
                    put("p_party_ledger_id", selectedPartyId)
                    put("p_date", dbDate)
                    put("p_narration", narration)
                    put("p_total_amount", grandTotal)
                    put("p_entries", entriesPayload)
                    put("p_stock_items", stockItemsPayload)
                    put("p_references", referencesPayload)
                }

                supabase.postgrest.rpc("save_voucher_v3", params)
            }
            onSuccess()
        } catch (e: Exception) {
            println("Save error details: ${e.message}")
            setError("Failed to save: ${e.toUserFriendlyMessage()}")
        } finally {
            setSaving(false)
        }
    }
}

suspend fun deleteVoucherData(voucherId: String) {
    supabase.postgrest.rpc("delete_voucher_v3", buildJsonObject {
        put("p_voucher_id", voucherId)
    })
}
