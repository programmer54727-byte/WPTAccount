package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class Voucher(
    val id: String? = null,
    val company_id: String = "",
    val voucher_type: String = "", // Sale, Purchase, Payment, etc.
    val voucher_number: String? = null,
    val reference_no: String? = null,
    val reference_date: String? = null,
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
    val amount: Double,
    val entry_type: String,
    val vouchers: Voucher
)
