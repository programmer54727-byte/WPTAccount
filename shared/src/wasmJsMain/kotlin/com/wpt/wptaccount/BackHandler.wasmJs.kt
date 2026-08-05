package com.wpt.wptaccount

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Wasm platform doesn't have a system back button.
}
