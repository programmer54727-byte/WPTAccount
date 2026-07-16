package com.wpt.wptaccount

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.wpt.wptaccount.landingpage.PreLogin
import com.wpt.wptaccount.signupform.SignUp
import com.wpt.wptaccount.login.Login
import com.wpt.wptaccount.UserDashboard

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf("landing") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Optional: Show status at the top for testing
            // Text(connectionStatus, style = MaterialTheme.typography.labelSmall) 

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                when (currentScreen) {
                    "landing" -> {
                        PreLogin(
                            onSignUpClick = { currentScreen = "signup" },
                            onLoginClick = { currentScreen = "login" }
                        )
                    }
                    "signup" -> {
                        SignUp(
                            onBackClick = { currentScreen = "landing" },
                            onSignUpSuccess = { currentScreen = "dashboard" },
                            onLoginClick = { currentScreen = "login" }
                        )
                    }
                    "login" -> {
                        Login(
                            onBackClick = { currentScreen = "landing" },
                            onLoginSuccess = { currentScreen = "dashboard" }
                        )
                    }
                    "dashboard" -> {
                        UserDashboard(
                            onLogout = { currentScreen = "landing" }
                        )
                    }
                }
            }
        }
    }
}
