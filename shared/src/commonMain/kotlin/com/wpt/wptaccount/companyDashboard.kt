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

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDashboard(
    company: Company,
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

                // Sales Graph Card
                GraphCard(
                    title = "Monthly Sales",
                    data = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f),
                    color = MaterialTheme.colorScheme.primary,
                    labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                )

                // Purchase Graph Card
                GraphCard(
                    title = "Monthly Purchases",
                    data = listOf(0.3f, 0.5f, 0.8f, 0.4f, 0.7f, 0.5f),
                    color = MaterialTheme.colorScheme.secondary,
                    labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                )
                
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
