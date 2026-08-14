package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class Ledger(
    val id: String? = null,
    val company_id: String,
    val group_id: String,
    val ledger_name: String,
    val alias: String? = null,
    val mailing_name: String? = null,
    val address: String? = null,
    val state: String? = null,
    val country: String? = null,
    val pincode: String? = null,
    val pan_it_number: String? = null,
    val gst_registration_type: String? = null, // Regular, Composition, Consumer, Unregistered
    val gstin_uin: String? = null,
    
    // Bank Details
    val bank_acc_no: String? = null,
    val bank_ifsc: String? = null,
    val bank_name: String? = null,
    val bank_branch: String? = null,
    val bank_swift: String? = null,
    
    // Party Details
    val bill_by_bill: Boolean = false,
    val credit_period: Int? = null,
    val credit_limit: Double? = null,
    
    // Tax Details (for Duties & Taxes)
    val duty_tax_type: String? = null, // GST, TDS, Others
    val gst_tax_sub_type: String? = null, // Central Tax, State Tax, Integrated Tax, Cess
    val tax_rate: Double? = null,

    // Revenue/Expense Details
    val inventory_affected: Boolean = false,
    val cost_centres_applicable: Boolean = false,
    val gst_applicable_type: String? = null, // Applicable, Not Applicable, Undefined
    val supply_type: String? = null, // Goods, Services, Capital Goods
    val hsn_sac_code: String? = null,
    val hsn_sac_desc: String? = null,
    
    val opening_balance: Double = 0.0,
    val opening_balance_type: String = "Dr", // Dr or Cr
    val current_balance: Double = 0.0,
    val created_at: String? = null
)
