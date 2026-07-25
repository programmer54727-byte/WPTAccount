package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class ScreenType {
    Home, Dashboard, Sale, Purchase, Payment, Receipt, Contra, Journal, CreditNote, DebitNote, Ledger, Exit
}

@Composable
fun AppNavigationDrawer(
    currentScreen: ScreenType,
    companyName: String,
    onNavigate: (ScreenType) -> Unit,
    content: @Composable (PaddingValues, onToggleDrawer: () -> Unit, isDesktop: Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BoxWithConstraints {
        val isDesktop = maxWidth > 840.dp

        if (isDesktop) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                        DrawerContent(currentScreen, companyName, onNavigate)
                    }
                }
            ) {
                content(PaddingValues(0.dp), {}, true)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerContent(currentScreen, companyName) { screen ->
                            scope.launch { drawerState.close() }
                            onNavigate(screen)
                        }
                    }
                }
            ) {
                content(PaddingValues(0.dp), { scope.launch { drawerState.open() } }, false)
            }
        }
    }
}

@Composable
private fun ColumnScope.DrawerContent(
    currentScreen: ScreenType,
    companyName: String,
    onNavigate: (ScreenType) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = companyName,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        color = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("Home") },
        selected = currentScreen == ScreenType.Home,
        onClick = { onNavigate(ScreenType.Home) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
        label = { Text("Dashboard") },
        selected = currentScreen == ScreenType.Dashboard,
        onClick = { onNavigate(ScreenType.Dashboard) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
        text = "Transactions",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
        label = { Text("Sale") },
        selected = currentScreen == ScreenType.Sale,
        onClick = { onNavigate(ScreenType.Sale) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
        label = { Text("Purchase") },
        selected = currentScreen == ScreenType.Purchase,
        onClick = { onNavigate(ScreenType.Purchase) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Payments, contentDescription = null) },
        label = { Text("Payment") },
        selected = currentScreen == ScreenType.Payment,
        onClick = { onNavigate(ScreenType.Payment) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
        label = { Text("Receipt") },
        selected = currentScreen == ScreenType.Receipt,
        onClick = { onNavigate(ScreenType.Receipt) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
        label = { Text("Contra") },
        selected = currentScreen == ScreenType.Contra,
        onClick = { onNavigate(ScreenType.Contra) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Description, contentDescription = null) },
        label = { Text("Journal") },
        selected = currentScreen == ScreenType.Journal,
        onClick = { onNavigate(ScreenType.Journal) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null) },
        label = { Text("Credit Note") },
        selected = currentScreen == ScreenType.CreditNote,
        onClick = { onNavigate(ScreenType.CreditNote) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null) },
        label = { Text("Debit Note") },
        selected = currentScreen == ScreenType.DebitNote,
        onClick = { onNavigate(ScreenType.DebitNote) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
        label = { Text("Ledger") },
        selected = currentScreen == ScreenType.Ledger,
        onClick = { onNavigate(ScreenType.Ledger) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    
    Spacer(Modifier.weight(1f))
    
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    NavigationDrawerItem(
        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
        label = { Text("Exit Company") },
        selected = false,
        onClick = { onNavigate(ScreenType.Exit) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    Spacer(Modifier.height(12.dp))
}
