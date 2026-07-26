package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class AccountingGroup(
    val id: String? = null,
    val company_id: String,
    val group_name: String,
    val parent_group_id: String? = null,
    val nature: String? = null, // Asset, Liability, Income, Expense
    val created_at: String? = null
)
