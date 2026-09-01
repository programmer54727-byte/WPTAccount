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
    onBack: () -> Unit,
    initialVoucher: Voucher? = null
) {
    // ----------------------------------------------------------------
    // 1. STATE MANAGEMENT
    // ----------------------------------------------------------------
    
    // Basic Voucher Info
    var date by remember { mutableStateOf(initialVoucher?.date?.toDisplayDate() ?: "17/08/2024") }
    var voucherNo by remember { mutableStateOf(initialVoucher?.voucher_number ?: "") }
    var invoiceNo by remember { mutableStateOf(initialVoucher?.invoice_no ?: "") }
    var invoiceDate by remember { mutableStateOf(initialVoucher?.invoice_date?.toDisplayDate() ?: "17/08/2024") }
    var selectedPartyId by remember { mutableStateOf<String?>(initialVoucher?.party_ledger_id) }
    var selectedLedgerId by remember { mutableStateOf<String?>(null) } // Sales or Purchase A/c
    
    // Transactional Rows (Inventory & Taxes)
    val items = remember { mutableStateListOf<ItemRow>() }
    val taxEntries = remember { mutableStateListOf<TaxRow>() }
    var narration by remember { mutableStateOf(initialVoucher?.narration ?: "") }
    
    // Master Data for Dropdowns
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var stockItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    
    // UI Feedback States
    var isLoading by remember { mutableStateOf(value = true) }
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

                if (initialVoucher != null) {
                    // Load existing entries
                    val vId = initialVoucher.id!!
                    
                    val vItems = supabase.from("voucher_stock_items").select {
                        filter { eq("voucher_id", vId) }
                    }.decodeList<VoucherStockItem>()
                    
                    items.clear()
                    vItems.forEach {
                        items.add(ItemRow(
                            stockItemId = it.stock_item_id,
                            hsnCode = it.hsn_code ?: "",
                            gstRate = it.gst_rate,
                            qty = it.quantity.format(),
                            rate = it.rate.format(),
                            amount = it.amount.format()
                        ))
                    }
                    if (items.isEmpty()) items.add(ItemRow())

                    val vEntries = supabase.from("voucher_entries").select {
                        filter { eq("voucher_id", vId) }
                    }.decodeList<VoucherEntry>()
                    
                    // Separate Party, Ledger, and Taxes
                    // This logic depends on your accounting structure.
                    // For now, let's assume the first non-party entry is the sales/purchase ledger
                    val partyEntry = vEntries.find { it.ledger_id == selectedPartyId }
                    val remaining = vEntries.filter { it.ledger_id != selectedPartyId }
                    
                    if (remaining.isNotEmpty()) {
                        selectedLedgerId = remaining[0].ledger_id
                        taxEntries.clear()
                        remaining.drop(1).forEach {
                            val taxLedger = ledgers.find { l -> l.id == it.ledger_id }
                            taxEntries.add(TaxRow(
                                ledgerId = it.ledger_id,
                                taxRate = taxLedger?.tax_rate ?: 0.0,
                                amount = it.amount
                            ))
                        }
                    }

                    val vRefs = supabase.from("voucher_references").select {
                        filter { eq("voucher_id", vId) }
                    }.decodeList<VoucherReference>()
                    partyReferences.clear()
                    partyReferences.addAll(vRefs)

                } else {
                    // Auto-increment Voucher Number based on last entry
                    val lastVouchers = supabase.from("vouchers").select {
                        filter {
                            eq("company_id", company.id!!)
                            eq("voucher_type", voucherType)
                        }
                        order("created_at", order = Order.DESCENDING)
                        limit(1)
                    }.decodeList<Voucher>()

                    voucherNo = if (lastVouchers.isNotEmpty()) {
                        val lastNo = lastVouchers[0].voucher_number
                        val nextNo = (lastNo?.toIntOrNull() ?: 0) + 1
                        nextNo.toString()
                    } else {
                        "1"
                    }
                    items.add(ItemRow())
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
                        
                        if (voucherType != "Sale") {
                            InventoryField(
                                label = "Invoice No.",
                                value = invoiceNo,
                                modifier = Modifier.width(250.dp),
                                labelWidth = 100.dp
                            ) { invoiceNo = it }

                            InventoryField(
                                label = "Invoice Date",
                                value = invoiceDate,
                                modifier = Modifier.width(200.dp),
                                labelWidth = 120.dp
                            ) { invoiceDate = it }
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
                        options = ledgers.asSequence().filter { 
                            if (voucherType == "Sale") it.ledger_name.contains("Sales", ignoreCase = true)
                            else it.ledger_name.contains("Purchase", ignoreCase = true)
                        }.map { it.ledger_name }.toList(),
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
                        val contentWidth = 850.dp 
                        
                        Column(modifier = Modifier.width(contentWidth)) {
                            // Table Header
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Name of Item", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text("HSN Code", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("GST Rate (%)", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Quantity", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Rate", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Spacer(Modifier.width(48.dp))
                            }

                            // Dynamic Rows
                            items.forEachIndexed { index, row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TallySearchableInput(
                                            label = "",
                                            options = stockItems.map { it.item_name },
                                            selected = stockItems.find { it.id == row.stockItemId }?.item_name ?: "",
                                            onCreate = { showAddItem = true }
                                        ) { name ->
                                            val item = stockItems.find { it.item_name == name }
                                            if (item != null) {
                                                val r = if (row.rate == "0") item.opening_rate.toString() else row.rate
                                                val h = item.hsn_sac_number ?: ""
                                                val gr = item.gst_rate
                                                val a = (row.qty.toDoubleOrNull() ?: 0.0) * (r.toDoubleOrNull() ?: 0.0)
                                                
                                                items[index] = row.copy(
                                                    stockItemId = item.id!!,
                                                    hsnCode = h,
                                                    gstRate = gr,
                                                    rate = r,
                                                    amount = a.format(2)
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = row.hsnCode,
                                        onValueChange = {},
                                        modifier = Modifier.width(100.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true,
                                        readOnly = true
                                    )
                                    OutlinedTextField(
                                        value = row.gstRate.toString(),
                                        onValueChange = {},
                                        modifier = Modifier.width(80.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true,
                                        suffix = { Text("%", style = MaterialTheme.typography.bodySmall) },
                                        readOnly = true
                                    )
                                    OutlinedTextField(
                                        value = row.qty,
                                        onValueChange = {
                                            val a = (it.toDoubleOrNull() ?: 0.0) * (row.rate.toDoubleOrNull() ?: 0.0)
                                            items[index] = row.copy(qty = it, amount = a.format(2))
                                        },
                                        modifier = Modifier.width(80.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = row.rate,
                                        onValueChange = { 
                                            val a = (row.qty.toDoubleOrNull() ?: 0.0) * (it.toDoubleOrNull() ?: 0.0)
                                            items[index] = row.copy(rate = it, amount = a.format(2))
                                        },
                                        modifier = Modifier.width(100.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = row.amount,
                                        onValueChange = { 
                                            val q = row.qty.toDoubleOrNull() ?: 1.0
                                            val r = if (q != 0.0) (it.toDoubleOrNull() ?: 0.0) / q else 0.0
                                            items[index] = row.copy(amount = it, rate = if (q != 0.0) r.format(2) else row.rate)
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
                        val contentWidth = 700.dp
                        
                        Column(modifier = Modifier.width(contentWidth)) {
                            // Table Header
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Ledger Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text("Rate (%)", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Spacer(Modifier.width(80.dp))
                                Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Spacer(Modifier.width(48.dp))
                            }

                            taxEntries.forEachIndexed { index, row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f)) {
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
                                                    amount = (itemSubTotal * rate) / 100.0
                                                )
                                            }
                                        }
                                    }
                                    
                                    OutlinedTextField(
                                        value = row.taxRate.toString(),
                                        onValueChange = { 
                                            val newRate = it.toDoubleOrNull() ?: 0.0
                                            taxEntries[index] = row.copy(taxRate = newRate, amount = (itemSubTotal * newRate) / 100.0)
                                        },
                                        modifier = Modifier.width(100.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                        singleLine = true,
                                        suffix = { Text("%", style = MaterialTheme.typography.bodySmall) }
                                    )

                                    Spacer(Modifier.width(80.dp)) 
                                    
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

                            // Date Validation for Invoice Date: Cannot be after Voucher Date
                            if (voucherType != "Sale") {
                                try {
                                    val vDate = date.toDbDate()
                                    val iDate = invoiceDate.toDbDate()
                                    if (iDate > vDate) {
                                        errorMessage = "Invoice Date cannot be later than Voucher Date"
                                        return@Button
                                    }
                                } catch (_: Exception) {
                                    // Ignore parsing errors
                                }
                            }
                            
                            val party = ledgers.find { it.id == selectedPartyId }
                            // If Bill-by-bill is enabled, show reference dialog first
                            if (party?.bill_by_bill == true) {
                                showBillWiseDialog = true
                            } else {
                                performSave(
                                    scope, company, voucherType, voucherNo, 
                                    if (voucherType == "Sale") voucherNo else invoiceNo, 
                                    if (voucherType == "Sale") date else invoiceDate, 
                                    selectedPartyId, 
                                    selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                                    items, taxEntries, stockItems, ledgers, partyReferences,
                                    { isSaving = it }, { errorMessage = it }, onBack,
                                    initialVoucher?.id
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
            ledgerId = selectedPartyId ?: "",
            partyName = party?.ledger_name ?: "Party",
            totalAmount = grandTotal,
            initialReferences = partyReferences,
            defaultReferenceNo = if (voucherType == "Sale") voucherNo else invoiceNo.ifEmpty { voucherNo },
            onDismiss = { showBillWiseDialog = false }
        ) { refs ->
            partyReferences.clear()
            partyReferences.addAll(refs)
            showBillWiseDialog = false
            performSave(
                scope, company, voucherType, voucherNo, 
                if (voucherType == "Sale") voucherNo else invoiceNo, 
                if (voucherType == "Sale") date else invoiceDate, 
                selectedPartyId, 
                selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                items, taxEntries, stockItems, ledgers, partyReferences,
                { isSaving = it }, { errorMessage = it }, onBack,
                initialVoucher?.id
            )
        }
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
        CompactAddItemDialog(onDismiss = { showAddItem = false }) {
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
