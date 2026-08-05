package com.wpt.wptaccount

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // JVM platform doesn't have a system back button. 
    // Handled via keyboard or window events if needed.
}
