package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class UnitOfMeasure(
    val id: String? = null,
    val company_id: String,
    val unit_symbol: String,
    val formal_name: String? = null,
    val created_at: String? = null
)

@Serializable
data class StockGroup(
    val id: String? = null,
    val company_id: String,
    val group_name: String,
    val parent_group_id: String? = null,
    val created_at: String? = null
)

@Serializable
data class StockItem(
    val id: String? = null,
    val company_id: String,
    val group_id: String? = null,
    val unit_id: String,
    val item_name: String,
    val opening_quantity: Double = 0.0,
    val opening_rate: Double = 0.0,
    val current_quantity: Double = 0.0,
    val created_at: String? = null
)
