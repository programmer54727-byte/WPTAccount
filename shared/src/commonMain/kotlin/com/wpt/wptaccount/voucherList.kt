package com.wpt.wptaccount

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherListScreen(
    company: Company,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onSaleClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onBack: () -> Unit
) {
    var vouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                vouchers = supabase.from("vouchers").select {
                    filter { eq("company_id", company.id!!) }
                    order("date", order = Order.DESCENDING)
                }.decodeList<Voucher>()

                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()
            } catch (e: Exception) {
                println("Fetch vouchers error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val filteredVouchers = vouchers.filter { voucher ->
        val partyName = ledgers.find { it.id == voucher.party_ledger_id }?.ledger_name ?: ""
        partyName.contains(searchQuery, ignoreCase = true) || 
        voucher.voucher_type.contains(searchQuery, ignoreCase = true) ||
        (voucher.voucher_number ?: "").contains(searchQuery, ignoreCase = true)
    }

    AppNavigationDrawer(
        currentScreen = ScreenType.DayBook,
        companyName = company.company_name,
        onNavigate = { screen ->
            when (screen) {
                ScreenType.Home -> onHomeClick()
                ScreenType.Dashboard -> onDashboardClick()
                ScreenType.Exit -> onBack()
                ScreenType.Sale -> onSaleClick()
                ScreenType.Purchase -> onPurchaseClick()
                ScreenType.Payment -> { /* TODO */ }
                ScreenType.Receipt -> { /* TODO */ }
                ScreenType.Ledger -> onLedgerClick()
                ScreenType.Contra -> { /* TODO */ }
                ScreenType.Journal -> { /* TODO */ }
                ScreenType.CreditNote -> { /* TODO */ }
                ScreenType.DebitNote -> { /* TODO */ }
                ScreenType.BalanceSheet -> { /* TODO */ }
                ScreenType.ProfitAndLoss -> { /* TODO */ }
                ScreenType.CashFlow -> { /* TODO */ }
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> { /* Already here */ }
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Day Book") },
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
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search by Party or Type") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                        val contentWidth = 900.dp
                        
                        // Header
                        Row(
                            modifier = Modifier.width(contentWidth).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("Voucher Type", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("Vch No.", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("Particulars", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        }
                        HorizontalDivider()

                        LazyColumn(modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                            items(filteredVouchers) { voucher ->
                                val partyName = ledgers.find { it.id == voucher.party_ledger_id }?.ledger_name ?: "Direct Entry"
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(voucher.date, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(voucher.voucher_type, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(voucher.voucher_number ?: "-", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(partyName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(voucher.total_amount.format(), modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
