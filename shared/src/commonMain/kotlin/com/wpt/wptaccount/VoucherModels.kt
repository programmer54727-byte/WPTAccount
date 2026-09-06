package com.wpt.wptaccount

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Voucher(
    val id: String? = null,
    val company_id: String = "",
    val voucher_type: String = "", // Sale, Purchase, Payment, etc.
    val voucher_number: String? = null,
    val invoice_no: String? = null,
    val invoice_date: String? = null,
    val party_ledger_id: String? = null,
    val date: String = "",
    val narration: String? = null,
    val total_amount: Double = 0.0,
    val created_at: String? = null
)

@Serializable
data class VoucherEntry(
    val id: String? = null,
    val voucher_id: String,
    val ledger_id: String,
    val amount: Double,
    val entry_type: String, // Debit, Credit
    val created_at: String? = null
)

@Serializable
data class VoucherStockItem(
    val id: String? = null,
    val voucher_id: String,
    val stock_item_id: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double,
    val hsn_code: String? = null,
    val gst_rate: Double = 0.0,
    val created_at: String? = null
)

@Serializable
data class VoucherStockItemWithVoucher(
    val quantity: Double,
    val rate: Double,
    val amount: Double,
    val vouchers: Voucher
)

@Serializable
data class VoucherEntryWithVoucher(
    val ledger_id: String = "",
    val amount: Double,
    val entry_type: String,
    val vouchers: Voucher
)

@Serializable
data class VoucherReference(
    val id: String? = null,
    val voucher_id: String? = null,
    val ledger_id: String? = null,
    val reference_type: String, // Advance, Against Reference, New Reference, On Account
    val reference_no: String,
    val amount: Double,
    val created_at: String? = null
)

// --- UI Helper Models for Voucher Entry ---

@Serializable
data class ItemRow(
    var stockItemId: String = "",
    var hsnCode: String = "",
    var gstRate: Double = 0.0,
    var qty: String = "1",
    var rate: String = "0",
    var amount: String = "0"
)

@Serializable
data class TaxRow(
    var ledgerId: String = "",
    var taxRate: Double = 0.0,
    var amount: Double = 0.0
)

val ItemRowListSaver = listSaver<SnapshotStateList<ItemRow>, String>(
    save = { list -> list.map { Json.encodeToString(it) } },
    restore = { strings -> 
        val list = mutableStateListOf<ItemRow>()
        list.addAll(strings.map { Json.decodeFromString(it) })
        list
    }
)

val TaxRowListSaver = listSaver<SnapshotStateList<TaxRow>, String>(
    save = { list -> list.map { Json.encodeToString(it) } },
    restore = { strings -> 
        val list = mutableStateListOf<TaxRow>()
        list.addAll(strings.map { Json.decodeFromString(it) })
        list
    }
)

val VoucherReferenceListSaver = listSaver<SnapshotStateList<VoucherReference>, String>(
    save = { list -> list.map { Json.encodeToString(it) } },
    restore = { strings -> 
        val list = mutableStateListOf<VoucherReference>()
        list.addAll(strings.map { Json.decodeFromString(it) })
        list
    }
)
