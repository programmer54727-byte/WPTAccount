package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
                    supportingContent = { unit.formal_name?.let { Text(it) } }
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
    var name by remember { mutableStateOf("") }
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
                ListItem(headlineContent = { Text(group.group_name) })
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Stock Group") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") })
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        supabase.from("stock_groups").insert(StockGroup(company_id = company.id!!, group_name = name))
                        showDialog = false
                        name = ""
                        fetchGroups()
                    }
                }) { Text("Save") }
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
    var name by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
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
            
            if (units.isNotEmpty()) selectedUnitId = units[0].id!!
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.item_name) },
                    supportingContent = { Text("Stock: ${item.current_quantity} @ ${item.opening_rate}") }
                )
                HorizontalDivider()
            }
        }
        
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Item")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Stock Item") },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Select Unit", style = MaterialTheme.typography.labelSmall)
                    units.forEach { unit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedUnitId == unit.id, onClick = { selectedUnitId = unit.id!! })
                            Text(unit.unit_symbol)
                        }
                    }
                    
                    Text("Select Group (Optional)", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedGroupId == null, onClick = { selectedGroupId = null })
                        Text("No Group")
                    }
                    groups.forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedGroupId == group.id, onClick = { selectedGroupId = group.id })
                            Text(group.group_name)
                        }
                    }

                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Opening Quantity") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Opening Rate") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val item = StockItem(
                            company_id = company.id!!,
                            item_name = name,
                            unit_id = selectedUnitId,
                            group_id = selectedGroupId,
                            opening_quantity = qty.toDoubleOrNull() ?: 0.0,
                            opening_rate = rate.toDoubleOrNull() ?: 0.0,
                            current_quantity = qty.toDoubleOrNull() ?: 0.0
                        )
                        supabase.from("stock_items").insert(item)
                        showDialog = false
                        name = ""; qty = "0"; rate = "0"
                        fetchData()
                    }
                }) { Text("Save") }
            }
        )
    }
}
