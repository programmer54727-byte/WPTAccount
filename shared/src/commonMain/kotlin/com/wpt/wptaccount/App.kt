package com.wpt.wptaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val CompanySaver = Saver<Company?, String>(
    save = { company -> if (company != null) Json.encodeToString(company) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<Company>(value) else null }
)

@Composable
fun App(onOrientationRequest: (ScreenOrientation) -> Unit = {}) {
    var currentScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCompany by rememberSaveable(stateSaver = CompanySaver) { mutableStateOf<Company?>(null) }

    val sessionStatus by supabase.auth.sessionStatus.collectAsState()

    LaunchedEffect(currentScreen) {
        if (currentScreen == "inventory_management") {
            onOrientationRequest(ScreenOrientation.LANDSCAPE)
        } else {
            onOrientationRequest(ScreenOrientation.UNSPECIFIED)
        }
    }

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                if (currentScreen == null || currentScreen == "landing" || currentScreen == "login" || currentScreen == "signup") {
                    currentScreen = "company_list"
                }
            }
            is SessionStatus.NotAuthenticated -> {
                if (currentScreen == null || currentScreen == "company_list" || currentScreen == "company_home" || currentScreen == "company_dashboard") {
                    currentScreen = "landing"
                }
            }
            else -> {}
        }
    }

    MaterialTheme {
        CompositionLocalProvider(LocalScreenOrientation provides onOrientationRequest) {
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
                                onSignUpSuccess = { currentScreen = "company_list" }
                            )
                        }
                        "login" -> {
                            Login(
                                onBackClick = { currentScreen = "landing" },
                                onLoginSuccess = { currentScreen = "company_list" }
                            )
                        }
                        "company_list" -> {
                            CompanyList(
                                onCreateCompanyClick = { currentScreen = "create_company" },
                                onCompanyClick = { 
                                    selectedCompany = it
                                    currentScreen = "company_home"
                                },
                                onLogout = { currentScreen = "landing" }
                            )
                        }
                        "create_company" -> {
                            CreateCompanyForm(
                                onBackClick = { currentScreen = "company_list" },
                                onSuccess = { currentScreen = "company_list" }
                            )
                        }
                        "company_home" -> {
                            selectedCompany?.let { company ->
                                UserHome(
                                    company = company,
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onBack = { currentScreen = "company_list" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "company_dashboard" -> {
                            selectedCompany?.let { company ->
                                CompanyDashboard(
                                    company = company,
                                    onHomeClick = { currentScreen = "company_home" },
                                    onStockClick = { currentScreen = "inventory_management" },
                                    onBack = { currentScreen = "company_list" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "gst_details" -> {
                            selectedCompany?.let { company ->
                                GstDetailsScreen(
                                    company = company,
                                    onBack = { currentScreen = "company_home" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "inventory_management" -> {
                            selectedCompany?.let { company ->
                                InventoryManagement(
                                    company = company,
                                    onBack = { currentScreen = "company_home" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
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
