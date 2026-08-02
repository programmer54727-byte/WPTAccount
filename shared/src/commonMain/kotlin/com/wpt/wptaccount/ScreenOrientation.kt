package com.wpt.wptaccount

import androidx.compose.runtime.staticCompositionLocalOf

enum class ScreenOrientation {
    UNSPECIFIED,
    LANDSCAPE,
    PORTRAIT,
    SENSOR
}

val LocalScreenOrientation = staticCompositionLocalOf<(ScreenOrientation) -> Unit> {
    { _ -> }
}
