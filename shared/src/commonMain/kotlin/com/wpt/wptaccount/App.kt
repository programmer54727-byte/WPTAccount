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
import kotlinx.serialization.json.Json

@Composable
fun App(onOrientationRequest: (ScreenOrientation) -> Unit = {}) {
    var currentScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCompany by rememberSaveable(stateSaver = CompanySaver) { mutableStateOf<Company?>(null) }
    
    // New states for voucher drill-down
    var currentVoucherType by rememberSaveable { mutableStateOf("") }
    var currentMonthInt by rememberSaveable { mutableStateOf(0) }
    var editingVoucher by rememberSaveable(stateSaver = VoucherSaver) { mutableStateOf<Voucher?>(null) }
    var selectedPeriod by rememberSaveable(stateSaver = PeriodSaver) { mutableStateOf<AccountPeriod?>(null) }
    var companyToEdit by rememberSaveable(stateSaver = CompanySaver) { mutableStateOf<Company?>(null) }

    val sessionStatus by supabase.auth.sessionStatus.collectAsState()

    LaunchedEffect(currentScreen) {
        onOrientationRequest(ScreenOrientation.UNSPECIFIED)
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
            BackHandler(enabled = currentScreen != null && currentScreen != "landing" && currentScreen != "company_list") {
                when (currentScreen) {
                    "inventory_management", "gst_details", "company_dashboard", "ledger_management", "voucher_list", "voucher_summary" -> currentScreen = "company_home"
                    "voucher_month_list" -> currentScreen = "voucher_summary"
                    "voucher_sale", "voucher_purchase", "voucher_payment", "voucher_receipt", "voucher_contra", "voucher_journal" -> {
                        currentScreen = "voucher_month_list"
                        editingVoucher = null
                    }
                    "company_home" -> currentScreen = "company_list"
                    "create_company" -> {
                        currentScreen = "company_list"
                        companyToEdit = null
                    }
                    "login", "signup" -> currentScreen = "landing"
                }
            }

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
                                onCreateCompanyClick = { 
                                    companyToEdit = null
                                    currentScreen = "create_company" 
                                },
                                onEditCompanyClick = { company ->
                                    companyToEdit = company
                                    currentScreen = "create_company"
                                },
                                onCompanyClick = { 
                                    selectedCompany = it
                                    selectedPeriod = it.getDefaultPeriod()
                                    currentScreen = "company_home"
                                },
                                onLogout = { currentScreen = "landing" }
                            )
                        }
                        "create_company" -> {
                            CreateCompanyForm(
                                onBackClick = { 
                                    currentScreen = "company_list" 
                                    companyToEdit = null
                                },
                                onSuccess = { 
                                    currentScreen = "company_list" 
                                    companyToEdit = null
                                },
                                initialCompany = companyToEdit
                            )
                        }
                        "company_home" -> {
                            selectedCompany?.let { company ->
                                UserHome(
                                    company = company,
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { 
                                        currentVoucherType = "Sale"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onPurchaseClick = { 
                                        currentVoucherType = "Purchase"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onPaymentClick = { 
                                        currentVoucherType = "Payment"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onReceiptClick = { 
                                        currentVoucherType = "Receipt"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onContraClick = { 
                                        currentVoucherType = "Contra"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onJournalClick = { 
                                        currentVoucherType = "Journal"
                                        currentScreen = "voucher_summary" 
                                    },
                                    onBack = { currentScreen = "company_list" },
                                    currentPeriod = selectedPeriod!!,
                                    onPeriodChange = { selectedPeriod = it }
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
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onStockClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
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
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
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
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { currentScreen = "company_home" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "ledger_management" -> {
                            selectedCompany?.let { company ->
                                LedgerManagement(
                                    company = company,
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { currentScreen = "company_home" },
                                    currentPeriod = selectedPeriod!!,
                                    onPeriodChange = { selectedPeriod = it }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "voucher_sale" -> {
                            selectedCompany?.let { company ->
                                VoucherEntryScreen(
                                    company = company,
                                    voucherType = "Sale",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "voucher_purchase" -> {
                            selectedCompany?.let { company ->
                                VoucherEntryScreen(
                                    company = company,
                                    voucherType = "Purchase",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "voucher_payment" -> {
                            selectedCompany?.let { company ->
                                AccountingVoucherEntryScreen(
                                    company = company,
                                    voucherType = "Payment",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run { currentScreen = "company_list" }
                        }
                        "voucher_receipt" -> {
                            selectedCompany?.let { company ->
                                AccountingVoucherEntryScreen(
                                    company = company,
                                    voucherType = "Receipt",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run { currentScreen = "company_list" }
                        }
                        "voucher_contra" -> {
                            selectedCompany?.let { company ->
                                AccountingVoucherEntryScreen(
                                    company = company,
                                    voucherType = "Contra",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run { currentScreen = "company_list" }
                        }
                        "voucher_journal" -> {
                            selectedCompany?.let { company ->
                                AccountingVoucherEntryScreen(
                                    company = company,
                                    voucherType = "Journal",
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onVoucherListClick = { currentScreen = "voucher_list" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { 
                                        currentScreen = "voucher_month_list" 
                                        editingVoucher = null
                                    },
                                    initialVoucher = editingVoucher
                                )
                            } ?: run { currentScreen = "company_list" }
                        }
                        "voucher_list" -> {
                            selectedCompany?.let { company ->
                                VoucherListScreen(
                                    company = company,
                                    onHomeClick = { currentScreen = "company_home" },
                                    onDashboardClick = { currentScreen = "company_dashboard" },
                                    onStockSummaryClick = { currentScreen = "inventory_management" },
                                    onGstDetailsClick = { currentScreen = "gst_details" },
                                    onLedgerClick = { currentScreen = "ledger_management" },
                                    onSaleClick = { currentScreen = "voucher_sale" },
                                    onPurchaseClick = { currentScreen = "voucher_purchase" },
                                    onPaymentClick = { currentScreen = "voucher_payment" },
                                    onReceiptClick = { currentScreen = "voucher_receipt" },
                                    onContraClick = { currentScreen = "voucher_contra" },
                                    onJournalClick = { currentScreen = "voucher_journal" },
                                    onBack = { currentScreen = "company_home" }
                                )
                            } ?: run {
                                currentScreen = "company_list"
                            }
                        }
                        "voucher_summary" -> {
                            selectedCompany?.let { company ->
                                VoucherMonthlySummary(
                                    company = company,
                                    voucherType = currentVoucherType,
                                    period = selectedPeriod!!,
                                    onMonthClick = { month ->
                                        currentMonthInt = month
                                        currentScreen = "voucher_month_list"
                                    },
                                    onBack = { currentScreen = "company_home" }
                                )
                            } ?: run { currentScreen = "company_list" }
                        }
                        "voucher_month_list" -> {
                            selectedCompany?.let { company ->
                                VoucherTypeMonthList(
                                    company = company,
                                    voucherType = currentVoucherType,
                                    monthInt = currentMonthInt,
                                    period = selectedPeriod!!,
                                    onVoucherClick = { voucher ->
                                        editingVoucher = voucher
                                        currentScreen = when (currentVoucherType) {
                                            "Sale" -> "voucher_sale"
                                            "Purchase" -> "voucher_purchase"
                                            "Payment" -> "voucher_payment"
                                            "Receipt" -> "voucher_receipt"
                                            "Contra" -> "voucher_contra"
                                            "Journal" -> "voucher_journal"
                                            else -> "company_home"
                                        }
                                    },
                                    onAddClick = {
                                        editingVoucher = null
                                        currentScreen = when (currentVoucherType) {
                                            "Sale" -> "voucher_sale"
                                            "Purchase" -> "voucher_purchase"
                                            "Payment" -> "voucher_payment"
                                            "Receipt" -> "voucher_receipt"
                                            "Contra" -> "voucher_contra"
                                            "Journal" -> "voucher_journal"
                                            else -> "company_home"
                                        }
                                    },
                                    onBack = { currentScreen = "voucher_summary" }
                                )
                            } ?: run { currentScreen = "company_list" }
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
