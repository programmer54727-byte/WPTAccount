package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.DialogProperties

data class MonthlyStockData(
    val monthName: String,
    var inwardQty: Double = 0.0,
    var inwardValue: Double = 0.0,
    var outwardQty: Double = 0.0,
    var outwardValue: Double = 0.0,
    var closingQty: Double = 0.0,
    var closingValue: Double = 0.0
)

@Composable
fun InventoryField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelWidth: Dp = 150.dp,
    onValueChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 4.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = "$label : ", 
                style = MaterialTheme.typography.bodySmall, 
                modifier = Modifier.width(labelWidth),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun InventoryDropdown(label: String, options: List<String>, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 4.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = "$label : ", 
                style = MaterialTheme.typography.bodySmall, 
                modifier = Modifier.width(150.dp),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            ) {
                Text(selected, style = MaterialTheme.typography.bodySmall)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagement(
    company: Company,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onVoucherListClick: () -> Unit,
    onSaleClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onPaymentClick: () -> Unit = {},
    onReceiptClick: () -> Unit = {},
    onContraClick: () -> Unit = {},
    onJournalClick: () -> Unit = {},
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Units", "Groups", "Items")

    AppNavigationDrawer(
        currentScreen = ScreenType.Stock,
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
                ScreenType.BalanceSheet -> { /* TODO */ }
                ScreenType.ProfitAndLoss -> { /* TODO */ }
                ScreenType.CashFlow -> { /* TODO */ }
                ScreenType.Stock -> { /* Already here */ }
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Inventory: ${company.company_name}", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        if (isDesktop) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = onToggleDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        if (!isDesktop) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> UnitsTab(company)
                    1 -> StockGroupsTab(company)
                    2 -> StockItemsTab(company)
                }
            }
        }
    }
}

