package com.wpt.wptaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import io.github.jan.supabase.postgrest.from
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHome(
    company: Company,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onBack: () -> Unit
) {
    var isInitializing by remember { mutableStateOf(false) }

    LaunchedEffect(company.id) {
        val companyId = company.id ?: return@LaunchedEffect
        try {
            // Check if groups already exist
            val groups = supabase.from("groups")
                .select { 
                    filter { eq("company_id", companyId) }
                    limit(1)
                }.decodeList<AccountingGroup>()
            
            if (groups.isEmpty()) {
                isInitializing = true
                initializeCompanySetup(companyId)
            }
        } catch (e: Exception) {
            println("Error during Smart Check: ${e.message}")
        } finally {
            isInitializing = false
        }
    }

    val items = listOf(
        DashboardItem("Balance Sheet", Icons.Default.AccountBalance, Color(0xFF3F51B5)),
        DashboardItem("Profit & Loss", Icons.Default.Description, Color(0xFF4CAF50)),
        DashboardItem("Cash Flow", Icons.Default.SyncAlt, Color(0xFFFF9800)),
        DashboardItem("Stock Summary", Icons.Default.Inventory, Color(0xFF795548)),
        DashboardItem("Sale", Icons.Default.ShoppingCart, MaterialTheme.colorScheme.primary),
        DashboardItem("Purchase", Icons.Default.AddShoppingCart, MaterialTheme.colorScheme.secondary),
        DashboardItem("Payment", Icons.Default.Payments, MaterialTheme.colorScheme.tertiary),
        DashboardItem("Receipt", Icons.Default.Receipt, MaterialTheme.colorScheme.error),
        DashboardItem("Contra", Icons.Default.SyncAlt, Color(0xFF009688)),
        DashboardItem("Journal", Icons.Default.Description, Color(0xFFFF5722)),
        DashboardItem("Credit Note", Icons.AutoMirrored.Filled.AssignmentReturn, Color(0xFF9C27B0)),
        DashboardItem("Debit Note", Icons.AutoMirrored.Filled.KeyboardReturn, Color(0xFF00BCD4)),
        DashboardItem("Ledger", Icons.Default.AccountBalance, MaterialTheme.colorScheme.primary),

    )

    if (isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Initializing Accounting Groups...")
            }
        }
    } else {
        AppNavigationDrawer(
            currentScreen = ScreenType.Home,
            companyName = company.company_name,
            onNavigate = { screen ->
                when (screen) {
                    ScreenType.Home -> { /* Already here */ }
                    ScreenType.Dashboard -> onDashboardClick()
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
                    ScreenType.Stock -> onStockSummaryClick()
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
                                    text = "Home",
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Masters & Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items) { item ->
                            TransactionCard(item) {
                                when (item.title) {
                                    "Dashboard" -> onDashboardClick()
                                    "Stock Summary" -> onStockSummaryClick()
                                    "Balance Sheet", "Profit & Loss", "Cash Flow" -> { /* TODO */ }
                                    "Sale", "Purchase", "Payment", "Receipt", "Contra", "Journal", "Credit Note", "Debit Note", "Ledger" -> { /* TODO */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = item.color
            )
        }
    }
}
