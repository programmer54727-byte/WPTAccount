package com.wpt.wptaccount

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.logging.LogLevel

val supabase by lazy {
    createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.KEY,
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        defaultLogLevel = LogLevel.DEBUG
    }
}