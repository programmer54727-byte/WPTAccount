package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
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
    period: AccountPeriod,
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
    onBalanceSheetClick: () -> Unit = {},
    onProfitAndLossClick: () -> Unit = {},
    onBack: () -> Unit,
    initialVoucher: Voucher? = null
) {
    // ----------------------------------------------------------------
    // 1. STATE MANAGEMENT
    // ----------------------------------------------------------------
    
    // Basic Voucher Info
    var date by rememberSaveable { mutableStateOf(initialVoucher?.date?.toDisplayDate() ?: period.startDate.toDisplayDate()) }
    var voucherNo by rememberSaveable { mutableStateOf(initialVoucher?.voucher_number ?: "") }
    var invoiceNo by rememberSaveable { mutableStateOf(initialVoucher?.invoice_no ?: "") }
    var invoiceDate by rememberSaveable { mutableStateOf(initialVoucher?.invoice_date?.toDisplayDate() ?: period.startDate.toDisplayDate()) }
    var selectedPartyId by rememberSaveable { mutableStateOf<String?>(initialVoucher?.party_ledger_id) }
    var selectedLedgerId by rememberSaveable { mutableStateOf<String?>(null) } // Sales or Purchase A/c
    
    // Transactional Rows (Inventory & Taxes)
    val items = rememberSaveable(saver = ItemRowListSaver) { mutableStateListOf<ItemRow>() }
    val taxEntries = rememberSaveable(saver = TaxRowListSaver) { mutableStateListOf<TaxRow>() }
    var narration by rememberSaveable { mutableStateOf(initialVoucher?.narration ?: "") }
    
    // Master Data for Dropdowns
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var stockItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    
    // UI Feedback States
    var isLoading by rememberSaveable { mutableStateOf(value = true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Bill-wise Details (for Outstanding tracking)
    var showBillWiseDialog by rememberSaveable { mutableStateOf(false) }
    val partyReferences = rememberSaveable(saver = VoucherReferenceListSaver) { mutableStateListOf<VoucherReference>() }

    // Quick Add Dialog States (triggered by Alt+C or 'Create' button)
    var showAddLedger by rememberSaveable { mutableStateOf(false) }
    var showAddItem by rememberSaveable { mutableStateOf(false) }

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
                    // 1. Fetch last voucher date for this company inside current period to use as default
                    val lastAnyVouchers = supabase.from("vouchers").select {
                        filter { 
                            eq("company_id", company.id!!) 
                            gte("date", period.startDate)
                            lte("date", period.endDate)
                        }
                        order("date", order = Order.DESCENDING)
                        limit(1)
                    }.decodeList<Voucher>()

                    if (lastAnyVouchers.isNotEmpty()) {
                        val lastDate = lastAnyVouchers[0].date.toDisplayDate()
                        date = lastDate
                        invoiceDate = lastDate
                    } else {
                        date = period.startDate.toDisplayDate()
                        invoiceDate = period.startDate.toDisplayDate()
                    }

                    // 2. Auto-increment Voucher Number based on last entry of this TYPE within selected period
                    val lastVouchers = supabase.from("vouchers").select {
                        filter {
                            eq("company_id", company.id!!)
                            eq("voucher_type", voucherType)
                            gte("date", period.startDate)
                            lte("date", period.endDate)
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
                ScreenType.BalanceSheet -> onBalanceSheetClick()
                ScreenType.ProfitAndLoss -> onProfitAndLossClick()
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
                else -> {}
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = Color(0xFFF8F9FA),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = { 
                        Text(
                            text = "$voucherType Creation", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = if (isDesktop) onBack else onToggleDrawer) {
                            Icon(
                                imageVector = if (isDesktop) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, 
                                contentDescription = null,
                                tint = WptColors.PrimaryAccent
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WptColors.PrimaryAccent)
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
                    // --- SECTION: Header Info Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TallyDateField("Date", date, modifier = Modifier.width(200.dp), labelWidth = 60.dp) { date = it }
                            InventoryField("Voucher No.", voucherNo, modifier = Modifier.width(150.dp), labelWidth = 90.dp) { voucherNo = it }
                            
                            if (voucherType != "Sale") {
                                InventoryField("Invoice No.", invoiceNo, modifier = Modifier.width(200.dp), labelWidth = 100.dp) { invoiceNo = it }
                                TallyDateField("Invoice Date", invoiceDate, modifier = Modifier.width(200.dp), labelWidth = 120.dp) { invoiceDate = it }
                            }
                        }
                    }

                    // --- SECTION: Party & Selection Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TallySearchableInput(
                                label = "Party A/c Name",
                                options = ledgers.map { it.ledger_name },
                                selected = ledgers.find { it.id == selectedPartyId }?.ledger_name ?: "",
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
                                onCreate = { showAddLedger = true }
                            ) { name ->
                                selectedLedgerId = ledgers.find { it.ledger_name == name }?.id
                            }
                        }
                    }

                    // --- SECTION: Inventory Details Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Inventory Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WptColors.PrimaryAccent, modifier = Modifier.padding(bottom = 12.dp))
                            
                            val itemScrollState = rememberScrollState()
                            Column(modifier = Modifier.fillMaxWidth().horizontalScroll(itemScrollState)) {
                                val contentWidth = 900.dp 
                                Column(modifier = Modifier.width(contentWidth)) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F3F8), RoundedCornerShape(8.dp)).padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Name of Item", modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF49454F))
                                        Text("HSN Code", modifier = Modifier.width(100.dp).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Text("GST (%)", modifier = Modifier.width(80.dp).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Text("Quantity", modifier = Modifier.width(80.dp).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Text("Rate", modifier = Modifier.width(100.dp).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Text("Amount", modifier = Modifier.width(120.dp).padding(end = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Spacer(Modifier.width(48.dp))
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    items.forEachIndexed { index, row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                                        items[index] = row.copy(stockItemId = item.id!!, hsnCode = h, gstRate = gr, rate = r, amount = a.format(2))
                                                    }
                                                }
                                            }
                                            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                                            OutlinedTextField(
                                                value = row.hsnCode, onValueChange = {},
                                                modifier = Modifier.width(100.dp),
                                                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                                singleLine = true, readOnly = true,
                                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE0E0E0))
                                            )
                                            OutlinedTextField(
                                                value = row.gstRate.toString(), onValueChange = {},
                                                modifier = Modifier.width(80.dp),
                                                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                                singleLine = true, readOnly = true,
                                                suffix = { Text("%", style = MaterialTheme.typography.labelSmall) },
                                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE0E0E0))
                                            )
                                            OutlinedTextField(
                                                value = row.qty,
                                                onValueChange = {
                                                    val q = it.evaluateExpression() ?: it.toDoubleOrNull() ?: 0.0
                                                    val r = row.rate.evaluateExpression() ?: row.rate.toDoubleOrNull() ?: 0.0
                                                    val a = q * r
                                                    items[index] = row.copy(qty = it, amount = a.format(2))
                                                },
                                                modifier = Modifier.width(80.dp)
                                                    .onFocusChanged { focusState ->
                                                        if (!focusState.isFocused && row.qty.isNotEmpty()) {
                                                            val evaluated = row.qty.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = if (evaluated % 1.0 == 0.0) evaluated.toInt().toString() else evaluated.format(2)
                                                                items[index] = row.copy(qty = formatted)
                                                            }
                                                        }
                                                    }
                                                    .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                                        if (row.qty.isNotEmpty()) {
                                                            val evaluated = row.qty.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = if (evaluated % 1.0 == 0.0) evaluated.toInt().toString() else evaluated.format(2)
                                                                items[index] = row.copy(qty = formatted)
                                                            }
                                                        }
                                                        focusManager.moveFocus(FocusDirection.Next); true
                                                    } else false
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                                            )
                                            OutlinedTextField(
                                                value = row.rate,
                                                onValueChange = {
                                                    val q = row.qty.evaluateExpression() ?: row.qty.toDoubleOrNull() ?: 0.0
                                                    val r = it.evaluateExpression() ?: it.toDoubleOrNull() ?: 0.0
                                                    val a = q * r
                                                    items[index] = row.copy(rate = it, amount = a.format(2))
                                                },
                                                modifier = Modifier.width(100.dp)
                                                    .onFocusChanged { focusState ->
                                                        if (!focusState.isFocused && row.rate.isNotEmpty()) {
                                                            val evaluated = row.rate.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = if (evaluated % 1.0 == 0.0) evaluated.toInt().toString() else evaluated.format(2)
                                                                items[index] = row.copy(rate = formatted)
                                                            }
                                                        }
                                                    }
                                                    .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                                        if (row.rate.isNotEmpty()) {
                                                            val evaluated = row.rate.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = if (evaluated % 1.0 == 0.0) evaluated.toInt().toString() else evaluated.format(2)
                                                                items[index] = row.copy(rate = formatted)
                                                            }
                                                        }
                                                        focusManager.moveFocus(FocusDirection.Next); true
                                                    } else false
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                                            )
                                            OutlinedTextField(
                                                value = row.amount,
                                                onValueChange = {
                                                    val q = (row.qty.evaluateExpression() ?: row.qty.toDoubleOrNull() ?: 1.0).let { if (it == 0.0) 1.0 else it }
                                                    val a = it.evaluateExpression() ?: it.toDoubleOrNull() ?: 0.0
                                                    val r = a / q
                                                    items[index] = row.copy(amount = it, rate = r.format(2))
                                                },
                                                modifier = Modifier.width(120.dp)
                                                    .onFocusChanged { focusState ->
                                                        if (!focusState.isFocused && row.amount.isNotEmpty()) {
                                                            val evaluated = row.amount.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = evaluated.format(2)
                                                                items[index] = row.copy(amount = formatted)
                                                            }
                                                        }
                                                    }
                                                    .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                                        if (row.amount.isNotEmpty()) {
                                                            val evaluated = row.amount.evaluateExpression()
                                                            if (evaluated != null) {
                                                                val formatted = evaluated.format(2)
                                                                items[index] = row.copy(amount = formatted)
                                                            }
                                                        }
                                                        focusManager.moveFocus(FocusDirection.Next); true
                                                    } else false
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                                            )

                                            IconButton(onClick = { items.removeAt(index) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                            }
                                        }
                                    }
                                }
                            }

                            TextButton(onClick = { items.add(ItemRow()) }, colors = ButtonDefaults.textButtonColors(contentColor = WptColors.PrimaryAccent)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Item", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- SECTION: Ledger/Taxes Details Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ledger Details (Taxes/Charges)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WptColors.PrimaryAccent, modifier = Modifier.padding(bottom = 12.dp))
                            
                            val ledgerScrollState = rememberScrollState()
                            Column(modifier = Modifier.fillMaxWidth().horizontalScroll(ledgerScrollState)) {
                                val contentWidth = 700.dp
                                Column(modifier = Modifier.width(contentWidth)) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F3F8), RoundedCornerShape(8.dp)).padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ledger Name", modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF49454F))
                                        Text("Rate (%)", modifier = Modifier.width(100.dp).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Spacer(Modifier.width(80.dp))
                                        Text("Amount", modifier = Modifier.width(120.dp).padding(end = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                        Spacer(Modifier.width(48.dp))
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    taxEntries.forEachIndexed { index, row ->
                                        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                                        taxEntries[index] = row.copy(ledgerId = ledger.id!!, taxRate = rate, amount = (itemSubTotal * rate) / 100.0)
                                                    }
                                                }
                                            }
                                            
                                            OutlinedTextField(
                                                value = row.taxRate.toString(),
                                                onValueChange = { 
                                                    val newRate = it.toDoubleOrNull() ?: 0.0
                                                    taxEntries[index] = row.copy(taxRate = newRate, amount = (itemSubTotal * newRate) / 100.0)
                                                },
                                                modifier = Modifier.width(100.dp).onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) { focusManager.moveFocus(FocusDirection.Next); true } else false
                                                },
                                                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                                singleLine = true, suffix = { Text("%", style = MaterialTheme.typography.labelSmall) },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                                            )

                                            Spacer(Modifier.width(80.dp)) 
                                            
                                            OutlinedTextField(
                                                value = row.amount.toString(),
                                                onValueChange = { taxEntries[index] = row.copy(amount = it.toDoubleOrNull() ?: 0.0) },
                                                modifier = Modifier.width(120.dp).onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) { focusManager.moveFocus(FocusDirection.Next); true } else false
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                                            )

                                            IconButton(onClick = { taxEntries.removeAt(index) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                            }
                                        }
                                    }
                                }
                            }

                            TextButton(onClick = { taxEntries.add(TaxRow()) }, colors = ButtonDefaults.textButtonColors(contentColor = WptColors.PrimaryAccent)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Ledger", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- SECTION: Narration & Totals Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InventoryField("Narration", narration, labelWidth = 100.dp) { narration = it }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                Row(modifier = Modifier.width(300.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sub Total:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                    Text(itemSubTotal.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                }
                                Row(modifier = Modifier.width(300.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tax Total:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                    Text(taxTotal.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.width(300.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                    Text(grandTotal.format(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = WptColors.PrimaryAccent)
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(it, color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                    }

                    Button(
                        onClick = {
                            if (selectedPartyId == null || selectedLedgerId == null || items.all { it.stockItemId.isEmpty() }) {
                                errorMessage = "Please fill all mandatory fields"
                                return@Button
                            }

                            try {
                                val dbVoucherDate = date.toDbDate()
                                if (dbVoucherDate < period.startDate || dbVoucherDate > period.endDate) {
                                    errorMessage = "Voucher Date is outside the current period (${period.startDate.toDisplayDate()} to ${period.endDate.toDisplayDate()})"
                                    return@Button
                                }
                            } catch (_: Exception) {}

                            if (voucherType != "Sale") {
                                try {
                                    val vDate = date.toDbDate()
                                    val iDate = invoiceDate.toDbDate()
                                    if (iDate > vDate) {
                                        errorMessage = "Invoice Date cannot be later than Voucher Date"
                                        return@Button
                                    }
                                } catch (_: Exception) {}
                            }
                            
                            val party = ledgers.find { it.id == selectedPartyId }
                            if (party?.bill_by_bill == true) {
                                showBillWiseDialog = true
                            } else {
                                performSave(
                                    scope, company, voucherType, voucherNo, 
                                    if (voucherType == "Sale") voucherNo else invoiceNo, 
                                    if (voucherType == "Sale") date else invoiceDate, 
                                    selectedPartyId, selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                                    items, taxEntries, stockItems, ledgers, partyReferences,
                                    { isSaving = it }, { errorMessage = it }, onBack, initialVoucher?.id
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WptColors.PrimaryAccent),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Save $voucherType", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- OVERLAYS ---

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
                selectedPartyId, selectedLedgerId, date, narration, grandTotal, itemSubTotal, 
                items, taxEntries, stockItems, ledgers, partyReferences,
                { isSaving = it }, { errorMessage = it }, onBack, initialVoucher?.id
            )
        }
    }

    if (showAddLedger) {
        CompactAddLedgerDialog(company, groups, onDismiss = { showAddLedger = false }) {
            fetchData()
            showAddLedger = false
        }
    }

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
