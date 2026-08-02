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
    val gst_applicability: String = "Applicable",
    val hsn_sac_details: String = "As per Company/Stock Group",
    val hsn_sac_number: String? = null,
    val hsn_description: String? = null,
    val gst_rate_details: String = "As per Company/Stock Group",
    val taxability_type: String = "Taxable",
    val gst_rate: Double = 0.0,
    val type_of_supply: String = "Goods",
    val created_at: String? = null
)

@Serializable
data class StockItem(
    val id: String? = null,
    val company_id: String,
    val group_id: String? = null,
    val unit_id: String,
    val item_name: String,
    val alias: String? = null,
    val gst_applicability: String = "Applicable",
    val hsn_sac_details: String = "As per Company/Stock Group",
    val hsn_sac_number: String? = null,
    val hsn_description: String? = null,
    val gst_rate_details: String = "As per Company/Stock Group",
    val taxability_type: String = "Taxable",
    val gst_rate: Double = 0.0,
    val type_of_supply: String = "Goods",
    val opening_quantity: Double = 0.0,
    val opening_rate: Double = 0.0,
    val current_quantity: Double = 0.0,
    val created_at: String? = null
)
