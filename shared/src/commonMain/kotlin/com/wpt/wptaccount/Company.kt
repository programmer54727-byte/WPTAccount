package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String? = null,
    val company_name: String,
    val mailing_name: String? = null,
    val address: String? = null,
    val state: String? = null,
    val country: String? = null,
    val pincode: String? = null,
    val telephone: String? = null,
    val mobile: String? = null,
    val fax: String? = null,
    val email: String? = null,
    val website: String? = null,
    val financial_year_beginning: String? = null,
    val books_beginning: String? = null,
    val tally_vault_password_enabled: String? = null,
    val control_user_access_enabled: String? = null,
    val base_currency_symbol: String? = null,
    val formal_name: String? = null,
    val owner_id: String? = null,
    val created_at: String? = null
)
