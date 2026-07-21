package com.wpt.wptaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf("landing") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                when (currentScreen) {
                    "landing" -> {
                        LandingPage(
                            onSignUpClick = { currentScreen = "signup" },
                            onLoginClick = { currentScreen = "login" }
                        )
                    }
                    "signup" -> {
                        SignUp(
                            onBackClick = { currentScreen = "landing" },
                            onSignUpSuccess = { currentScreen = "dashboard" }
                        )
                    }
                    "login" -> {
                        Login(
                            onBackClick = { currentScreen = "landing" },
                            onLoginSuccess = { currentScreen = "dashboard" }
                        )
                    }
                    "dashboard" -> {
                        PlaceholderScreen("Dashboard Screen") { currentScreen = "landing" }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name, style = MaterialTheme.typography.headlineLarge)
            androidx.compose.material3.Button(onClick = onBack) {
                Text("Back to Landing")
            }
        }
    }
}
