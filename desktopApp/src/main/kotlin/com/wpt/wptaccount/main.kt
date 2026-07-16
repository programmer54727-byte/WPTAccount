package com.wpt.wptaccount

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import wptaccount.shared.generated.resources.Res
import wptaccount.shared.generated.resources.applogo

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "WPTAccount",
        icon = painterResource(Res.drawable.applogo),
    ) {
        App()
    }
}