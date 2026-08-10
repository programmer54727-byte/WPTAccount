package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.DialogProperties

@Composable
fun InventoryField(label: String, value: String, modifier: Modifier = Modifier, enabled: Boolean = true, onValueChange: (String) -> Unit) {
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
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Units", "Groups", "Items")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory: ${company.company_name}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

@Composable
fun UnitsTab(company: Company) {
    var units by remember { mutableStateOf<List<UnitOfMeasure>>(emptyList()) }
    var items by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var selectedUnitForItems by remember { mutableStateOf<UnitOfMeasure?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<UnitOfMeasure?>(null) }
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

    LaunchedEffect(Unit) { fetchData() }

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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 70.dp else 100.dp
            val rateWidth = if (isMobile) 70.dp else 100.dp
            val valueWidth = if (isMobile) 90.dp else 120.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                        items(units) { unit ->
                            val unitItems = items.filter { it.unit_id == unit.id }
                            val totalQty = unitItems.sumOf { it.current_quantity }
                            val totalValue = unitItems.sumOf { it.current_quantity * it.opening_rate }
                            val avgRate = if (totalQty > 0) totalValue / totalQty else 0.0

                            Surface(
                                color = Color(0xFFFFE082),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clickable { selectedUnitForItems = unit }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(unit.unit_symbol, modifier = Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                    if (!isMobile) Text(unit.formal_name ?: "", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(totalQty.format(), modifier = Modifier.width(qtyWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                    Text(avgRate.format(), modifier = Modifier.width(rateWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                    Text(totalValue.format(), modifier = Modifier.width(valueWidth), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    
                                    IconButton(onClick = { unitToDelete = unit }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
            onDismissRequest = { showDialog = false },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Add Unit of Measure") },
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
                        supabase.from("units").insert(UnitOfMeasure(company_id = company.id!!, unit_symbol = symbol, formal_name = formalName))
                        showDialog = false
                        symbol = ""; formalName = ""
                        fetchData()
                    }
                }) { Text("Save") }
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
    var showDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<StockGroup?>(null) }
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

    LaunchedEffect(Unit) { fetchData() }

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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isMobile = maxWidth < 600.dp
            val qtyWidth = if (isMobile) 70.dp else 100.dp
            val rateWidth = if (isMobile) 70.dp else 100.dp
            val valueWidth = if (isMobile) 90.dp else 120.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                        items(groups) { group ->
                            val groupItems = items.filter { it.group_id == group.id }
                            val totalValue = groupItems.sumOf { it.current_quantity * it.opening_rate }
                            
                            // Check if all items have the same unit
                            val uniqueUnitIds = groupItems.map { it.unit_id }.distinct()
                            val hasSameUnit = uniqueUnitIds.size == 1
                            val unitSymbol = if (hasSameUnit) units.find { it.id == uniqueUnitIds[0] }?.unit_symbol ?: "" else ""
                            val totalQty = groupItems.sumOf { it.current_quantity }
                            val avgRate = if (totalQty > 0) totalValue / totalQty else 0.0

                            Surface(
                                color = Color(0xFFFFE082),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clickable { selectedGroupForItems = group }
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
                                    
                                    IconButton(onClick = { groupToDelete = group }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
            onDismissRequest = { showDialog = false },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Stock Group Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InventoryField("Name", name) { name = it }
                    
                    InventoryDropdown("Under", groups.map { it.group_name }.plus("Primary"), 
                        groups.find { it.id == selectedParentId }?.group_name ?: "Primary") {
                        selectedParentId = groups.find { g -> g.group_name == it }?.id
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val newGroup = StockGroup(
                            company_id = company.id!!,
                            group_name = name,
                            parent_group_id = selectedParentId
                        )
                        supabase.from("stock_groups").insert(newGroup)
                        showDialog = false
                        name = ""
                        fetchData()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
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

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                            color = if (index == selectedIndex) Color(0xFF0D47A1) else Color(0xFFFFE082),
                            contentColor = if (index == selectedIndex) Color.White else Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clickable { 
                                    selectedIndex = index
                                    isSummaryMode = true 
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
                                        text = item.gst_rate.format(),
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
            
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Item")
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
                                supabase.from("stock_items").delete {
                                    filter { eq("id", id) }
                                }
                                fetchData()
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Stock Item Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Section
                    Column {
                        InventoryField("Name", name) { name = it }
                        InventoryField("(alias)", alias) { alias = it }
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left Column: General
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InventoryDropdown("Under", groups.map { it.group_name }.plus("Primary"), 
                                groups.find { it.id == selectedGroupId }?.group_name ?: "Primary") {
                                selectedGroupId = groups.find { g -> g.group_name == it }?.id
                            }
                            InventoryDropdown("Units", units.map { it.unit_symbol }, 
                                units.find { it.id == selectedUnitId }?.unit_symbol ?: "") {
                                selectedUnitId = units.find { u -> u.unit_symbol == it }?.id ?: ""
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        // Right Column: Statutory
                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }

                    HorizontalDivider()

                    // Footer Section: Opening Balance
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
                Button(onClick = {
                    scope.launch {
                        val item = StockItem(
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
                            current_quantity = qty.toDoubleOrNull() ?: 0.0
                        )
                        supabase.from("stock_items").insert(item)
                        showDialog = false
                        // Reset fields
                        name = ""; alias = ""; qty = "0"; rate = "0"
                        fetchData()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
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
                        Text("For 1-Apr-2024 to 31-Mar-2025", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Particulars", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
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

                // Monthly Rows (Showing movement during the year)
                items(months) { month ->
                    SummaryRow(
                        label = month,
                        inwardQty = "",
                        inwardRate = "",
                        inwardValue = "",
                        outwardQty = "",
                        outwardRate = "",
                        outwardValue = "",
                        closingQty = "${item.opening_quantity.format()} $unitSymbol", // Showing current balance
                        closingRate = item.opening_rate.format(),
                        closingValue = openingValue.format()
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp, color = Color.Black)
            // Grand Total Row (Total movements during the year)
            SummaryRow(
                label = "Grand Total",
                bold = true,
                inwardQty = "0 $unitSymbol",
                inwardRate = "0.00",
                inwardValue = "0.00",
                outwardQty = "0 $unitSymbol",
                outwardRate = "0.00",
                outwardValue = "0.00",
                closingQty = "${item.opening_quantity.format()} $unitSymbol",
                closingRate = item.opening_rate.format(),
                closingValue = openingValue.format()
            )
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

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                            color = if (index == selectedIndex) Color(0xFF0D47A1) else Color(0xFFFFE082),
                            contentColor = if (index == selectedIndex) Color.White else Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clickable { 
                                    selectedIndex = index
                                    isSummaryMode = true 
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
