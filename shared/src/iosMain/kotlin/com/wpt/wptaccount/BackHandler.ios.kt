package com.wpt.wptaccount

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a standard system back button.
}
