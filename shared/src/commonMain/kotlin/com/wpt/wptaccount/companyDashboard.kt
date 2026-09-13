package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDashboard(
    company: Company,
    period: AccountPeriod,
    onHomeClick: () -> Unit,
    onVoucherListClick: () -> Unit,
    onStockClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    AppNavigationDrawer(
        currentScreen = ScreenType.Dashboard,
        companyName = company.company_name,
        onNavigate = { screen ->
            when (screen) {
                ScreenType.Home -> onHomeClick()
                ScreenType.Dashboard -> { /* Already here */ }
                ScreenType.Exit -> onBack()
                ScreenType.Sale -> { /* TODO */ }
                ScreenType.Purchase -> { /* TODO */ }
                ScreenType.Payment -> { /* TODO */ }
                ScreenType.Receipt -> { /* TODO */ }
                ScreenType.Ledger -> { /* TODO */ }
                ScreenType.Contra -> { /* TODO */ }
                ScreenType.Journal -> { /* TODO */ }
                ScreenType.CreditNote -> { /* TODO */ }
                ScreenType.DebitNote -> { /* TODO */ }
                ScreenType.BalanceSheet -> { /* TODO */ }
                ScreenType.ProfitAndLoss -> { /* TODO */ }
                ScreenType.CashFlow -> { /* TODO */ }
                ScreenType.Stock -> onStockClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(company.company_name)
                            Text(
                                text = "Analytics Dashboard",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (!isDesktop) {
                            IconButton(onClick = onToggleDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            var salesData by remember { mutableStateOf<List<Float>>(emptyList()) }
            var purchaseData by remember { mutableStateOf<List<Float>>(emptyList()) }
            var labels by remember { mutableStateOf<List<String>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            val scope = rememberCoroutineScope()

            fun fetchData() {
                scope.launch {
                    try {
                        isLoading = true
                        val vouchers = supabase.from("vouchers").select {
                            filter { 
                                eq("company_id", company.id!!)
                                gte("date", period.startDate)
                                lte("date", period.endDate)
                            }
                        }.decodeList<Voucher>()

                        // Group by Month
                        val monthlySales = mutableMapOf<String, Double>()
                        val monthlyPurchases = mutableMapOf<String, Double>()
                        
                        vouchers.forEach { v ->
                            val monthLabel = v.date.toMonthYearLabel()
                            if (v.voucher_type == "Sale") {
                                monthlySales[monthLabel] = (monthlySales[monthLabel] ?: 0.0) + v.total_amount
                            } else if (v.voucher_type == "Purchase") {
                                monthlyPurchases[monthLabel] = (monthlyPurchases[monthLabel] ?: 0.0) + v.total_amount
                            }
                        }

                        val allMonths = (monthlySales.keys + monthlyPurchases.keys).distinct().sorted() // Sorted might be alphabetical, better to sort by date
                        labels = allMonths
                        
                        val maxVal = (monthlySales.values + monthlyPurchases.values).maxOrNull() ?: 1.0
                        salesData = allMonths.map { (monthlySales[it] ?: 0.0).toFloat() / maxVal.toFloat() }
                        purchaseData = allMonths.map { (monthlyPurchases[it] ?: 0.0).toFloat() / maxVal.toFloat() }

                    } catch (e: Exception) {
                        println("Dashboard error: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(period) { fetchData() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Business Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (labels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No data available for this period", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    // Sales Graph Card
                    GraphCard(
                        title = "Monthly Sales",
                        data = salesData,
                        color = MaterialTheme.colorScheme.primary,
                        labels = labels
                    )

                    // Purchase Graph Card
                    GraphCard(
                        title = "Monthly Purchases",
                        data = purchaseData,
                        color = MaterialTheme.colorScheme.secondary,
                        labels = labels
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GraphCard(
    title: String,
    data: List<Float>,
    color: Color,
    labels: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(value)
                                .width(30.dp)
                                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
