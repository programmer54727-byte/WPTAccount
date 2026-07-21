package com.wpt.wptaccount

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.logging.LogLevel
import io.ktor.client.plugins.HttpTimeout
import io.github.jan.supabase.annotations.SupabaseInternal

val supabase by lazy {
    @OptIn(SupabaseInternal::class)
    createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.KEY,
    ) {
        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000L
                connectTimeoutMillis = 30000L
                socketTimeoutMillis = 30000L
            }
        }
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        defaultLogLevel = LogLevel.DEBUG
    }
}