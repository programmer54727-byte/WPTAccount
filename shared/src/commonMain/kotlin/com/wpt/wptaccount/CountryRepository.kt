package com.wpt.wptaccount

import kotlinx.serialization.json.Json
import wptaccount.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object CountryRepository {
    private var cachedCountries: List<CountryData>? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getCountries(): List<CountryData> {
        if (cachedCountries != null) return cachedCountries!!
        
        return try {
            val bytes = Res.readBytes("files/countries.json")
            val jsonString = bytes.decodeToString()
            cachedCountries = Json.decodeFromString<List<CountryData>>(jsonString)
            cachedCountries!!
        } catch (e: Exception) {
            println("Error loading countries: ${e.message}")
            emptyList()
        }
    }
}
