package com.wpt.wptaccount

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

/**
 * Main Screen for creating Sale and Purchase Vouchers.
 * Handles inventory details, tax calculations, and party/ledger selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEntryScreen(
    company: Company,
    voucherType: String, // "Sale" or "Purchase"
    onHomeClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onStockSummaryClick: () -> Unit,
    onGstDetailsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onVoucherListClick: () -> Unit,
    onSaleClick: () -> Unit = {},
    onPurchaseClick: () -> Unit = {},
    onPaymentClick: () -> Unit = {},
    onReceiptClick: () -> Unit = {},
    onContraClick: () -> Unit = {},
    onJournalClick: () -> Unit = {},
    onBack: () -> Unit
) {
    // ----------------------------------------------------------------
    // 1. STATE MANAGEMENT
    // ----------------------------------------------------------------
    
    // Basic Voucher Info
    var date by remember { mutableStateOf("17-08-2024") }
    var voucherNo by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }
    var refDate by remember { mutableStateOf("17-08-2024") }
    var selectedPartyId by remember { mutableStateOf<String?>(null) }
    var selectedLedgerId by remember { mutableStateOf<String?>(null) } // Sales or Purchase A/c
    
    // Transactional Rows (Inventory & Taxes)
    val items = remember { mutableStateListOf(ItemRow()) }
    val taxEntries = remember { mutableStateListOf<TaxRow>() }
    var narration by remember { mutableStateOf("") }
    
    // Master Data for Dropdowns
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var stockItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    
    // UI Feedback States
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bill-wise Details (for Outstanding tracking)
    var showBillWiseDialog by remember { mutableStateOf(false) }
    val partyReferences = remember { mutableStateListOf<VoucherReference>() }

    // Quick Add Dialog States (triggered by Alt+C or 'Create' button)
    var showAddLedger by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // ----------------------------------------------------------------
    // 2. DATA FETCHING LOGIC
    // ----------------------------------------------------------------
    
    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                // Fetch Ledgers belonging to this company
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()
                
                // Fetch Accounting Groups
                groups = supabase.from("groups").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<AccountingGroup>()

                // Fetch Stock Items
                stockItems = supabase.from("stock_items").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<StockItem>()

                // Auto-increment Voucher Number based on last entry
                val lastVouchers = supabase.from("vouchers").select {
                    filter {
                        eq("company_id", company.id!!)
                        eq("voucher_type", voucherType)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }.decodeList<Voucher>()

                if (lastVouchers.isNotEmpty()) {
                    val lastNo = lastVouchers[0].voucher_number
                    val nextNo = (lastNo?.toIntOrNull() ?: 0) + 1
                    voucherNo = nextNo.toString()
                } else {
                    voucherNo = "1"
                }
            } catch (e: Exception) {
                println("Fetch error: ${e.message}")
                errorMessage = "Failed to load data"
            } finally {
                isLoading = false
            }
        }
    }

    // Load data on first launch
    LaunchedEffect(Unit) { fetchData() }

    // ----------------------------------------------------------------
    // 3. COMPUTED CALCULATIONS
    // ----------------------------------------------------------------
    
    val itemSubTotal = items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val taxTotal = taxEntries.sumOf { it.amount }
    val grandTotal = itemSubTotal + taxTotal

    // ----------------------------------------------------------------
    // 4. UI STRUCTURE
    // ----------------------------------------------------------------
    
    AppNavigationDrawer(
        currentScreen = if (voucherType == "Sale") ScreenType.Sale else ScreenType.Purchase,
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
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$voucherType Creation") },
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- SECTION: Header Info (Date, No, Ref) ---
                    val headerScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(headerScrollState),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InventoryField(
                            label = "Date",
                            value = date,
                            modifier = Modifier.width(200.dp),
                            labelWidth = 60.dp
                        ) { date = it }

                        InventoryField(
                            label = "Voucher No.",
                            value = voucherNo,
                            modifier = Modifier.width(150.dp),
                            labelWidth = 80.dp
                        ) { voucherNo = it }
                        
                        InventoryField(
                            label = if (voucherType == "Sale") "Ref No." else "Supplier Inv No.",
                            value = refNo,
                            modifier = Modifier.width(250.dp),
                            labelWidth = 100.dp
                        ) { refNo = it }

                        if (voucherType == "Purchase") {
                            InventoryField(
                                label = "Supplier Inv Date",
                                value = refDate,
                                modifier = Modifier.width(200.dp),
                                labelWidth = 120.dp
                            ) { refDate = it }
                        }
                    }

                    // --- SECTION: Party & Sales/Purchase Ledger Selection ---
                    TallySearchableInput(
                        label = "Party A/c Name",
                        options = ledgers.map { it.ledger_name },
                        selected = ledgers.find { it.id == selectedPartyId }?.ledger_name ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        onCreate = { showAddLedger = true }
                    ) { name ->
                        selectedPartyId = ledgers.find { it.ledger_name == name }?.id
                    }

                    TallySearchableInput(
                        label = if (voucherType == "Sale") "Sales Ledger" else "Purchase Ledger",
                        options = ledgers.filter { 
                            if (voucherType == "Sale") it.ledger_name.contains("Sales", ignoreCase = true)
                            else it.ledger_name.contains("Purchase", ignoreCase = true)
                        }.map { it.ledger_name },
                        selected = ledgers.find { it.id == selectedLedgerId }?.ledger_name ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        onCreate = { showAddLedger = true }
                    ) { name ->
                        selectedLedgerId = ledgers.find { it.ledger_name == name }?.id
                    }

                    HorizontalDivider()

                    // --- SECTION: Items Table (Inventory) ---
                    Text("Inventory Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    val itemScrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(itemScrollState)) {
                        val contentWidth = 600.dp 
                        
                        Column(modifier = Modifier.width(contentWidth)) {
                            // Table Header
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Name of Item", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text("Quantity", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Rate", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Spacer(Modifier.width(48.dp))
                            }

                            // Dynamic Rows
                            items.forEachIndexed { index, row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(2f)) {
                                        TallySearchableInput(
                                            label = "",
                                            options = stockItems.map { it.item_name },
                                            selected = stockItems.find { it.id == row.stockItemId }?.item_name ?: "",
                                            onCreate = { showAddItem = true }
                                        ) { name ->
                                            val item = stockItems.find { it.item_name == name }
                                            if (item != null) {
                                                val r = if (row.rate == "0") item.opening_rate.toString() else row.rate
                                                val q = row.qty
                                                val a = (q.toDoubleOrNull() ?: 0.0) * (r.toDoubleOrNull() ?: 0.0)
                                                
                                                items[index] = row.copy(
                                                    stockItemId = item.id!!,
                                                    rate = r,
                                                    amount = a.format(2)
                                                )
                                            }
                                        }
                                    }
                                    
                                    OutlinedTextField(
                                        value = row.qty,
                                        onValueChange = { 
                                            val q = it
                                            val r = row.rate
                                            val a = (q.toDoubleOrNull() ?: 0.0) * (r.toDoubleOrNull() ?: 0.0)
                                            items[index] = row.copy(qty = q, amount = a.format(2))
                                        },
                                        modifier = Modifier.width(80.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = row.rate,
                                        onValueChange = { 
                                            val r = it
                                            val q = row.qty
                                            val a = (q.toDoubleOrNull() ?: 0.0) * (r.toDoubleOrNull() ?: 0.0)
                                            items[index] = row.copy(rate = r, amount = a.format(2))
                                        },
                                        modifier = Modifier.width(100.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = row.amount,
                                        onValueChange = { 
                                            val a = it
                                            val q = row.qty.toDoubleOrNull() ?: 1.0
                                            val r = if (q != 0.0) (a.toDoubleOrNull() ?: 0.0) / q else 0.0
                                            items[index] = row.copy(amount = a, rate = if (q != 0.0) r.format(2) else row.rate)
                                        },
                                        modifier = Modifier.width(120.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                                        singleLine = true
                                    )

                                    IconButton(onClick = { items.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = { items.add(ItemRow()) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Item")
                    }

                    HorizontalDivider()

                    // --- SECTION: Ledger Details (Taxes/Charges) ---
                    Text("Ledger Details (Taxes/Charges)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    val ledgerScrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(ledgerScrollState)) {
                        val contentWidth = 600.dp
                        
                        Column(modifier = Modifier.width(contentWidth)) {
                            taxEntries.forEachIndexed { index, row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(2f)) {
                                        TallySearchableInput(
                                            label = "",
                                            options = ledgers.map { it.ledger_name },
                                            selected = ledgers.find { it.id == row.ledgerId }?.ledger_name ?: "",
                                            onCreate = { showAddLedger = true }
                                        ) { name ->
                                            val ledger = ledgers.find { it.ledger_name == name }
                                            if (ledger != null) {
                                                val rate = ledger.tax_rate ?: 0.0
                                                taxEntries[index] = row.copy(
                                                    ledgerId = ledger.id!!,
                                                    taxRate = rate,
                                                    amount = itemSubTotal * rate / 100.0
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(180.dp)) 
                                    
                                    OutlinedTextField(
                                        value = row.amount.toString(),
                                        onValueChange = { 
                                            taxEntries[index] = row.copy(amount = it.toDoubleOrNull() ?: 0.0)
                                        },
                                        modifier = Modifier.width(120.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true
                                    )

                                    IconButton(onClick = { taxEntries.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = { taxEntries.add(TaxRow()) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Ledger")
                    }

                    HorizontalDivider()

                    // --- SECTION: Totals (Subtotal, Tax, Grand Total) ---
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(modifier = Modifier.width(300.dp)) {
                            Text("Sub Total:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(itemSubTotal.format(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.width(300.dp)) {
                            Text("Tax Total:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(taxTotal.format(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.width(300.dp)) {
                            Text("Grand Total:", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(grandTotal.format(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Narration field
                    InventoryField("Narration", narration) { narration = it }

                    // Error feedback
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // --- SECTION: Save Button ---
                    Button(
                        onClick = {
                            // Validation
                            if (selectedPartyId == null || selectedLedgerId == null || items.all { it.stockItemId.isEmpty() }) {
                                errorMessage = "Please fill all mandatory fields"
                                return@Button
                            }

                            // Date Validation for Purchase: Invoice Date cannot be after Voucher Date
                            if (voucherType == "Purchase") {
                                try {
                                    val vParts = date.split("-")
                                    val iParts = refDate.split("-")
                                    if (vParts.size == 3 && iParts.size == 3) {
                                        val vDate = "${vParts[2]}${vParts[1]}${vParts[0]}"
                                        val iDate = "${iParts[2]}${iParts[1]}${iParts[0]}"
                                        if (iDate > vDate) {
                                            errorMessage = "Supplier Invoice Date cannot be later than Voucher Date"
                                            return@Button
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore parsing errors for now
                                }
                            }
                            
                            val party = ledgers.find { it.id == selectedPartyId }
                            // If Bill-by-bill is enabled, show reference dialog first
                            if (party?.bill_by_bill == true) {
                                showBillWiseDialog = true
                            } else {
                                performSave(
                                    scope, company, voucherType, voucherNo, refNo, refDate, selectedPartyId, 
                                    selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                                    items, taxEntries, stockItems, ledgers, partyReferences,
                                    { isSaving = it }, { errorMessage = it }, onBack
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save $voucherType")
                    }
                }
            }
        }
    }

    // --- OVERLAYS: Dialogs & Modals ---

    // Reference Management (Bill-wise)
    if (showBillWiseDialog) {
        val party = ledgers.find { it.id == selectedPartyId }
        BillWiseDetailsDialog(
            partyName = party?.ledger_name ?: "Party",
            totalAmount = grandTotal,
            initialReferences = partyReferences,
            onDismiss = { showBillWiseDialog = false },
            onConfirm = { refs ->
                partyReferences.clear()
                partyReferences.addAll(refs)
                showBillWiseDialog = false
                performSave(
                    scope, company, voucherType, voucherNo, refNo, refDate, selectedPartyId, 
                    selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                    items, taxEntries, stockItems, ledgers, partyReferences,
                    { isSaving = it }, { errorMessage = it }, onBack
                )
            }
        )
    }

    // Quick Add Ledger (Alt+C)
    if (showAddLedger) {
        CompactAddLedgerDialog(company, groups, onDismiss = { showAddLedger = false }) {
            fetchData()
            showAddLedger = false
        }
    }

    // Quick Add Stock Item (Alt+C)
    if (showAddItem) {
        CompactAddItemDialog(company, onDismiss = { showAddItem = false }) {
            fetchData()
            showAddItem = false
        }
    }
}

/**
 * Simplified dialog for adding a ledger quickly without leaving the screen.
 */
@Composable
fun CompactAddLedgerDialog(
    company: Company,
    groups: List<AccountingGroup>,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var taxRate by remember { mutableStateOf("0") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Ledger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                InventoryDropdown("Under", groups.map { it.group_name }, groups.find { it.id == selectedGroupId }?.group_name ?: "") { n ->
                    selectedGroupId = groups.find { it.group_name == n }?.id ?: ""
                }
                OutlinedTextField(value = taxRate, onValueChange = { taxRate = it }, label = { Text("Tax Rate (%)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    val newLedger = Ledger(
                        company_id = company.id!!,
                        ledger_name = name,
                        group_id = selectedGroupId,
                        tax_rate = taxRate.toDoubleOrNull() ?: 0.0
                    )
                    supabase.from("ledgers").insert(newLedger)
                    onSuccess()
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Simplified dialog for adding a stock item quickly.
 */
@Composable
fun CompactAddItemDialog(
    company: Company,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Item") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    // Note: Basic implementation, assumes default unit/group.
                    onSuccess() 
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
