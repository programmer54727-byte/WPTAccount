package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstDetailsScreen(
    company: Company,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onVoucherListClick: () -> Unit,
    onSaleClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onPaymentClick: () -> Unit = {},
    onReceiptClick: () -> Unit = {},
    onContraClick: () -> Unit = {},
    onJournalClick: () -> Unit = {},
    onBack: () -> Unit
) {
    var gstDetails by remember { mutableStateOf<GstDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Form States
    var regStatus by remember { mutableStateOf("Active") }
    var state by remember { mutableStateOf(company.state ?: "") }
    var regType by remember { mutableStateOf("Regular") }
    var isOtherTerritory by remember { mutableStateOf(false) }
    var gstin by remember { mutableStateOf("") }
    var periodicity by remember { mutableStateOf("Monthly") }
    
    var gstUsername by remember { mutableStateOf("") }
    var modeOfFiling by remember { mutableStateOf("Not Applicable") }
    
    var ewayBillApplicable by remember { mutableStateOf(false) }
    var ewayBillDate by remember { mutableStateOf("") }
    var ewayBillIntrastate by remember { mutableStateOf(false) }
    
    var einvoiceApplicable by remember { mutableStateOf(false) }
    var registrationName by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(company.id) {
        try {
            val result = supabase.from("gst_details").select {
                filter { eq("company_id", company.id!!) }
            }.decodeSingleOrNull<GstDetails>()
            
            if (result != null) {
                gstDetails = result
                regStatus = result.registration_status
                state = result.state ?: state
                regType = result.registration_type ?: "Regular"
                isOtherTerritory = result.is_other_territory_assessee
                gstin = result.gstin_uin ?: ""
                periodicity = result.gstr1_periodicity ?: "Monthly"
                gstUsername = result.gst_username ?: ""
                modeOfFiling = result.filing_mode ?: "Not Applicable"
                ewayBillApplicable = result.eway_bill_applicable
                ewayBillDate = result.eway_bill_date?.toDisplayDate() ?: ""
                ewayBillIntrastate = result.eway_bill_intrastate
                einvoiceApplicable = result.einvoice_applicable
                registrationName = result.registration_name ?: ""
            }
        } catch (e: Exception) {
            println("Error fetching GST details: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    AppNavigationDrawer(
        currentScreen = ScreenType.Gst,
        companyName = company.company_name,
        onNavigate = { screen ->
            when (screen) {
                ScreenType.Home -> onHomeClick()
                ScreenType.Dashboard -> onDashboardClick()
                ScreenType.Exit -> onBack()
                ScreenType.Sale -> onSaleClick()
                ScreenType.Purchase -> onPurchaseClick()
                ScreenType.Payment -> onPaymentClick()
                ScreenType.Receipt -> onReceiptClick()
                ScreenType.Ledger -> onLedgerClick()
                ScreenType.Contra -> onContraClick()
                ScreenType.Journal -> onJournalClick()
                ScreenType.CreditNote -> { /* TODO */ }
                ScreenType.DebitNote -> { /* TODO */ }
                ScreenType.BalanceSheet -> { /* TODO */ }
                ScreenType.ProfitAndLoss -> { /* TODO */ }
                ScreenType.CashFlow -> { /* TODO */ }
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> { /* Already here */ }
                ScreenType.DayBook -> onVoucherListClick()
            }
        }
    )
{ _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GST Details") },
                    navigationIcon = {
                        if (isDesktop) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = onToggleDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        if (!isDesktop) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    saveError = null
                                    isSaving = true
                                    try {
                                        val newDetails = GstDetails(
                                            id = gstDetails?.id,
                                            company_id = company.id!!,
                                            registration_status = regStatus,
                                            state = state,
                                            registration_type = regType,
                                            is_other_territory_assessee = isOtherTerritory,
                                            gstin_uin = gstin,
                                            gstr1_periodicity = periodicity,
                                            gst_username = gstUsername,
                                            filing_mode = modeOfFiling,
                                            eway_bill_applicable = ewayBillApplicable,
                                            eway_bill_date = ewayBillDate.toDbDate().ifEmpty { null },
                                            eway_bill_intrastate = ewayBillIntrastate,
                                            einvoice_applicable = einvoiceApplicable,
                                            registration_name = registrationName
                                        )
                                        supabase.from("gst_details").upsert(newDetails)
                                        onBack()
                                    } catch (e: Exception) {
                                        println("Error saving GST details: ${e.message}")
                                        saveError = "Failed to save data. Please check your connection."
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 800.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (saveError != null) {
                            Text(
                                text = saveError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Registration status : ", modifier = Modifier.width(150.dp))
                            Text(regStatus, fontWeight = FontWeight.Bold)
                        }

                        DividerWithLabel("GST Registration Details")
                        
                        GstField("State", state) { state = it }
                        GstDropdown("Registration type", listOf("Regular", "Composition"), regType) { regType = it }
                        GstSwitch("Assessee of Other Territory", isOtherTerritory) { isOtherTerritory = it }
                        GstField("GSTIN/UIN", gstin) { gstin = it }
                        GstDropdown("Periodicity of GSTR-1", listOf("Monthly", "Quarterly"), periodicity) { periodicity = it }

                        DividerWithLabel("Connected GST Details")
                        GstField("GST Username", gstUsername) { gstUsername = it }
                        GstField("Mode of Filing", modeOfFiling) { modeOfFiling = it }

                        DividerWithLabel("e-Way Bill Details")
                        GstSwitch("e-Way Bill applicable", ewayBillApplicable) { ewayBillApplicable = it }
                        if (ewayBillApplicable) {
                            GstField("Applicable from", ewayBillDate) { ewayBillDate = it }
                            GstSwitch("Applicable for intrastate", ewayBillIntrastate) { ewayBillIntrastate = it }
                        }

                        DividerWithLabel("e-Invoice Details")
                        GstSwitch("e-Invoicing applicable", einvoiceApplicable) { einvoiceApplicable = it }

                        GstField("Registration Name", registrationName) { registrationName = it }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DividerWithLabel(label: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun GstField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label : ", modifier = Modifier.width(200.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
fun GstSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label : ", modifier = Modifier.width(200.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(if (checked) "Yes" else "No", modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun GstDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label : ", modifier = Modifier.width(200.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
