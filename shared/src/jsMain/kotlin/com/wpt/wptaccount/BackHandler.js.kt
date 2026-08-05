package com.wpt.wptaccount

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Browser doesn't have a standard system back button like Android.
}
