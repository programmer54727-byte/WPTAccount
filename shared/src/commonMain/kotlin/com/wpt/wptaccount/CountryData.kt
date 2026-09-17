package com.wpt.wptaccount

import kotlinx.serialization.Serializable

@Serializable
data class CountryData(
    val country: String,
    val code: String,
    val states: List<StateData> = emptyList(),
    val union_territories: List<StateData> = emptyList()
) {
    fun getAllRegions(): List<StateData> = states + union_territories
}

@Serializable
data class StateData(
    val name: String,
    val code: String
)
