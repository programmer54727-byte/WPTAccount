package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun ProfitAndLossScreen(
    company: Company,
    period: AccountPeriod,
    onBack: () -> Unit,
    onHomeClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var groupTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var allGroups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    
    var openingStock by remember { mutableStateOf(0.0) }
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
                
                // Stock values
                // Opening stock is as of start of period (subtract 1 day ideally, or just lte startDate - 1)
                // For simplicity, we use endDate for closing, and a hypothetical "startDate" for opening
                openingStock = ReportEngine.calculateStockValue(company.id!!, period.startDate) 
                closingStock = ReportEngine.calculateStockValue(company.id!!, period.endDate)

            } catch (e: Exception) {
                println("P&L Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(period) { fetchData() }

    AppNavigationDrawer(
        currentScreen = ScreenType.ProfitAndLoss,
        companyName = company.company_name,
        onNavigate = { if (it == ScreenType.Home) onHomeClick() else if (it == ScreenType.Exit) onBack() }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = WptColors.AppSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Profit & Loss A/c", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                val purchaseGrps = allGroups.filter { it.group_name.contains("Purchase", true) || it.group_name.contains("Direct Expenses", true) }
                val salesGrps = allGroups.filter { it.group_name.contains("Sales", true) || it.group_name.contains("Direct Incomes", true) }
                
                val indirectExpGrps = allGroups.filter { it.group_name.contains("Indirect Expenses", true) }
                val indirectIncGrps = allGroups.filter { it.group_name.contains("Indirect Incomes", true) }

                val totalDirectExp = openingStock + purchaseGrps.sumOf { groupTotals[it.id] ?: 0.0 }
                val totalDirectInc = closingStock + salesGrps.sumOf { kotlin.math.abs(groupTotals[it.id] ?: 0.0) }
                
                val grossProfit = totalDirectInc - totalDirectExp
                
                val totalIndirectExp = indirectExpGrps.sumOf { groupTotals[it.id] ?: 0.0 }
                val totalIndirectInc = indirectIncGrps.sumOf { kotlin.math.abs(groupTotals[it.id] ?: 0.0) }
                
                val netProfit = (grossProfit + totalIndirectInc) - totalIndirectExp

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TRADING ACCOUNT SECTION
                    item { ReportHeader("Trading Account", alignment = Alignment.Start) }
                    
                    item { ReportRow("Opening Stock", openingStock) }
                    items(purchaseGrps) { g -> ReportRow(g.group_name, groupTotals[g.id] ?: 0.0) }
                    
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = WptColors.Divider) }
                    
                    items(salesGrps) { g -> ReportRow(g.group_name, kotlin.math.abs(groupTotals[g.id] ?: 0.0)) }
                    item { ReportRow("Closing Stock", closingStock) }
                    
                    item { 
                        ReportRow(
                            label = if (grossProfit >= 0) "Gross Profit c/o" else "Gross Loss c/o", 
                            amount = kotlin.math.abs(grossProfit),
                            isHighlighted = true
                        ) 
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    // INCOME & EXPENDITURE SECTION
                    item { ReportHeader("Income & Expenditure", alignment = Alignment.Start) }
                    
                    item { 
                        ReportRow(
                            label = if (grossProfit >= 0) "Gross Profit b/f" else "Gross Loss b/f", 
                            amount = kotlin.math.abs(grossProfit)
                        ) 
                    }
                    
                    items(indirectExpGrps) { g -> ReportRow(g.group_name, groupTotals[g.id] ?: 0.0) }
                    items(indirectIncGrps) { g -> ReportRow(g.group_name, kotlin.math.abs(groupTotals[g.id] ?: 0.0)) }
                    
                    item { 
                        ReportTotal(
                            label = if (netProfit >= 0) "Net Profit" else "Net Loss", 
                            amount = kotlin.math.abs(netProfit)
                        ) 
                    }
                }
            }
        }
    }
}
