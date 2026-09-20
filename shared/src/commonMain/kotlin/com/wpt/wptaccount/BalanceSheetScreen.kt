package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun BalanceSheetScreen(
    company: Company,
    period: AccountPeriod,
    onBack: () -> Unit,
    onHomeClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var groupTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var allGroups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var netProfit by remember { mutableStateOf(0.0) }
    var closingStock by remember { mutableStateOf(0.0) }

    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                val ledgerBalances = ReportEngine.getFinancialData(company.id!!, period)
                allGroups = supabase.from("groups").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<AccountingGroup>()
                
                val ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()

                groupTotals = ReportEngine.calculateGroupTotals(allGroups, ledgerBalances, ledgers)
                closingStock = ReportEngine.calculateStockValue(company.id!!, period.endDate)

                // Calculate Net Profit = (Total Incomes - Total Expenses)
                // In our system: Dr is positive, Cr is negative.
                // Income groups have Cr balances (negative), Expense groups have Dr balances (positive).
                // Profit = Incomes (absolute) - Expenses (absolute)
                // Profit = -(Sum of Income Balances) - (Sum of Expense Balances)
                var totalIncomes = 0.0
                var totalExpenses = 0.0
                
                allGroups.forEach { group ->
                    val nature = group.nature ?: ""
                    val total = groupTotals[group.id] ?: 0.0
                    if (nature == "Income") totalIncomes += total
                    if (nature == "Expense") totalExpenses += total
                }
                
                // Income is usually Cr (-ve), Expense is usually Dr (+ve)
                // Net Profit = (-totalIncomes) - totalExpenses
                // Note: We also need to consider Stock difference in P&L, but for simple BS balancing:
                // Profit/Loss = -(All Income + All Expense balances)
                netProfit = -(totalIncomes + totalExpenses)
                
            } catch (e: Exception) {
                println("Balance Sheet Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(period) { fetchData() }

    AppNavigationDrawer(
        currentScreen = ScreenType.BalanceSheet,
        companyName = company.company_name,
        onNavigate = { if (it == ScreenType.Home) onHomeClick() else if (it == ScreenType.Exit) onBack() }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = WptColors.AppSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Balance Sheet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${period.startDate.toDisplayDate()} to ${period.endDate.toDisplayDate()}", style = MaterialTheme.typography.labelSmall, color = WptColors.SecondaryText)
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                    val isWide = maxWidth > 800.dp
                    
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // LIABILITIES SIDE (Left)
                            Column(modifier = Modifier.weight(1f)) {
                                ReportHeader("Liabilities", alignment = Alignment.Start)
                                
                                val liabilityGroups = listOf("Capital Account", "Loans (Liability)", "Current Liabilities", "Suspense A/c")
                                val liabilityData = allGroups.filter { g -> liabilityGroups.any { it.equals(g.group_name, true) } }
                                
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(liabilityData) { group ->
                                        val amount = groupTotals[group.id] ?: 0.0
                                        // Liabilities usually have Cr (-ve) balances. Display absolute.
                                        ReportRow(group.group_name, kotlin.math.abs(amount))
                                    }
                                    
                                    // Add Profit & Loss A/c if Profit
                                    if (netProfit > 0) {
                                        item { ReportRow("Profit & Loss A/c (Net Profit)", netProfit, isHighlighted = true) }
                                    }
                                }
                                
                                val totalLiabilities = liabilityData.sumOf { kotlin.math.abs(groupTotals[it.id] ?: 0.0) } + (if (netProfit > 0) netProfit else 0.0)
                                ReportTotal("Total", totalLiabilities)
                            }
                            
                            if (isWide) {
                                VerticalDivider(color = WptColors.Divider, modifier = Modifier.fillMaxHeight().width(1.dp))
                            }

                            // ASSETS SIDE (Right)
                            Column(modifier = Modifier.weight(1f)) {
                                ReportHeader("Assets", alignment = Alignment.End)
                                
                                val assetGroups = listOf("Fixed Assets", "Investments", "Current Assets", "Misc. Expenses (ASSET)")
                                val assetData = allGroups.filter { g -> assetGroups.any { it.equals(g.group_name, true) } }
                                
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(assetData) { group ->
                                        val amount = groupTotals[group.id] ?: 0.0
                                        // Assets usually have Dr (+ve) balances.
                                        ReportRow(group.group_name, amount)
                                    }
                                    
                                    // Special: Closing Stock (calculated from inventory)
                                    item { ReportRow("Closing Stock", closingStock) }
                                    
                                    // Add Profit & Loss A/c if Loss
                                    if (netProfit < 0) {
                                        item { ReportRow("Profit & Loss A/c (Net Loss)", kotlin.math.abs(netProfit), isHighlighted = true) }
                                    }
                                }
                                
                                val totalAssets = assetData.sumOf { groupTotals[it.id] ?: 0.0 } + closingStock + (if (netProfit < 0) kotlin.math.abs(netProfit) else 0.0)
                                ReportTotal("Total", totalAssets)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportHeader(title: String, alignment: Alignment.Horizontal) {
    Surface(
        color = WptColors.PrimaryAccent.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WptColors.PrimaryAccent,
            textAlign = if (alignment == Alignment.Start) TextAlign.Start else TextAlign.End
        )
    }
}

@Composable
fun ReportRow(label: String, amount: Double, isHighlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) WptColors.PrimaryAccent else WptColors.PrimaryText,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = amount.format(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WptColors.PrimaryText
        )
    }
}

@Composable
fun ReportTotal(label: String, amount: Double) {
    Column {
        HorizontalDivider(color = WptColors.Divider)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = WptColors.PrimaryText)
            Text(text = amount.format(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = WptColors.PrimaryAccent)
        }
        HorizontalDivider(thickness = 2.dp, color = WptColors.PrimaryAccent)
    }
}
