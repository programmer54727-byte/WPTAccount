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

@Composable
fun InventoryField(label: String, value: String, modifier: Modifier = Modifier, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 4.dp)) {
        Text(text = "$label : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
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
fun InventoryDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
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

@Composable
fun TallyListAction(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(value) }
    val filteredOptions = options.filter { it.contains(searchText, ignoreCase = true) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { 
                    searchText = it
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFF9C4), // Tally-like yellow focus
                    unfocusedContainerColor = Color.Transparent
                )
            )

            DropdownMenu(
                expanded = expanded && filteredOptions.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Text(
                    text = "List of Actions",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3F51B5))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option, 
                                style = MaterialTheme.typography.bodySmall,
                                color = if (option == value) Color(0xFFE91E63) else Color.Unspecified
                            ) 
                        },
                        onClick = {
                            onSelect(option)
                            searchText = option
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
                title = { Text("Inventory: ${company.company_name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
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
    var showDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<UnitOfMeasure?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var symbol by remember { mutableStateOf("") }
    var formalName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun fetchUnits() {
        scope.launch {
            units = supabase.from("units").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<UnitOfMeasure>()
        }
    }

    LaunchedEffect(Unit) { fetchUnits() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(units) { unit ->
                ListItem(
                    headlineContent = { Text(unit.unit_symbol) },
                    supportingContent = { unit.formal_name?.let { Text(it) } },
                    trailingContent = {
                        IconButton(onClick = { unitToDelete = unit }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
        
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Unit")
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
                                    fetchUnits()
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
            title = { Text("Add Unit of Measure") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol (e.g. Pcs)") })
                    OutlinedTextField(value = formalName, onValueChange = { formalName = it }, label = { Text("Formal Name") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        supabase.from("units").insert(UnitOfMeasure(company_id = company.id!!, unit_symbol = symbol, formal_name = formalName))
                        showDialog = false
                        symbol = ""; formalName = ""
                        fetchUnits()
                    }
                }) { Text("Save") }
            }
        )
    }
}

@Composable
fun StockGroupsTab(company: Company) {
    var groups by remember { mutableStateOf<List<StockGroup>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<StockGroup?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // Form States
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    
    // Statutory Details
    var gstApplicability by remember { mutableStateOf("Applicable") }
    var hsnDetails by remember { mutableStateOf("As per Company/Stock Group") }
    var hsnNumber by remember { mutableStateOf("") }
    var hsnDescription by remember { mutableStateOf("") }
    var gstRateDetails by remember { mutableStateOf("As per Company/Stock Group") }
    var taxabilityType by remember { mutableStateOf("Taxable") }
    var gstRate by remember { mutableStateOf("0") }
    var typeOfSupply by remember { mutableStateOf("Goods") }

    val scope = rememberCoroutineScope()

    fun fetchGroups() {
        scope.launch {
            groups = supabase.from("stock_groups").select {
                filter { eq("company_id", company.id!!) }
            }.decodeList<StockGroup>()
        }
    }

    LaunchedEffect(Unit) { fetchGroups() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(groups) { group ->
                ListItem(
                    headlineContent = { Text(group.group_name) },
                    trailingContent = {
                        IconButton(onClick = { groupToDelete = group }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
        
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Group")
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
                                    fetchGroups()
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
            title = { Text("Stock Group Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InventoryField("Name", name) { name = it }
                    
                    InventoryDropdown("Under", groups.map { it.group_name }.plus("Primary"), 
                        groups.find { it.id == selectedParentId }?.group_name ?: "Primary") {
                        selectedParentId = groups.find { g -> g.group_name == it }?.id
                    }

                    HorizontalDivider()

                    Text("Statutory Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    InventoryDropdown("GST applicability", listOf("Applicable", "Not Applicable", "Undefined"), gstApplicability) { gstApplicability = it }
                    
                    if (gstApplicability == "Applicable") {
                        Text("HSN/SAC & Related Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        
                        TallyListAction(
                            label = "HSN/SAC Details", 
                            value = hsnDetails,
                            options = listOf("As per Company/Stock Group", "Specify Details Here", "Use GST Classification", "Specify in Voucher")
                        ) { hsnDetails = it }

                        if (hsnDetails == "Specify Details Here") {
                            InventoryField("HSN/SAC", hsnNumber) { hsnNumber = it }
                            InventoryField("Description", hsnDescription) { hsnDescription = it }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("GST Rate & Related Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        
                        TallyListAction(
                            label = "GST Rate Details", 
                            value = gstRateDetails,
                            options = listOf("As per Company/Stock Group", "Specify Details Here", "Use GST Classification", "Specify in Voucher")
                        ) { gstRateDetails = it }

                        if (gstRateDetails == "Specify Details Here") {
                            InventoryDropdown("Taxability Type", listOf("Taxable", "Nil Rated", "Exempt"), taxabilityType) { taxabilityType = it }
                            if (taxabilityType == "Taxable") {
                                InventoryField("GST Rate (%)", gstRate) { gstRate = it }
                            }
                        }
                        InventoryDropdown("Type of Supply", listOf("Goods", "Services", "Capital Goods"), typeOfSupply) { typeOfSupply = it }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val newGroup = StockGroup(
                            company_id = company.id!!,
                            group_name = name,
                            parent_group_id = selectedParentId,
                            gst_applicability = gstApplicability,
                            hsn_sac_details = hsnDetails,
                            hsn_sac_number = hsnNumber,
                            hsn_description = hsnDescription,
                            gst_rate_details = gstRateDetails,
                            taxability_type = taxabilityType,
                            gst_rate = gstRate.toDoubleOrNull() ?: 0.0,
                            type_of_supply = typeOfSupply
                        )
                        supabase.from("stock_groups").insert(newGroup)
                        showDialog = false
                        name = ""
                        fetchGroups()
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
    var hsnDetails by remember { mutableStateOf("As per Company/Stock Group") }
    var hsnNumber by remember { mutableStateOf("") }
    var hsnDescription by remember { mutableStateOf("") }
    var gstRateDetails by remember { mutableStateOf("As per Company/Stock Group") }
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
        Box(
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
            Column(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Particulars",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "HSN Code",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "GST Rate",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Closing Balance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row {
                            Text("Quantity", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text("Rate", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text("Value", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                        }
                    }
                    Spacer(Modifier.width(48.dp))
                }
                HorizontalDivider(thickness = 2.dp, color = Color.Black)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(items) { index, item ->
                        val unitSymbol = units.find { it.id == item.unit_id }?.unit_symbol ?: ""
                        val value = item.current_quantity * item.opening_rate
                        
                        Surface(
                            color = if (index == selectedIndex) Color(0xFF0D47A1) else Color(0xFFFFE082), // Deep blue for selection
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
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${item.hsn_sac_number ?: ""}",
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${item.gst_rate}",
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${item.current_quantity} $unitSymbol",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${item.opening_rate}",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${value}",
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                IconButton(onClick = { itemToDelete = item }) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Delete Item", 
                                        tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.error
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
            title = { Text("Stock Item Creation") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
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
                                
                                TallyListAction(
                                    label = "HSN/SAC Details", 
                                    value = hsnDetails,
                                    options = listOf("As per Company/Stock Group", "Specify Details Here", "Use GST Classification", "Specify in Voucher")
                                ) { hsnDetails = it }

                                if (hsnDetails == "Specify Details Here") {
                                    InventoryField("HSN/SAC", hsnNumber) { hsnNumber = it }
                                    InventoryField("Description", hsnDescription) { hsnDescription = it }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text("GST Rate & Related Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                
                                TallyListAction(
                                    label = "GST Rate Details", 
                                    value = gstRateDetails,
                                    options = listOf("As per Company/Stock Group", "Specify Details Here", "Use GST Classification", "Specify in Voucher")
                                ) { gstRateDetails = it }

                                if (gstRateDetails == "Specify Details Here") {
                                    InventoryDropdown("Taxability Type", listOf("Taxable", "Nil Rated", "Exempt"), taxabilityType) { taxabilityType = it }
                                    if (taxabilityType == "Taxable") {
                                        InventoryField("GST Rate (%)", gstRate) { gstRate = it }
                                    }
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
                            InventoryField("Value", totalValue.toString(), enabled = false, modifier = Modifier.weight(1f)) { }
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
                            hsn_sac_details = hsnDetails,
                            hsn_sac_number = hsnNumber,
                            hsn_description = hsnDescription,
                            gst_rate_details = gstRateDetails,
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
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
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
                Text("Particulars", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                SummaryColumnHeader("Inwards", Modifier.weight(2f))
                SummaryColumnHeader("Outwards", Modifier.weight(2f))
                SummaryColumnHeader("Closing Balance", Modifier.weight(2f))
            }
            HorizontalDivider(thickness = 2.dp, color = Color.Black)

            LazyColumn(modifier = Modifier.weight(1f)) {
                // Opening Balance Row
                item {
                    SummaryRow(
                        label = "Opening Balance",
                        italic = true,
                        closingQty = "${item.opening_quantity} $unitSymbol",
                        closingValue = openingValue.toString()
                    )
                }

                // Monthly Rows (Currently showing Opening Balance for April as a mock)
                items(months) { month ->
                    SummaryRow(
                        label = month,
                        inwardQty = if (month == "April") "${item.opening_quantity} $unitSymbol" else "",
                        inwardValue = if (month == "April") openingValue.toString() else "",
                        outwardQty = "",
                        outwardValue = "",
                        closingQty = if (month == "April") "${item.opening_quantity} $unitSymbol" else "${item.opening_quantity} $unitSymbol", // Rolling balance
                        closingValue = openingValue.toString()
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp, color = Color.Black)
            // Grand Total Row
            SummaryRow(
                label = "Grand Total",
                bold = true,
                inwardQty = "${item.opening_quantity} $unitSymbol",
                inwardValue = openingValue.toString(),
                outwardQty = "0 $unitSymbol",
                outwardValue = "0.00",
                closingQty = "${item.opening_quantity} $unitSymbol",
                closingValue = openingValue.toString()
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
    inwardValue: String = "",
    outwardQty: String = "",
    outwardValue: String = "",
    closingQty: String = "",
    closingValue: String = ""
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1.5f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (italic) MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else MaterialTheme.typography.bodyMedium
        )
        
        Row(modifier = Modifier.weight(2f)) {
            Text(inwardQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(inwardValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.weight(2f)) {
            Text(outwardQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(outwardValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.weight(2f)) {
            Text(closingQty, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Text(closingValue, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
