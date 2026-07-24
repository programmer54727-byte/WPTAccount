package com.wpt.wptaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDashboard(
    company: Company,
    onBack: () -> Unit
) {
    val items = listOf(
        DashboardItem("Sale", Icons.Default.ShoppingCart, MaterialTheme.colorScheme.primary),
        DashboardItem("Purchase", Icons.Default.AddShoppingCart, MaterialTheme.colorScheme.secondary),
        DashboardItem("Payment", Icons.Default.Payments, MaterialTheme.colorScheme.tertiary),
        DashboardItem("Receipt", Icons.Default.Receipt, MaterialTheme.colorScheme.error),
        DashboardItem("Ledger", Icons.Default.AccountBalance, MaterialTheme.colorScheme.primary),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(company.company_name)
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { /* Handle click */ },
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
