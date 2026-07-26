package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class Ledger(
    val id: String? = null,
    val company_id: String,
    val group_id: String,
    val ledger_name: String,
    val opening_balance: Double = 0.0,
    val current_balance: Double = 0.0,
    val created_at: String? = null
)
