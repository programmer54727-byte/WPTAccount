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
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.DialogProperties

data class MonthlyLedgerData(
    val monthName: String,
    var debit: Double = 0.0,
    var credit: Double = 0.0,
    var balance: Double = 0.0
)

data class LedgerBalance(
    val opening: Double = 0.0,
    val closing: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManagement(
    company: Company,
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
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
    var selectedTab by remember { mutableStateOf(0) }
    var showPeriodDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Groups", "Ledgers")
    
    AppNavigationDrawer(
        currentScreen = ScreenType.Ledger,
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
                ScreenType.Ledger -> { /* Already here */ }
                ScreenType.Contra -> onContraClick()
                ScreenType.Journal -> onJournalClick()
                ScreenType.CreditNote -> { /* TODO */ }
                ScreenType.DebitNote -> { /* TODO */ }
                ScreenType.BalanceSheet -> { /* TODO */ }
                ScreenType.ProfitAndLoss -> { /* TODO */ }
                ScreenType.CashFlow -> { /* TODO */ }
                ScreenType.Stock -> onStockSummaryClick()
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
                            Text("Ledger: ${company.company_name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Period: ${currentPeriod.startDate.toDisplayDate()} to ${currentPeriod.endDate.toDisplayDate()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { showPeriodDialog = true }
                            )
                        }
                    },
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
                        IconButton(onClick = { showPeriodDialog = true }) {
                            Icon(Icons.Default.Event, contentDescription = "Change Period")
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
                    0 -> LedgerGroupsTab(company, currentPeriod)
                    1 -> LedgersTab(company, currentPeriod)
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
                        label = { Text("Start Date (DD/MM/YYYY)") }
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Date (DD/MM/YYYY)") }
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
fun LedgerGroupsTab(company: Company, period: AccountPeriod) {
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var selectedGroupForLedgers by remember { mutableStateOf<AccountingGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<AccountingGroup?>(null) }
    var groupToEdit by remember { mutableStateOf<AccountingGroup?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // Form States
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var nature by remember { mutableStateOf<String?>(null) }
    
    // Selection and Navigation
    var selectedIndex by remember { mutableStateOf(0) }
    var balances by remember { mutableStateOf<Map<String, LedgerBalance>>(emptyMap()) }
    val focusRequester = remember { FocusRequester() }
    
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                groups = supabase.from("groups").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<AccountingGroup>()
            
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()

                val allEntries = supabase.from("voucher_entries").select(Columns.raw("ledger_id, amount, entry_type, vouchers(date, company_id)")) {
                    filter { eq("vouchers.company_id", company.id!!) }
                }.decodeList<VoucherEntryWithVoucher>()

                val calcBalances = ledgers.associate { ledger ->
                    var opening = ledger.opening_balance
                    if (ledger.opening_balance_type == "Cr") opening = -opening
                
                    var periodTotal = 0.0
                    allEntries.filter { it.ledger_id == ledger.id }.forEach { entry ->
                        val sign = if (entry.entry_type == "Debit") 1.0 else -1.0
                        if (entry.vouchers.date < period.startDate) {
                            opening += entry.amount * sign
                        } else if (entry.vouchers.date <= period.endDate) {
                            periodTotal += entry.amount * sign
                        }
                    }
                    ledger.id!! to LedgerBalance(opening, opening + periodTotal)
                }
                balances = calcBalances
            } catch (e: Exception) {
                println("Error fetching groups/ledgers: ${e.message}")
            }
        }
    }

    LaunchedEffect(period) { fetchData() }
    
    LaunchedEffect(Unit) { 
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = selectedGroupForLedgers != null) {
        selectedGroupForLedgers = null
    }

    if (selectedGroupForLedgers != null) {
                        FilteredLedgersList(
                            company = company,
                            title = "Ledgers in ${selectedGroupForLedgers!!.group_name}",
                            ledgers = ledgers.filter { it.group_id == selectedGroupForLedgers!!.id },
                            period = period,
                            onBack = { selectedGroupForLedgers = null }
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
                                if (groups.isNotEmpty()) selectedGroupForLedgers = groups[selectedIndex]
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val balanceWidth = if (isMobile) 100.dp else 150.dp
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
                            Text("Nature", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Current Balance", modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(80.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(groups) { index, group ->
                                val groupLedgers = ledgers.filter { it.group_id == group.id }
                                val totalBalance = groupLedgers.sumOf { ledger -> 
                                    balances[ledger.id]?.closing ?: ledger.current_balance 
                                }

                                Surface(
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clickable { 
                                            if (selectedIndex == index) {
                                                selectedGroupForLedgers = group
                                            } else {
                                                selectedIndex = index
                                            }
                                        }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(group.group_name, modifier = Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                        Text(group.nature ?: "", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            totalBalance.formatWithSign(), 
                                            modifier = Modifier.width(balanceWidth), 
                                            textAlign = TextAlign.End, 
                                            fontWeight = FontWeight.Bold, 
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        
                                        IconButton(
                                            onClick = { 
                                                groupToEdit = group
                                                name = group.group_name
                                                selectedParentId = group.parent_group_id
                                                nature = group.nature
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
            title = { Text("Delete Accounting Group") },
            text = { Text("Are you sure you want to delete group '${groupToDelete?.group_name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            groupToDelete?.id?.let { id ->
                                // Check for usage in ledgers
                                val ledgerUsages = supabase.from("ledgers").select {
                                    filter { eq("group_id", id) }
                                    limit(1)
                                }.decodeList<Ledger>()

                                // Check for usage in sub-groups
                                val groupUsages = supabase.from("groups").select {
                                    filter { eq("parent_group_id", id) }
                                    limit(1)
                                }.decodeList<AccountingGroup>()

                                if (ledgerUsages.isNotEmpty() || groupUsages.isNotEmpty()) {
                                    errorMsg = "Cannot delete group '${groupToDelete?.group_name}' because it contains ledgers or sub-groups."
                                } else {
                                    supabase.from("groups").delete {
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
                name = ""; selectedParentId = null; nature = null
            },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(if (groupToEdit != null) "Edit Accounting Group" else "Add Accounting Group") },
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

                    InventoryDropdown("Nature", listOf("Asset", "Liability", "Income", "Expense"), nature ?: "") {
                        nature = it
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val group = AccountingGroup(
                            id = groupToEdit?.id,
                            company_id = company.id!!,
                            group_name = name,
                            parent_group_id = selectedParentId,
                            nature = nature
                        )
                        if (groupToEdit != null) {
                            supabase.from("groups").update(group) {
                                filter { eq("id", groupToEdit!!.id!!) }
                            }
                        } else {
                            supabase.from("groups").insert(group)
                        }
                        showDialog = false
                        groupToEdit = null
                        name = ""; selectedParentId = null; nature = null
                        fetchData()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    groupToEdit = null
                    name = ""; selectedParentId = null; nature = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LedgersTab(company: Company, period: AccountPeriod) {
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var ledgerToDelete by remember { mutableStateOf<Ledger?>(null) }
    var ledgerToEdit by remember { mutableStateOf<Ledger?>(null) }
    
    // Selection and View Mode
    var selectedIndex by remember { mutableStateOf(0) }
    var isSummaryMode by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Form States
    var name by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var openingBalance by remember { mutableStateOf("0") }
    var openingBalanceType by remember { mutableStateOf("Dr") }

    // Mailing Details
    var mailingName by remember { mutableStateOf("") }
    var isMailingNameSynced by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    // Tax Registration
    var panItNumber by remember { mutableStateOf("") }
    var gstRegistrationType by remember { mutableStateOf("Unregistered") }
    var gstinUin by remember { mutableStateOf("") }

    // Bank Details
    var bankAccNo by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankBranch by remember { mutableStateOf("") }
    var bankSwift by remember { mutableStateOf("") }

    // Party Details
    var billByBill by remember { mutableStateOf(false) }
    var creditPeriod by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }

    // Tax Details
    var dutyTaxType by remember { mutableStateOf("GST") }
    var gstTaxSubType by remember { mutableStateOf("Integrated Tax") }
    var taxRate by remember { mutableStateOf("0") }

    // Revenue/Expense Details
    var inventoryAffected by remember { mutableStateOf(false) }
    var costCentresApplicable by remember { mutableStateOf(false) }
    var gstApplicableType by remember { mutableStateOf("Applicable") }
    var supplyType by remember { mutableStateOf("Services") }
    var hsnSacCode by remember { mutableStateOf("") }
    var hsnSacDesc by remember { mutableStateOf("") }

    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var balances by remember { mutableStateOf<Map<String, LedgerBalance>>(emptyMap()) }
    
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()
            
                groups = supabase.from("groups").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<AccountingGroup>()
            
                val allEntries = supabase.from("voucher_entries").select(Columns.raw("ledger_id, amount, entry_type, vouchers(date, company_id)")) {
                    filter { eq("vouchers.company_id", company.id!!) }
                }.decodeList<VoucherEntryWithVoucher>()

                val calcBalances = ledgers.associate { ledger ->
                    var opening = ledger.opening_balance
                    if (ledger.opening_balance_type == "Cr") opening = -opening
                
                    var periodDebit = 0.0
                    var periodCredit = 0.0
                
                    allEntries.filter { it.ledger_id == ledger.id }.forEach { entry ->
                        if (entry.vouchers.date < period.startDate) {
                            if (entry.entry_type == "Debit") opening += entry.amount
                            else opening -= entry.amount
                        } else if (entry.vouchers.date <= period.endDate) {
                            if (entry.entry_type == "Debit") periodDebit += entry.amount
                            else periodCredit += entry.amount
                        }
                    }
                
                    ledger.id!! to LedgerBalance(opening, opening + periodDebit - periodCredit)
                }
                balances = calcBalances

                if (groups.isNotEmpty() && selectedGroupId == null) selectedGroupId = groups[0].id
            } catch (e: Exception) {
                println("Error fetching ledgers: ${e.message}")
            }
        }
    }

    LaunchedEffect(period) { fetchData() }
    
    LaunchedEffect(Unit) { 
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = isSummaryMode) {
        isSummaryMode = false
    }

    if (isSummaryMode && ledgers.isNotEmpty() && selectedIndex < ledgers.size) {
        LedgerMonthlySummary(
            company = company,
            ledger = ledgers[selectedIndex],
            period = period,
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
                                if (selectedIndex < ledgers.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (ledgers.isNotEmpty()) isSummaryMode = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val balanceWidth = if (isMobile) 80.dp else 120.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val constraints = this@BoxWithConstraints
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("Particulars", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            if (!isMobile) Text("Group", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Opening", modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Closing", modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(ledgers) { index, ledger ->
                                val groupName = groups.find { it.id == ledger.group_id }?.group_name ?: ""
                                
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
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val ledgerBalance = balances[ledger.id] ?: LedgerBalance(ledger.opening_balance, ledger.current_balance)
                                        
                                        Text(ledger.ledger_name, modifier = Modifier.weight(1.5f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                        if (!isMobile) Text(groupName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Text(ledgerBalance.opening.formatWithSign(), modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                        Text(ledgerBalance.closing.formatWithSign(), modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        
                                        IconButton(
                                            onClick = { 
                                                ledgerToEdit = ledger
                                                name = ledger.ledger_name
                                                alias = ledger.alias ?: ""
                                                selectedGroupId = ledger.group_id
                                                openingBalance = ledger.opening_balance.toString()
                                                openingBalanceType = ledger.opening_balance_type
                                                mailingName = ledger.mailing_name ?: ""
                                                address = ledger.address ?: ""
                                                state = ledger.state ?: ""
                                                country = ledger.country ?: ""
                                                pincode = ledger.pincode ?: ""
                                                panItNumber = ledger.pan_it_number ?: ""
                                                gstRegistrationType = ledger.gst_registration_type ?: "Unregistered"
                                                gstinUin = ledger.gstin_uin ?: ""
                                                bankAccNo = ledger.bank_acc_no ?: ""
                                                bankIfsc = ledger.bank_ifsc ?: ""
                                                bankName = ledger.bank_name ?: ""
                                                bankBranch = ledger.bank_branch ?: ""
                                                bankSwift = ledger.bank_swift ?: ""
                                                billByBill = ledger.bill_by_bill
                                                creditPeriod = ledger.credit_period?.toString() ?: ""
                                                creditLimit = ledger.credit_limit?.toString() ?: ""
                                                dutyTaxType = ledger.duty_tax_type ?: "GST"
                                                gstTaxSubType = ledger.gst_tax_sub_type ?: "Integrated Tax"
                                                taxRate = ledger.tax_rate?.toString() ?: "0"
                                                inventoryAffected = ledger.inventory_affected
                                                costCentresApplicable = ledger.cost_centres_applicable
                                                gstApplicableType = ledger.gst_applicable_type ?: "Applicable"
                                                supplyType = ledger.supply_type ?: "Services"
                                                hsnSacCode = ledger.hsn_sac_code ?: ""
                                                hsnSacDesc = ledger.hsn_sac_desc ?: ""
                                                showDialog = true 
                                            }, 
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Ledger", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }

                                        IconButton(onClick = { ledgerToDelete = ledger }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Ledger", tint = if (index == selectedIndex) Color.White else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
                    Icon(Icons.Default.Add, "Add Ledger")
                }
            }
        }
    }

    if (ledgerToDelete != null) {
        AlertDialog(
            onDismissRequest = { ledgerToDelete = null },
            title = { Text("Delete Ledger") },
            text = { Text("Are you sure you want to delete ledger '${ledgerToDelete?.ledger_name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            ledgerToDelete?.id?.let { id ->
                                // Check for usage in voucher entries
                                val usages = supabase.from("voucher_entries").select {
                                    filter { eq("ledger_id", id) }
                                    limit(1)
                                }.decodeList<VoucherEntry>()

                                if (usages.isNotEmpty()) {
                                    saveError = "Cannot delete ledger '${ledgerToDelete?.ledger_name}' because it has existing voucher entries."
                                } else {
                                    supabase.from("ledgers").delete {
                                        filter { eq("id", id) }
                                    }
                                    fetchData()
                                }
                            }
                            ledgerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { ledgerToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (saveError != null && !showDialog) {
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("Attention") },
            text = { Text(saveError!!) },
            confirmButton = {
                Button(onClick = { saveError = null }) { Text("OK") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                ledgerToEdit = null
                isMailingNameSynced = true
                name = ""; alias = ""; mailingName = ""; address = ""; state = ""; country = ""; pincode = ""
                panItNumber = ""; gstinUin = ""; openingBalance = "0"; openingBalanceType = "Dr"
                bankAccNo = ""; bankIfsc = ""; bankName = ""; bankBranch = ""; bankSwift = ""
                billByBill = false; creditPeriod = ""; creditLimit = ""
                taxRate = "0"
                inventoryAffected = false; costCentresApplicable = false
                gstApplicableType = "Applicable"; hsnSacCode = ""; hsnSacDesc = ""
            },
            modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth(0.95f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(if (ledgerToEdit != null) "Edit Ledger" else "Ledger Creation") },
            text = {
                val scrollState = rememberScrollState()
                val selectedGroup = groups.find { it.id == selectedGroupId }
                val groupName = selectedGroup?.group_name ?: ""
                
                // Group Categorization
                val isBankRelated = groupName.contains("Bank", ignoreCase = true)
                val isPartyRelated = groupName.contains("Sundry", ignoreCase = true) || 
                                    groupName.contains("Branch", ignoreCase = true) || 
                                    groupName.contains("Current Liabilities", ignoreCase = true) || 
                                    groupName.contains("Loans & Advances", ignoreCase = true)
                val isLoanRelated = groupName.contains("Loans", ignoreCase = true) || 
                                   groupName.contains("Secured", ignoreCase = true) || 
                                   groupName.contains("Unsecured", ignoreCase = true)
                val isRevenueRelated = groupName.contains("Sales", ignoreCase = true) || 
                                      groupName.contains("Purchase", ignoreCase = true) || 
                                      groupName.contains("Income", ignoreCase = true) || 
                                      groupName.contains("Expense", ignoreCase = true)
                val isFixedAsset = groupName.contains("Fixed Assets", ignoreCase = true) || 
                                  groupName.contains("Investments", ignoreCase = true)
                val isCapital = groupName.contains("Capital", ignoreCase = true)
                
                val isInternalOnly = groupName.contains("Cash-in-Hand", ignoreCase = true) || 
                                    groupName.contains("Provisions", ignoreCase = true) || 
                                    groupName.contains("Reserves", ignoreCase = true) || 
                                    groupName.contains("Retained", ignoreCase = true) || 
                                    groupName.contains("Suspense", ignoreCase = true)

                // Auto-set inventory affected for Sales/Purchase
                LaunchedEffect(groupName) {
                    if (groupName.contains("Sales", ignoreCase = true) || groupName.contains("Purchase", ignoreCase = true)) {
                        inventoryAffected = true
                    }
                }

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

                    // Section 1: General
                    Column {
                        InventoryField("Name", name) { 
                            name = it
                            if (isMailingNameSynced) mailingName = it
                        }
                        InventoryField("(alias)", alias) { alias = it }
                        InventoryDropdown("Under", groups.map { it.group_name }, 
                            groups.find { it.id == selectedGroupId }?.group_name ?: "") {
                            selectedGroupId = groups.find { g -> g.group_name == it }?.id
                        }
                    }

                    HorizontalDivider()

                    // Section 2: Mailing Details (Hidden for Internal accounts)
                    if (!isInternalOnly) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mailing Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            InventoryField("Name", mailingName) { 
                                mailingName = it
                                isMailingNameSynced = false
                            }
                            InventoryField("Address", address) { address = it }
                            InventoryField("State", state) { state = it }
                            InventoryField("Country", country) { country = it }
                            InventoryField("Pincode", pincode) { pincode = it }
                        }

                        HorizontalDivider()

                        // Section 3: Tax Registration (Show for Parties, Loans, Capital)
                        if (isPartyRelated || isLoanRelated || isCapital || isFixedAsset) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Tax Registration Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                InventoryField("PAN/IT No.", panItNumber) { panItNumber = it }
                                InventoryDropdown("Registration Type", listOf("Regular", "Composition", "Consumer", "Unregistered"), gstRegistrationType) {
                                    gstRegistrationType = it
                                }
                                if (gstRegistrationType == "Regular" || gstRegistrationType == "Composition") {
                                    InventoryField("GSTIN/UIN", gstinUin) { gstinUin = it }
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    // Section 4: Bank Details (Visible for Bank, Capital, Parties, Loans)
                    if (isBankRelated || isCapital || isPartyRelated || isLoanRelated) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bank Account Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            InventoryField("A/c No.", bankAccNo) { bankAccNo = it }
                            InventoryField("IFSC Code", bankIfsc) { bankIfsc = it }
                            InventoryField("Bank Name", bankName) { bankName = it }
                            InventoryField("Branch", bankBranch) { bankBranch = it }
                            InventoryField("SWIFT Code", bankSwift) { bankSwift = it }
                        }
                        HorizontalDivider()
                    }

                    // Section 5: Credit Control (Visible for Parties, Branches, Loans, Assets/Liabilities)
                    if (isPartyRelated || isLoanRelated) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Credit Control Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Bill-by-bill tracking", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Switch(checked = billByBill, onCheckedChange = { billByBill = it })
                            }
                            InventoryField("Credit Period (Days)", creditPeriod) { creditPeriod = it }
                            InventoryField("Credit Limit", creditLimit) { creditLimit = it }
                        }
                        HorizontalDivider()
                    }

                    // Section 6: Tax Details (For Duties & Taxes only)
                    if (groupName.contains("Duties & Taxes", ignoreCase = true)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tax Calculation Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            InventoryDropdown("Type of Duty", listOf("GST", "TDS", "Others"), dutyTaxType) { dutyTaxType = it }
                            if (dutyTaxType == "GST") {
                                InventoryDropdown("Tax Type", listOf("Central Tax", "State Tax", "Integrated Tax", "Cess"), gstTaxSubType) {
                                    gstTaxSubType = it
                                }
                            }
                            InventoryField("Percentage (%)", taxRate) { taxRate = it }
                        }
                        HorizontalDivider()
                    }

                    // Section 7: Inventory & Costing (For Revenue and Assets)
                    if (isRevenueRelated || isFixedAsset) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Inventory & Costing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Inventory values are affected", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Switch(checked = inventoryAffected, onCheckedChange = { inventoryAffected = it })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cost Centres are applicable", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Switch(checked = costCentresApplicable, onCheckedChange = { costCentresApplicable = it })
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Statutory Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            InventoryDropdown("Is GST Applicable", listOf("Applicable", "Not Applicable", "Undefined"), gstApplicableType) {
                                gstApplicableType = it
                            }
                            
                            if (gstApplicableType == "Applicable") {
                                InventoryField("HSN/SAC Code", hsnSacCode) { hsnSacCode = it }
                                InventoryField("HSN/SAC Description", hsnSacDesc) { hsnSacDesc = it }
                                InventoryField("GST Rate (%)", taxRate) { taxRate = it }
                                InventoryDropdown("Type of Supply", if (isFixedAsset) listOf("Capital Goods") else listOf("Goods", "Services"), supplyType) {
                                    supplyType = it
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    // Section 8: Opening Balance
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InventoryField("Opening Balance", openingBalance, modifier = Modifier.weight(1f)) { openingBalance = it }
                            InventoryDropdown("", listOf("Dr", "Cr"), openingBalanceType, modifier = Modifier.width(80.dp)) { 
                                openingBalanceType = it 
                            }
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
                                val ledger = Ledger(
                                    id = ledgerToEdit?.id,
                                    company_id = company.id!!,
                                    ledger_name = name,
                                    alias = alias.ifEmpty { null },
                                    group_id = selectedGroupId!!,
                                    mailing_name = mailingName.ifEmpty { null },
                                    address = address.ifEmpty { null },
                                    state = state.ifEmpty { null },
                                    country = country.ifEmpty { null },
                                    pincode = pincode.ifEmpty { null },
                                    pan_it_number = panItNumber.ifEmpty { null },
                                    gst_registration_type = gstRegistrationType,
                                    gstin_uin = gstinUin.ifEmpty { null },
                                    
                                    // Bank
                                    bank_acc_no = bankAccNo.ifEmpty { null },
                                    bank_ifsc = bankIfsc.ifEmpty { null },
                                    bank_name = bankName.ifEmpty { null },
                                    bank_branch = bankBranch.ifEmpty { null },
                                    bank_swift = bankSwift.ifEmpty { null },
                                    
                                    // Party
                                    bill_by_bill = billByBill,
                                    credit_period = creditPeriod.toIntOrNull(),
                                    credit_limit = creditLimit.toDoubleOrNull(),
                                    
                                    // Tax
                                    duty_tax_type = dutyTaxType,
                                    gst_tax_sub_type = gstTaxSubType,
                                    tax_rate = taxRate.toDoubleOrNull(),

                                    // Revenue/Expense
                                    inventory_affected = inventoryAffected,
                                    cost_centres_applicable = costCentresApplicable,
                                    gst_applicable_type = gstApplicableType,
                                    supply_type = supplyType,
                                    hsn_sac_code = hsnSacCode.ifEmpty { null },
                                    hsn_sac_desc = hsnSacDesc.ifEmpty { null },

                                    opening_balance = openingBalance.toDoubleOrNull() ?: 0.0,
                                    opening_balance_type = openingBalanceType,
                                    current_balance = if (openingBalanceType == "Cr") -(openingBalance.toDoubleOrNull() ?: 0.0) else (openingBalance.toDoubleOrNull() ?: 0.0)
                                )
                                if (ledgerToEdit != null) {
                                    supabase.from("ledgers").update(ledger) {
                                        filter { eq("id", ledgerToEdit!!.id!!) }
                                    }
                                } else {
                                    supabase.from("ledgers").insert(ledger)
                                }
                                showDialog = false
                                ledgerToEdit = null
                                // Reset fields
                                name = ""; alias = ""; mailingName = ""; address = ""; state = ""; country = ""; pincode = ""
                                isMailingNameSynced = true
                                panItNumber = ""; gstinUin = ""; openingBalance = "0"; openingBalanceType = "Dr"
                                bankAccNo = ""; bankIfsc = ""; bankName = ""; bankBranch = ""; bankSwift = ""
                                billByBill = false; creditPeriod = ""; creditLimit = ""
                                taxRate = "0"
                                inventoryAffected = false; costCentresApplicable = false
                                gstApplicableType = "Applicable"; hsnSacCode = ""; hsnSacDesc = ""
                                fetchData()
                            } catch (e: Exception) {
                                println("Error saving ledger: ${e.message}")
                                saveError = "Failed to save ledger. Please check your connection."
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
                    ledgerToEdit = null
                    isMailingNameSynced = true
                    name = ""; alias = ""; mailingName = ""; address = ""; state = ""; country = ""; pincode = ""
                    panItNumber = ""; gstinUin = ""; openingBalance = "0"; openingBalanceType = "Dr"
                    bankAccNo = ""; bankIfsc = ""; bankName = ""; bankBranch = ""; bankSwift = ""
                    billByBill = false; creditPeriod = ""; creditLimit = ""
                    taxRate = "0"
                    inventoryAffected = false; costCentresApplicable = false
                    gstApplicableType = "Applicable"; hsnSacCode = ""; hsnSacDesc = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FilteredLedgersList(
    company: Company,
    title: String,
    ledgers: List<Ledger>,
    period: AccountPeriod,
    onBack: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var isSummaryMode by remember { mutableStateOf(false) }
    var balances by remember { mutableStateOf<Map<String, LedgerBalance>>(emptyMap()) }
    
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                val allEntries = supabase.from("voucher_entries").select(Columns.raw("ledger_id, amount, entry_type, vouchers(date, company_id)")) {
                    filter { eq("vouchers.company_id", company.id!!) }
                }.decodeList<VoucherEntryWithVoucher>()

                val calcBalances = ledgers.associate { ledger ->
                    var opening = ledger.opening_balance
                    if (ledger.opening_balance_type == "Cr") opening = -opening
                
                    var periodDebit = 0.0
                    var periodCredit = 0.0
                
                    allEntries.filter { it.ledger_id == ledger.id }.forEach { entry ->
                        if (entry.vouchers.date < period.startDate) {
                            if (entry.entry_type == "Debit") opening += entry.amount
                            else opening -= entry.amount
                        } else if (entry.vouchers.date <= period.endDate) {
                            if (entry.entry_type == "Debit") periodDebit += entry.amount
                            else periodCredit += entry.amount
                        }
                    }
                
                    ledger.id!! to LedgerBalance(opening, opening + periodDebit - periodCredit)
                }
                balances = calcBalances
            } catch (e: Exception) {
                println("Error fetching filtered ledger balances: ${e.message}")
            }
        }
    }

    LaunchedEffect(period) { fetchData() }
    
    LaunchedEffect(Unit) {
        fetchData()
        focusRequester.requestFocus()
    }

    BackHandler(enabled = isSummaryMode) {
        isSummaryMode = false
    }

    if (isSummaryMode && ledgers.isNotEmpty() && selectedIndex < ledgers.size) {
        LedgerMonthlySummary(
            company = company,
            ledger = ledgers[selectedIndex],
            period = period,
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
                                if (selectedIndex < ledgers.size - 1) selectedIndex++
                                true
                            }
                            Key.DirectionUp -> {
                                if (selectedIndex > 0) selectedIndex--
                                true
                            }
                            Key.Enter -> {
                                if (ledgers.isNotEmpty()) isSummaryMode = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val isMobile = maxWidth < 600.dp
            val balanceWidth = if (isMobile) 80.dp else 120.dp
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val constraints = this@BoxWithConstraints
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Header with Back Button
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                            }
                            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp), verticalAlignment = Alignment.Bottom) {
                            Text("Particulars", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Opening", modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Closing", modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(40.dp))
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(ledgers) { index, ledger ->
                                val ledgerBalance = balances[ledger.id] ?: LedgerBalance(ledger.opening_balance, ledger.current_balance)

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
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(ledger.ledger_name, modifier = Modifier.weight(1.5f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                                        Text(ledgerBalance.opening.formatWithSign(), modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                        Text(ledgerBalance.closing.formatWithSign(), modifier = Modifier.width(balanceWidth), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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

@Composable
fun LedgerMonthlySummary(
    company: Company,
    ledger: Ledger,
    period: AccountPeriod,
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
            MonthlyLedgerData(months[monthSequence.indexOf(m)]) 
        }) 
    }
    var effectiveOpeningBalanceState by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                val entries = supabase.from("voucher_entries").select(Columns.raw("amount, entry_type, vouchers(date, company_id, voucher_type)")) {
                    filter { eq("ledger_id", ledger.id!!) }
                }.decodeList<VoucherEntryWithVoucher>()

                val dataMap = monthSequence.associateWith { m -> 
                    MonthlyLedgerData(months[monthSequence.indexOf(m)]) 
                }.toMutableMap()

                var opening = ledger.opening_balance
                if (ledger.opening_balance_type == "Cr") opening = -opening

                entries.forEach { entry ->
                    if (entry.vouchers.date < period.startDate) {
                        if (entry.entry_type == "Debit") {
                            opening += entry.amount
                        } else {
                            opening -= entry.amount
                        }
                    } else if (entry.vouchers.date <= period.endDate) {
                        val monthLabel = entry.vouchers.date.toMonthYearLabel()
                        val monthName = monthLabel.split(" ")[0]
                        val monthInt = when (monthName) {
                            "Jan" -> 1; "Feb" -> 2; "Mar" -> 3; "Apr" -> 4
                            "May" -> 5; "Jun" -> 6; "Jul" -> 7; "Aug" -> 8
                            "Sep" -> 9; "Oct" -> 10; "Nov" -> 11; "Dec" -> 12
                            else -> 0
                        }
                        val monthData = dataMap[monthInt]
                        if (monthData != null) {
                            if (entry.entry_type == "Debit") {
                                monthData.debit += entry.amount
                            } else {
                                monthData.credit += entry.amount
                            }
                        }
                    }
                }

                effectiveOpeningBalanceState = opening
                var currentBalance = opening

                monthSequence.forEach { m ->
                    val monthData = dataMap[m]!!
                    currentBalance += (monthData.debit - monthData.credit)
                    monthData.balance = currentBalance
                }

                monthlyDataMap = dataMap
            } catch (e: Exception) {
                println("Error fetching ledger summary: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(ledger.id, period) { fetchData() }
    
    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(ledger.ledger_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Monthly Summary", style = MaterialTheme.typography.bodySmall)
                        Text("For ${period.startDate.toDisplayDate()} to ${period.endDate.toDisplayDate()}", style = MaterialTheme.typography.bodySmall)
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
                val constraints = this@BoxWithConstraints

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val contentWidth = if (isMobile) 800.dp else constraints.maxWidth
                    
                    Column(modifier = Modifier.width(contentWidth)) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Text("Particulars", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            SummaryColumnHeader("Debit", Modifier.weight(1f))
                            SummaryColumnHeader("Credit", Modifier.weight(1f))
                            SummaryColumnHeader("Closing Balance", Modifier.weight(1.5f))
                        }
                        HorizontalDivider(thickness = 2.dp, color = Color.Black)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            // Opening Balance Row
                            item {
                                LedgerSummaryRow(
                                    label = "Opening Balance",
                                    italic = true,
                                    closingValue = effectiveOpeningBalanceState.formatWithSign()
                                )
                            }

                            // Monthly Rows
                            items(monthSequence) { m ->
                                val data = monthlyDataMap[m]!!
                                LedgerSummaryRow(
                                    label = data.monthName,
                                    debit = if (data.debit != 0.0) data.debit.format() else "",
                                    credit = if (data.credit != 0.0) data.credit.format() else "",
                                    closingValue = data.balance.formatWithSign()
                                )
                            }
                        }

                        HorizontalDivider(thickness = 2.dp, color = Color.Black)
                        // Grand Total Row
                        val totalDebit = monthlyDataMap.values.sumOf { it.debit }
                        val totalCredit = monthlyDataMap.values.sumOf { it.credit }
                        val finalBal = monthlyDataMap[3]?.balance ?: 0.0

                        LedgerSummaryRow(
                            label = "Grand Total",
                            bold = true,
                            debit = totalDebit.format(),
                            credit = totalCredit.format(),
                            closingValue = finalBal.formatWithSign()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerSummaryRow(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    debit: String = "",
    credit: String = "",
    closingValue: String = ""
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (italic) MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else MaterialTheme.typography.bodyMedium
        )
        
        Text(debit, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        Text(credit, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
        Text(closingValue, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
