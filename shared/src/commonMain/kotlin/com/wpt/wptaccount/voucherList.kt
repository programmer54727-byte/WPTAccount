package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    period: AccountPeriod,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onSaleClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onPaymentClick: () -> Unit = {},
    onReceiptClick: () -> Unit = {},
    onContraClick: () -> Unit = {},
    onJournalClick: () -> Unit = {},
    onBalanceSheetClick: () -> Unit = {},
    onProfitAndLossClick: () -> Unit = {},
    onCashFlowClick: () -> Unit = {},
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
                    filter { 
                        eq("company_id", company.id!!) 
                        gte("date", period.startDate)
                        lte("date", period.endDate)
                    }
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

    LaunchedEffect(period) { fetchData() }

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
                ScreenType.Payment -> onPaymentClick()
                ScreenType.Receipt -> onReceiptClick()
                ScreenType.Ledger -> onLedgerClick()
                ScreenType.Contra -> onContraClick()
                ScreenType.Journal -> onJournalClick()
                ScreenType.CreditNote -> { /* TODO */ }
                ScreenType.DebitNote -> { /* TODO */ }
                ScreenType.BalanceSheet -> onBalanceSheetClick()
                ScreenType.ProfitAndLoss -> onProfitAndLossClick()
                ScreenType.CashFlow -> onCashFlowClick()
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> { /* Already here */ }
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = Color(0xFFF8F9FA),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = { 
                        Text(
                            text = "Day Book", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        ) 
                    },
                    navigationIcon = {
                        if (isDesktop) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WptColors.PrimaryAccent)
                            }
                        } else {
                            IconButton(onClick = onToggleDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = WptColors.PrimaryAccent)
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
                    placeholder = { Text("Search by Party or Type", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = WptColors.PrimaryAccent) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WptColors.PrimaryAccent,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WptColors.PrimaryAccent)
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                        val contentWidth = 1000.dp
                        
                        // Header
                        Row(
                            modifier = Modifier.width(contentWidth).padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color(0xFF49454F))
                            Text("Voucher Type", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color(0xFF49454F))
                            Text("Vch No.", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color(0xFF49454F))
                            Text("Particulars", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color(0xFF49454F))
                            Text("Amount", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End, color = Color(0xFF49454F))
                        }

                        LazyColumn(
                            modifier = Modifier.width(contentWidth).fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredVouchers) { voucher ->
                                val partyName = ledgers.find { it.id == voucher.party_ledger_id }?.ledger_name ?: "Direct Entry"
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left accent strip
                                        val accentColor = when(voucher.voucher_type) {
                                            "Sale" -> Color(0xFF1E88E5)
                                            "Purchase" -> Color(0xFF7E57C2)
                                            "Payment" -> Color(0xFF546E7A)
                                            "Receipt" -> Color(0xFFE53935)
                                            "Contra" -> Color(0xFF00897B)
                                            "Journal" -> Color(0xFFFFB300)
                                            else -> WptColors.PrimaryAccent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(4.dp)
                                                .background(accentColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(voucher.date.toDisplayDate(), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                            Text(voucher.voucher_type, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = accentColor)
                                            Text(voucher.voucher_number ?: "-", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                            Text(partyName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1B20))
                                            Text(voucher.total_amount.format(), modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D1B20))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
