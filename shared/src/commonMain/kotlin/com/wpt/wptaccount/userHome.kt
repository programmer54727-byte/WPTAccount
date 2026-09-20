package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import io.github.jan.supabase.postgrest.from
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHome(
    company: Company,
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
    onBack: () -> Unit,
    currentPeriod: AccountPeriod,
    onPeriodChange: (AccountPeriod) -> Unit
) {
    var isInitializing by rememberSaveable { mutableStateOf(false) }
    var showPeriodDialog by rememberSaveable { mutableStateOf(false) }

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
        DashboardItem("Balance Sheet", Icons.Default.PieChart, androidx.compose.ui.graphics.Color(0xFF1E88E5)),
        DashboardItem("Profit & Loss", Icons.AutoMirrored.Filled.TrendingUp, androidx.compose.ui.graphics.Color(0xFF43A047)),
        DashboardItem("Cash Flow", Icons.Default.AccountBalanceWallet, androidx.compose.ui.graphics.Color(0xFFF4511E)),
        DashboardItem("Stock Summary", Icons.Default.Layers, androidx.compose.ui.graphics.Color(0xFF7E57C2)),
        DashboardItem("Sale", Icons.Default.ShoppingCart, androidx.compose.ui.graphics.Color(0xFF1E88E5)),
        DashboardItem("Purchase", Icons.Default.ShoppingCart, androidx.compose.ui.graphics.Color(0xFF7E57C2)),
        DashboardItem("Payment", Icons.Default.Savings, androidx.compose.ui.graphics.Color(0xFF546E7A)),
        DashboardItem("Receipt", Icons.Default.ArrowDownward, androidx.compose.ui.graphics.Color(0xFFE53935)),
        DashboardItem("Contra", Icons.Default.Autorenew, androidx.compose.ui.graphics.Color(0xFF00897B)),
        DashboardItem("Journal", Icons.AutoMirrored.Filled.Assignment, androidx.compose.ui.graphics.Color(0xFFFFB300)),
        DashboardItem("Credit Note", Icons.AutoMirrored.Filled.AssignmentReturn, androidx.compose.ui.graphics.Color(0xFF8E24AA)),
        DashboardItem("Debit Note", Icons.AutoMirrored.Filled.Assignment, androidx.compose.ui.graphics.Color(0xFF00ACC1)),
        DashboardItem("Ledger", Icons.Default.AccountBalance, androidx.compose.ui.graphics.Color(0xFF5E35B1)),
        DashboardItem("Day Book", Icons.AutoMirrored.Filled.Assignment, androidx.compose.ui.graphics.Color(0xFF78909C)),
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
                    ScreenType.Sale -> onSaleClick()
                    ScreenType.Purchase -> onPurchaseClick()
                    ScreenType.Payment -> onPaymentClick()
                    ScreenType.Receipt -> onReceiptClick()
                    ScreenType.Ledger -> onLedgerClick()
                    ScreenType.DayBook -> onVoucherListClick()
                    ScreenType.Contra -> onContraClick()
                    ScreenType.Journal -> onJournalClick()
                    ScreenType.CreditNote -> { /* TODO */ }
                    ScreenType.DebitNote -> { /* TODO */ }
                    ScreenType.BalanceSheet -> { /* TODO */ }
                    ScreenType.ProfitAndLoss -> { /* TODO */ }
                    ScreenType.CashFlow -> { /* TODO */ }
                    ScreenType.Stock -> onStockSummaryClick()
                    ScreenType.Gst -> onGstDetailsClick()
                }
            }
        ) { _, onToggleDrawer, isDesktop ->
            Scaffold(
                containerColor = WptColors.AppSurface,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        title = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CorporateFare,
                                    contentDescription = null,
                                    tint = WptColors.PrimaryAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = company.company_name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { showPeriodDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFF49454F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${currentPeriod.startDate.toDisplayDate()} to ${currentPeriod.endDate.toDisplayDate()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF49454F)
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (!isDesktop) {
                                IconButton(onClick = onToggleDrawer) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = onGstDetailsClick,
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF1D1B20),
                                    containerColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = WptColors.PrimaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GST Detail", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    )
                }
            )
{ padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
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
                                    "Ledger" -> onLedgerClick()
                                    "Day Book" -> onVoucherListClick()
                                    "Sale" -> onSaleClick()
                                    "Purchase" -> onPurchaseClick()
                                    "Payment" -> onPaymentClick()
                                    "Receipt" -> onReceiptClick()
                                    "Contra" -> onContraClick()
                                    "Journal" -> onJournalClick()
                                    "Balance Sheet", "Profit & Loss", "Cash Flow" -> { /* TODO */ }
                                    "Credit Note", "Debit Note" -> { /* TODO */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPeriodDialog) {
        var start by remember { mutableStateOf(currentPeriod.startDate.toDisplayDate()) }
        var end by remember { mutableStateOf(currentPeriod.endDate.toDisplayDate()) }

        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            title = { Text("Change Period") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Date (DD/MM/YYYY)") },
                        modifier = Modifier.onFocusChanged { 
                            if (!it.isFocused && start.isNotEmpty()) start = start.formatSmartDate()
                        }
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Date (DD/MM/YYYY)") },
                        modifier = Modifier.onFocusChanged { 
                            if (!it.isFocused && end.isNotEmpty()) end = end.formatSmartDate()
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPeriodChange(AccountPeriod(start.toDbDate(), end.toDbDate()))
                    showPeriodDialog = false
                }) { Text("Change") }
            },
            dismissButton = {
                TextButton(onClick = { showPeriodDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TransactionCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color strip indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(item.color, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Soft rounded backing circle for high fidelity icon contrast
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(item.color.copy(alpha = 0.08f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1D1B20)
            )
        }
    }
}