@Composable
fun UnitsTab(company: Company) {
    var units by remember { mutableStateOf<List<UnitOfMeasure>>(emptyList()) }
    var items by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var selectedUnitForItems by remember { mutableStateOf<UnitOfMeasure?>(null) }
    
    // Selection and Navigation
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    
    var showDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<UnitOfMeasure?>(null) }
    var unitToEdit by remember { mutableStateOf<UnitOfMeasure?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var symbol by remember { mutableStateOf("") }
    var formalName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            units = supabase.from("units").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<UnitOfMeasure>()
            
            items = supabase.from("stock_items").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockItem>()
        }
    }

    LaunchedEffect(Unit) { 
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = selectedUnitForItems != null) {
        selectedUnitForItems = null
    }

    if (selectedUnitForItems != null) {
        FilteredStockItemsList(
            company = company,
            title = "Items for ${selectedUnitForItems!!.unit_symbol}",
            items = items.filter { it.unit_id == selectedUnitForItems!!.id },
            units = units,
            onBack = { selectedUnitForItems = null }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (selectedIndex < units.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (units.isNotEmpty()) selectedUnitForItems = units[selectedIndex]
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 70.dp else 100.dp
            val rateWidth = if (isMobile) 70.dp else 100.dp
            val valueWidth = if (isMobile) 90.dp else 120.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                val constraints = this@BoxWithConstraints
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                            Text("Unit Symbol", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            if (!isMobile) Text("Formal Name", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Quantity", modifier = Modifier.width(qtyWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Avg Rate", modifier = Modifier.width(rateWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Value", modifier = Modifier.width(valueWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(units) { index, unit ->
                                val unitItems = items.filter { it.unit_id == unit.id }
                                val totalQty = unitItems.sumOf { it.current_quantity }
                                val totalValue = unitItems.sumOf { it.current_quantity * it.opening_rate }
                                val avgRate = if (totalQty > 0) totalValue / totalQty else 0.0

                                Surface(
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clickable { 
                                            if (selectedIndex == index) {
                                                selectedUnitForItems = unit
                                            } else {
                                                selectedIndex = index
                                            }
                                        }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(unit.unit_symbol, modifier = Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                        if (!isMobile) Text(unit.formal_name ?: "", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Text(totalQty.format(), modifier = Modifier.width(qtyWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                        Text(avgRate.format(), modifier = Modifier.width(rateWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                        Text(totalValue.format(), modifier = Modifier.width(valueWidth), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        
                                        IconButton(
                                            onClick = { 
                                                unitToEdit = unit
                                                symbol = unit.unit_symbol
                                                formalName = unit.formal_name ?: ""
                                                showDialog = true 
                                            }, 
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Unit", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }

                                        IconButton(onClick = { unitToDelete = unit }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Unit")
                }
            }
        }
    }

    if (unitToDelete != null) {
        AlertDialog(
            onDismissRequest = { unitToDelete = null },
            title = { Text("Delete Unit") },
            text = { Text("Are you sure you want to delete unit '${unitToDelete?.unit_symbol}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            unitToDelete?.id?.let { id ->
                                // Check for usage in stock items
                                val usages = supabase.from("stock_items").select {
                                    filter { eq("unit_id", id) }
                                    limit(1)
                                }.decodeList<StockItem>()

                                if (usages.isNotEmpty()) {
                                    errorMsg = "Cannot delete unit '${unitToDelete?.unit_symbol}' because it is being used by one or more stock items."
                                } else {
                                    supabase.from("units").delete {
                                        filter { eq("id", id) }
                                    }
                                    fetchData()
                                }
                            }
                            unitToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { unitToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Cannot Delete") },
            text = { Text(errorMsg!!) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) { Text("OK") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                unitToEdit = null
                symbol = ""; formalName = ""
            },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(if (unitToEdit != null) "Edit Unit of Measure" else "Add Unit of Measure") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = symbol, 
                        onValueChange = { symbol = it }, 
                        label = { Text("Symbol (e.g. Pcs)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formalName, 
                        onValueChange = { formalName = it }, 
                        label = { Text("Formal Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val unit = UnitOfMeasure(
                            id = unitToEdit?.id,
                            company_id = company.id!!, 
                            unit_symbol = symbol, 
                            formal_name = formalName
                        )
                        if (unitToEdit != null) {
                            supabase.from("units").update(unit) {
                                filter { eq("id", unitToEdit!!.id!!) }
                            }
                        } else {
                            supabase.from("units").insert(unit)
                        }
                        showDialog = false
                        unitToEdit = null
                        symbol = ""; formalName = ""
                        fetchData()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    unitToEdit = null
                    symbol = ""; formalName = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StockGroupsTab(company: Company) {
    var groups by remember { mutableStateOf<List<StockGroup>>(emptyList()) }
    var items by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var units by remember { mutableStateOf<List<UnitOfMeasure>>(emptyList()) }
    var selectedGroupForItems by remember { mutableStateOf<StockGroup?>(null) }
    
    // Selection and Navigation
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    
    var showDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<StockGroup?>(null) }
    var groupToEdit by remember { mutableStateOf<StockGroup?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // Form States
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            groups = supabase.from("stock_groups").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockGroup>()
            
            items = supabase.from("stock_items").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockItem>()
            
            units = supabase.from("units").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<UnitOfMeasure>()
        }
    }

    LaunchedEffect(Unit) { 
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = selectedGroupForItems != null) {
        selectedGroupForItems = null
    }

    if (selectedGroupForItems != null) {
        FilteredStockItemsList(
            company = company,
            title = "Items in ${selectedGroupForItems!!.group_name}",
            items = items.filter { it.group_id == selectedGroupForItems!!.id },
            units = units,
            onBack = { selectedGroupForItems = null }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (selectedIndex < groups.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (groups.isNotEmpty()) selectedGroupForItems = groups[selectedIndex]
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 70.dp else 100.dp
            val rateWidth = if (isMobile) 70.dp else 100.dp
            val valueWidth = if (isMobile) 90.dp else 120.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                val constraints = this@BoxWithConstraints
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                            Text("Group Name", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Quantity", modifier = Modifier.width(qtyWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Avg Rate", modifier = Modifier.width(rateWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Value", modifier = Modifier.width(valueWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(groups) { index, group ->
                                val groupItems = items.filter { it.group_id == group.id }
                                val totalValue = groupItems.sumOf { it.current_quantity * it.opening_rate }
                                
                                // Check if all items have the same unit
                                val uniqueUnitIds = groupItems.map { it.unit_id }.distinct()
                                val hasSameUnit = uniqueUnitIds.size == 1
                                val unitSymbol = if (hasSameUnit) units.find { it.id == uniqueUnitIds[0] }?.unit_symbol ?: "" else ""
                                val totalQty = groupItems.sumOf { it.current_quantity }
                                val avgRate = if (totalQty > 0) totalValue / totalQty else 0.0

                                Surface(
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clickable { 
                                            if (selectedIndex == index) {
                                                selectedGroupForItems = group
                                            } else {
                                                selectedIndex = index
                                            }
                                        }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(group.group_name, modifier = Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                        
                                        if (hasSameUnit && groupItems.isNotEmpty()) {
                                            Text("${totalQty.format()} $unitSymbol", modifier = Modifier.width(qtyWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                            Text(avgRate.format(), modifier = Modifier.width(rateWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            Spacer(modifier = Modifier.width(qtyWidth + rateWidth)) 
                                        }
                                        
                                        Text(totalValue.format(), modifier = Modifier.width(valueWidth), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        
                                        IconButton(
                                            onClick = { 
                                                groupToEdit = group
                                                name = group.group_name
                                                selectedParentId = group.parent_group_id
                                                showDialog = true 
                                            }, 
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                        
                                        IconButton(onClick = { groupToDelete = group }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Group")
                }
            }
        }
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Stock Group") },
            text = { Text("Are you sure you want to delete group '${groupToDelete?.group_name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            groupToDelete?.id?.let { id ->
                                // Check for usage in stock items
                                val itemUsages = supabase.from("stock_items").select {
                                    filter { eq("group_id", id) }
                                    limit(1)
                                }.decodeList<StockItem>()

                                // Check for usage in sub-groups
                                val groupUsages = supabase.from("stock_groups").select {
                                    filter { eq("parent_group_id", id) }
                                    limit(1)
                                }.decodeList<StockGroup>()

                                if (itemUsages.isNotEmpty() || groupUsages.isNotEmpty()) {
                                    errorMsg = "Cannot delete group '${groupToDelete?.group_name}' because it is being used by stock items or has sub-groups."
                                } else {
                                    supabase.from("stock_groups").delete {
                                        filter { eq("id", id) }
                                    }
                                    fetchData()
                                }
                            }
                            groupToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Cannot Delete") },
            text = { Text(errorMsg!!) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) { Text("OK") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                groupToEdit = null
                name = ""; selectedParentId = null
            },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(if (groupToEdit != null) "Edit Stock Group" else "Stock Group Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InventoryField("Name", name) { name = it }
                    
                    InventoryDropdown("Under", groups.filter { it.id != groupToEdit?.id }.map { it.group_name }.plus("Primary"), 
                        groups.find { it.id == selectedParentId }?.group_name ?: "Primary") {
                        selectedParentId = groups.find { g -> g.group_name == it }?.id
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val updatedGroup = StockGroup(
                            id = groupToEdit?.id,
                            company_id = company.id!!,
                            group_name = name,
                            parent_group_id = selectedParentId
                        )
                        if (groupToEdit != null) {
                            supabase.from("stock_groups").update(updatedGroup) {
                                filter { eq("id", groupToEdit!!.id!!) }
                            }
                        } else {
                            supabase.from("stock_groups").insert(updatedGroup)
                        }
                        showDialog = false
                        groupToEdit = null
                        name = ""
                        fetchData()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    groupToEdit = null
                    name = ""; selectedParentId = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StockItemsTab(company: Company) {
    var items by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var units by remember { mutableStateOf<List<UnitOfMeasure>>(emptyList()) }
    var groups by remember { mutableStateOf<List<StockGroup>>(emptyList()) }
    
    var showDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<StockItem?>(null) }
    var itemToEdit by remember { mutableStateOf<StockItem?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // Selection and View Mode
    var selectedIndex by remember { mutableStateOf(0) }
    var isSummaryMode by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Form States
    var name by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    
    // Statutory Details
    var gstApplicability by remember { mutableStateOf("Applicable") }
    var hsnNumber by remember { mutableStateOf("") }
    var hsnDescription by remember { mutableStateOf("") }
    var taxabilityType by remember { mutableStateOf("Taxable") }
    var gstRate by remember { mutableStateOf("0") }
    var typeOfSupply by remember { mutableStateOf("Goods") }
    
    // Opening Balance
    var qty by remember { mutableStateOf("0") }
    var rate by remember { mutableStateOf("0") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            items = supabase.from("stock_items").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockItem>()
            
            units = supabase.from("units").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<UnitOfMeasure>()
            
            groups = supabase.from("stock_groups").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockGroup>()
            
            if (units.isNotEmpty() && selectedUnitId.isEmpty()) selectedUnitId = units[0].id!!
        }
    }

    LaunchedEffect(Unit) { 
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = isSummaryMode) {
        isSummaryMode = false
    }

    if (isSummaryMode && items.isNotEmpty() && selectedIndex < items.size) {
        StockItemMonthlySummary(
            company = company,
            item = items[selectedIndex],
            unitSymbol = units.find { it.id == items[selectedIndex].unit_id }?.unit_symbol ?: "",
            onBack = { 
                isSummaryMode = false 
                scope.launch { focusRequester.requestFocus() }
            }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (selectedIndex < items.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (items.isNotEmpty()) isSummaryMode = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 60.dp else 80.dp
            val rateWidth = if (isMobile) 60.dp else 80.dp
            val valueWidth = if (isMobile) 80.dp else 100.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                val constraints = this@BoxWithConstraints
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Particulars",
                                modifier = Modifier.weight(1.5f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isMobile) {
                                Text(
                                    text = "HSN",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GST",
                                    modifier = Modifier.weight(0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Closing Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Row {
                                    Text("Qty", modifier = Modifier.width(qtyWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Rate", modifier = Modifier.width(rateWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Value", modifier = Modifier.width(valueWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                }
                            }
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(items) { index, item ->
                                val unitSymbol = units.find { it.id == item.unit_id }?.unit_symbol ?: ""
                                val value = item.current_quantity * item.opening_rate
                                
                                Surface(
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clickable { 
                                            if (selectedIndex == index) {
                                                isSummaryMode = true
                                            } else {
                                                selectedIndex = index
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.item_name,
                                            modifier = Modifier.weight(1.5f).padding(start = 4.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (!isMobile) {
                                            Text(
                                                text = "${item.hsn_sac_number ?: ""}",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = if (item.gst_rate % 1.0 == 0.0) "${item.gst_rate.toInt()}%" else "${item.gst_rate.format(2)}%",
                                                modifier = Modifier.weight(0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = "${item.current_quantity.format()} $unitSymbol",
                                            modifier = Modifier.width(qtyWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = item.opening_rate.format(),
                                            modifier = Modifier.width(rateWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = value.format(),
                                            modifier = Modifier.width(valueWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        IconButton(
                                            onClick = { 
                                                itemToEdit = item
                                                name = item.item_name
                                                alias = item.alias ?: ""
                                                selectedUnitId = item.unit_id
                                                selectedGroupId = item.group_id
                                                gstApplicability = item.gst_applicability
                                                hsnNumber = item.hsn_sac_number ?: ""
                                                hsnDescription = item.hsn_description ?: ""
                                                taxabilityType = item.taxability_type
                                                gstRate = item.gst_rate.toString()
                                                typeOfSupply = item.type_of_supply
                                                qty = item.opening_quantity.toString()
                                                rate = item.opening_rate.toString()
                                                showDialog = true 
                                            }, 
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit, 
                                                contentDescription = "Edit Item", 
                                                tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(onClick = { itemToDelete = item }, modifier = Modifier.size(40.dp)) {
                                            Icon(
                                                Icons.Default.Delete, 
                                                contentDescription = "Delete Item", 
                                                tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Stock Item") },
            text = { Text("Are you sure you want to delete item '${itemToDelete?.item_name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            itemToDelete?.id?.let { id ->
                                // Check for usage in vouchers
                                val usages = supabase.from("voucher_stock_items").select {
                                    filter { eq("stock_item_id", id) }
                                    limit(1)
                                }.decodeList<VoucherStockItem>()

                                if (usages.isNotEmpty()) {
                                    errorMsg = "Cannot delete item '${itemToDelete?.item_name}' because it has been used in one or more voucher entries."
                                } else {
                                    supabase.from("stock_items").delete {
                                        filter { eq("id", id) }
                                    }
                                    fetchData()
                                }
                            }
                            itemToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Cannot Delete") },
            text = { Text(errorMsg!!) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) { Text("OK") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                itemToEdit = null
                name = ""; alias = ""; qty = "0"; rate = "0"
            },
            modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(if (itemToEdit != null) "Edit Stock Item" else "Stock Item Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (saveError != null) {
                        Text(
                            text = saveError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Header Section
                    Column {
                        InventoryField("Name", name) { name = it }
                        InventoryField("(alias)", alias) { alias = it }
                    }

                    HorizontalDivider()

                    // Section 1: General
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InventoryDropdown("Under", groups.map { it.group_name }.plus("Primary"), 
                            groups.find { it.id == selectedGroupId }?.group_name ?: "Primary") {
                            selectedGroupId = groups.find { g -> g.group_name == it }?.id
                        }
                        InventoryDropdown("Units", units.map { it.unit_symbol }, 
                            units.find { it.id == selectedUnitId }?.unit_symbol ?: "") {
                            selectedUnitId = units.find { u -> u.unit_symbol == it }?.id ?: ""
                        }
                    }

                    HorizontalDivider()

                    // Section 2: Statutory
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Statutory Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        InventoryDropdown("GST applicability", listOf("Applicable", "Not Applicable", "Undefined"), gstApplicability) { gstApplicability = it }
                        
                        if (gstApplicability == "Applicable") {
                            Text("HSN/SAC & Related Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            InventoryField("HSN/SAC", hsnNumber) { hsnNumber = it }
                            InventoryField("Description", hsnDescription) { hsnDescription = it }
                            
                            Spacer(Modifier.height(8.dp))
                            Text("GST Rate & Related Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            
                            InventoryDropdown("Taxability Type", listOf("Taxable", "Nil Rated", "Exempt"), taxabilityType) { taxabilityType = it }
                            if (taxabilityType == "Taxable") {
                                InventoryField("GST Rate (%)", gstRate) { gstRate = it }
                            }
                            InventoryDropdown("Type of Supply", listOf("Goods", "Services", "Capital Goods"), typeOfSupply) { typeOfSupply = it }
                        }
                    }

                    HorizontalDivider()

                    // Section 3: Opening Balance
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Opening Balance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InventoryField("Quantity", qty, modifier = Modifier.weight(1f)) { qty = it }
                            InventoryField("Rate", rate, modifier = Modifier.weight(1f)) { rate = it }
                            val totalValue = (qty.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)
                            InventoryField("Value", totalValue.format(), enabled = false, modifier = Modifier.weight(1f)) { }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            saveError = null
                            isSaving = true
                            try {
                                val item = StockItem(
                                    id = itemToEdit?.id,
                                    company_id = company.id!!,
                                    item_name = name,
                                    alias = alias,
                                    unit_id = selectedUnitId,
                                    group_id = selectedGroupId,
                                    gst_applicability = gstApplicability,
                                    hsn_sac_details = "Specify Details Here",
                                    hsn_sac_number = hsnNumber,
                                    hsn_description = hsnDescription,
                                    gst_rate_details = "Specify Details Here",
                                    taxability_type = taxabilityType,
                                    gst_rate = gstRate.toDoubleOrNull() ?: 0.0,
                                    type_of_supply = typeOfSupply,
                                    opening_quantity = qty.toDoubleOrNull() ?: 0.0,
                                    opening_rate = rate.toDoubleOrNull() ?: 0.0,
                                    current_quantity = if (itemToEdit != null) {
                                        // Simple logic: adjust current qty by difference in opening balance
                                        val oldOpening = itemToEdit!!.opening_quantity
                                        val newOpening = qty.toDoubleOrNull() ?: 0.0
                                        itemToEdit!!.current_quantity + (newOpening - oldOpening)
                                    } else {
                                        qty.toDoubleOrNull() ?: 0.0
                                    }
                                )
                                if (itemToEdit != null) {
                                    supabase.from("stock_items").update(item) {
                                        filter { eq("id", itemToEdit!!.id!!) }
                                    }
                                } else {
                                    supabase.from("stock_items").insert(item)
                                }
                                showDialog = false
                                itemToEdit = null
                                // Reset fields
                                name = ""; alias = ""; qty = "0"; rate = "0"
                                fetchData()
                            } catch (e: Exception) {
                                println("Error saving stock item: ${e.message}")
                                saveError = "Failed to save item. Please check your connection."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    itemToEdit = null
                    name = ""; alias = ""; qty = "0"; rate = "0"
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StockItemMonthlySummary(
    company: Company,
    item: StockItem,
    unitSymbol: String,
    onBack: () -> Unit
) {
    val months = listOf(
        "April", "May", "June", "July", "August", "September",
        "October", "November", "December", "January", "February", "March"
    )
    val monthSequence = listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3)

    // Initialize with empty months to prevent NullPointerException
    var monthlyDataMap by remember { 
        mutableStateOf(monthSequence.associateWith { m -> 
            MonthlyStockData(months[monthSequence.indexOf(m)]) 
        }) 
    }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                val entries = supabase.from("voucher_stock_items").select(Columns.raw("quantity, rate, amount, vouchers(date, voucher_type, company_id)")) {
                    filter { eq("stock_item_id", item.id!!) }
                }.decodeList<VoucherStockItemWithVoucher>()

                val dataMap = monthSequence.associateWith { m -> 
                    MonthlyStockData(months[monthSequence.indexOf(m)]) 
                }.toMutableMap()

                entries.forEach { entry ->
                    val dateParts = entry.vouchers.date.split("-")
                    if (dateParts.size == 3) {
                        val month = dateParts[1].toInt()
                        val monthData = dataMap[month]
                        if (monthData != null) {
                            if (entry.vouchers.voucher_type == "Purchase") {
                                monthData.inwardQty += entry.quantity
                                monthData.inwardValue += entry.amount
                            } else if (entry.vouchers.voucher_type == "Sale") {
                                monthData.outwardQty += entry.quantity
                                monthData.outwardValue += entry.amount
                            }
                        }
                    }
                }

                var currentQty = item.opening_quantity
                var currentValue = item.opening_quantity * item.opening_rate
                
                monthSequence.forEach { m ->
                    val monthData = dataMap[m]!!
                    monthData.closingQty = currentQty + monthData.inwardQty - monthData.outwardQty
                    monthData.closingValue = currentValue + monthData.inwardValue - monthData.outwardValue
                    currentQty = monthData.closingQty
                    currentValue = monthData.closingValue
                }

                monthlyDataMap = dataMap
            } catch (e: Exception) {
                println("Error fetching monthly summary: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(item.id) { fetchData() }
    
    val openingValue = item.opening_quantity * item.opening_rate

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(item.item_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Monthly Summary", style = MaterialTheme.typography.bodySmall)
                        // Note: Period dates for stock aren't currently passed in, 
                        // but we can use default period if needed or just show month range.
                        Text("Financial Year 2024-25", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                val isMobile = maxWidth < 600.dp
                val scrollState = rememberScrollState()

                Box(modifier = Modifier.fillMaxSize()) {
                    val constraints = this@BoxWithConstraints
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .horizontalScroll(scrollState)
                    ) {
                    val contentWidth = if (isMobile) 1000.dp else constraints.maxWidth
                        
                        Column(modifier = Modifier.width(contentWidth)) {
                            // Header
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Text("Particulars", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                SummaryColumnHeader("Inwards", Modifier.weight(2.5f))
                                SummaryColumnHeader("Outwards", Modifier.weight(2.5f))
                                SummaryColumnHeader("Closing Balance", Modifier.weight(2.5f))
                            }
                            HorizontalDivider(thickness = 2.dp, color = Color.Black)

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                // Opening Balance Row
                                item {
                                    SummaryRow(
                                        label = "Opening Balance",
                                        italic = true,
                                        closingQty = "${item.opening_quantity.format()} $unitSymbol",
                                        closingRate = item.opening_rate.format(),
                                        closingValue = openingValue.format()
                                    )
                                }

                                // Monthly Rows
                                items(monthSequence) { m ->
                                    val data = monthlyDataMap[m]!!
                                    val avgRateIn = if (data.inwardQty > 0) data.inwardValue / data.inwardQty else 0.0
                                    val avgRateOut = if (data.outwardQty > 0) data.outwardValue / data.outwardQty else 0.0
                                    val avgRateClosing = if (data.closingQty > 0) data.closingValue / data.closingQty else 0.0

                                    SummaryRow(
                                        label = data.monthName,
                                        inwardQty = if (data.inwardQty != 0.0) "${data.inwardQty.format()} $unitSymbol" else "",
                                        inwardRate = if (data.inwardQty != 0.0) avgRateIn.format() else "",
                                        inwardValue = if (data.inwardQty != 0.0) data.inwardValue.format() else "",
                                        outwardQty = if (data.outwardQty != 0.0) "${data.outwardQty.format()} $unitSymbol" else "",
                                        outwardRate = if (data.outwardQty != 0.0) avgRateOut.format() else "",
                                        outwardValue = if (data.outwardQty != 0.0) data.outwardValue.format() else "",
                                        closingQty = "${data.closingQty.format()} $unitSymbol",
                                        closingRate = avgRateClosing.format(),
                                        closingValue = data.closingValue.format()
                                    )
                                }
                            }

                            HorizontalDivider(thickness = 2.dp, color = Color.Black)
                            // Grand Total Row
                            val totalInQty = monthlyDataMap.values.sumOf { it.inwardQty }
                            val totalInVal = monthlyDataMap.values.sumOf { it.inwardValue }
                            val totalOutQty = monthlyDataMap.values.sumOf { it.outwardQty }
                            val totalOutVal = monthlyDataMap.values.sumOf { it.outwardValue }
                            val finalData = monthlyDataMap[3]!! // March is the last month

                            SummaryRow(
                                label = "Grand Total",
                                bold = true,
                                inwardQty = "${totalInQty.format()} $unitSymbol",
                                inwardRate = (if (totalInQty > 0) totalInVal / totalInQty else 0.0).format(),
                                inwardValue = totalInVal.format(),
                                outwardQty = "${totalOutQty.format()} $unitSymbol",
                                outwardRate = (if (totalOutQty > 0) totalOutVal / totalOutQty else 0.0).format(),
                                outwardValue = totalOutVal.format(),
                                closingQty = "${finalData.closingQty.format()} $unitSymbol",
                                closingRate = (if (finalData.closingQty > 0) finalData.closingValue / finalData.closingQty else 0.0).format(),
                                closingValue = finalData.closingValue.format()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryColumnHeader(label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Quantity", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
            Text("Rate", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
            Text("Value", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    inwardQty: String = "",
    inwardRate: String = "",
    inwardValue: String = "",
    outwardQty: String = "",
    outwardRate: String = "",
    outwardValue: String = "",
    closingQty: String = "",
    closingRate: String = "",
    closingValue: String = ""
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (italic) MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else MaterialTheme.typography.bodyMedium
        )
        
        Row(modifier = Modifier.weight(2.5f)) {
            Text(inwardQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(inwardRate, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(inwardValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.weight(2.5f)) {
            Text(outwardQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(outwardRate, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(outwardValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.weight(2.5f)) {
            Text(closingQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(closingRate, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(closingValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun FilteredStockItemsList(
    company: Company,
    title: String,
    items: List<StockItem>,
    units: List<UnitOfMeasure>,
    onBack: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var isSummaryMode by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = isSummaryMode) {
        isSummaryMode = false
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (isSummaryMode && items.isNotEmpty() && selectedIndex < items.size) {
        StockItemMonthlySummary(
            company = company,
            item = items[selectedIndex],
            unitSymbol = units.find { it.id == items[selectedIndex].unit_id }?.unit_symbol ?: "",
            onBack = { 
                isSummaryMode = false 
                scope.launch { focusRequester.requestFocus() }
            }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (selectedIndex < items.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (items.isNotEmpty()) isSummaryMode = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 60.dp else 80.dp
            val rateWidth = if (isMobile) 60.dp else 80.dp
            val valueWidth = if (isMobile) 80.dp else 100.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                val constraints = this@BoxWithConstraints
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Header with Back Button
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Particulars",
                                modifier = Modifier.weight(1.5f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Closing Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Row {
                                    Text("Qty", modifier = Modifier.width(qtyWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Rate", modifier = Modifier.width(rateWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                    Text("Value", modifier = Modifier.width(valueWidth), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                }
                            }
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(items) { index, item ->
                                val unitSymbol = units.find { it.id == item.unit_id }?.unit_symbol ?: ""
                                val value = item.current_quantity * item.opening_rate
                                
                                Surface(
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clickable { 
                                            if (selectedIndex == index) {
                                                isSummaryMode = true
                                            } else {
                                                selectedIndex = index
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.item_name,
                                            modifier = Modifier.weight(1.5f).padding(start = 4.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${item.current_quantity.format()} $unitSymbol",
                                            modifier = Modifier.width(qtyWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = item.opening_rate.format(),
                                            modifier = Modifier.width(rateWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = value.format(),
                                            modifier = Modifier.width(valueWidth),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(40.dp))
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
