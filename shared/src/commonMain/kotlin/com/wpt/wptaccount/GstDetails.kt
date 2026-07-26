package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class GstDetails(
    val id: String? = null,
    val company_id: String,
    val registration_status: String = "Active",
    val state: String? = null,
    val registration_type: String? = null,
    val is_other_territory_assessee: Boolean = false,
    val gstin_uin: String? = null,
    val gstr1_periodicity: String? = null,
    val gst_username: String? = null,
    val filing_mode: String? = null,
    val eway_bill_applicable: Boolean = false,
    val eway_bill_date: String? = null,
    val eway_bill_intrastate: Boolean = false,
    val einvoice_applicable: Boolean = false,
    val registration_name: String? = null,
    val created_at: String? = null
)
