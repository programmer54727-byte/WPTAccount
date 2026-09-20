package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowScreen(
    company: Company,
    period: AccountPeriod,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onVoucherListClick: () -> Unit,
    onSaleClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onPaymentClick: () -> Unit = {},
    onReceiptClick: () -> Unit = {},
    onContraClick: () -> Unit = {},
    onJournalClick: () -> Unit = {},
    onCreditNoteClick: () -> Unit = {},
    onDebitNoteClick: () -> Unit = {},
    onBalanceSheetClick: () -> Unit = {},
    onProfitAndLossClick: () -> Unit = {},
    onCashFlowClick: () -> Unit = {},
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var monthlyDataMap by remember { mutableStateOf<Map<Int, MonthlyLedgerData>>(emptyMap()) }
    val monthSequence = listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3)
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                monthlyDataMap = ReportEngine.getCashFlowData(company.id!!, period)
            } catch (e: Exception) {
                println("Cash Flow Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(period) { fetchData() }

    AppNavigationDrawer(
        currentScreen = ScreenType.CashFlow,
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
                ScreenType.DayBook -> onVoucherListClick()
                ScreenType.Contra -> onContraClick()
                ScreenType.Journal -> onJournalClick()
                ScreenType.CreditNote -> onCreditNoteClick()
                ScreenType.DebitNote -> onDebitNoteClick()
                ScreenType.BalanceSheet -> onBalanceSheetClick()
                ScreenType.ProfitAndLoss -> onProfitAndLossClick()
                ScreenType.CashFlow -> { /* Already here */ }
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = WptColors.AppSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Cash Flow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Monthly Cash & Bank Movement", style = MaterialTheme.typography.labelSmall, color = WptColors.SecondaryText)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = if (isDesktop) onBack else onToggleDrawer) {
                            Icon(if (isDesktop) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, contentDescription = null, tint = WptColors.PrimaryAccent)
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WptColors.PrimaryAccent)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Month", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WptColors.SecondaryText)
                        Text("Inflow", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WptColors.SecondaryText)
                        Text("Outflow", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WptColors.SecondaryText)
                        Text("Net Flow", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WptColors.SecondaryText)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(monthSequence) { m ->
                            val data = monthlyDataMap[m] ?: MonthlyLedgerData("")
                            val netFlow = data.debit - data.credit
                            
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(WptColors.PrimaryAccent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(data.monthName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(if (data.debit != 0.0) data.debit.format() else "-", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                                        Text(if (data.credit != 0.0) data.credit.format() else "-", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = netFlow.formatWithSign(), 
                                            modifier = Modifier.weight(1.5f), 
                                            textAlign = TextAlign.End, 
                                            style = MaterialTheme.typography.bodyLarge, 
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netFlow >= 0) Color(0xFF43A047) else Color(0xFFD32F2F)
                                        )
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
