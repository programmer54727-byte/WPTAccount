package com.wpt.wptaccount

import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver

@Serializable
data class AccountingRow(
    var ledgerId: String = "",
    var amount: String = "0",
    var entryType: String = "Debit",
    var references: List<VoucherReference> = emptyList()
)

val AccountingRowListSaver = listSaver<SnapshotStateList<AccountingRow>, String>(
    save = { list -> list.map { Json.encodeToString(it) } },
    restore = { strings -> 
        val list = mutableStateListOf<AccountingRow>()
        list.addAll(strings.map { Json.decodeFromString(it) })
        list
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingVoucherEntryScreen(
    company: Company,
    voucherType: String, // "Payment", "Receipt", "Contra", "Journal"
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
    onCashFlowClick: () -> Unit = {},
    onBack: () -> Unit,
    initialVoucher: Voucher? = null
) {
    var date by rememberSaveable { mutableStateOf(initialVoucher?.date?.toDisplayDate() ?: period.startDate.toDisplayDate()) }
    var voucherNo by rememberSaveable { mutableStateOf(initialVoucher?.voucher_number ?: "") }
    
    val entries = rememberSaveable(saver = AccountingRowListSaver) { mutableStateListOf<AccountingRow>() }
    var narration by rememberSaveable { mutableStateOf(initialVoucher?.narration ?: "") }
    
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Dialog States
    var showAddLedger by rememberSaveable { mutableStateOf(false) }
    var activeRefIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()

                if (initialVoucher != null) {
                    val vId = initialVoucher.id!!
                    val vEntries = supabase.from("voucher_entries").select {
                        filter { eq("voucher_id", vId) }
                    }.decodeList<VoucherEntry>()
                    
                    val vRefs = supabase.from("voucher_references").select {
                        filter { eq("voucher_id", vId) }
                    }.decodeList<VoucherReference>()

                    entries.clear()
                    vEntries.forEach { ve ->
                        entries.add(AccountingRow(
                            ledgerId = ve.ledger_id,
                            amount = ve.amount.format(),
                            entryType = ve.entry_type,
                            references = vRefs.filter { it.ledger_id == ve.ledger_id }
                        ))
                    }
                    if (entries.isEmpty()) entries.add(AccountingRow())

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
                        date = lastAnyVouchers[0].date.toDisplayDate()
                    } else {
                        date = period.startDate.toDisplayDate()
                    }

                    // 2. Fetch last voucher number to auto-increment for this TYPE within selected period
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

                    if (lastVouchers.isNotEmpty()) {
                        val lastNo = lastVouchers[0].voucher_number
                        val nextNo = (lastNo?.toIntOrNull() ?: 0) + 1
                        voucherNo = nextNo.toString()
                    } else {
                        voucherNo = "1"
                    }
                    entries.add(AccountingRow())
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val totalDebit = entries.filter { it.entryType == "Debit" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val totalCredit = entries.filter { it.entryType == "Credit" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val difference = totalDebit - totalCredit

    AppNavigationDrawer(
        currentScreen = when(voucherType) {
            "Payment" -> ScreenType.Payment
            "Receipt" -> ScreenType.Receipt
            "Contra" -> ScreenType.Contra
            "Journal" -> ScreenType.Journal
            else -> ScreenType.Home
        },
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
                ScreenType.CashFlow -> onCashFlowClick()
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
                else -> {}
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            containerColor = WptColors.AppSurface,
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
                    // Header Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                        }
                    }

                    // Entries Table Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Accounting Details", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = WptColors.PrimaryAccent,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F3F8), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Type", modifier = Modifier.width(80.dp).padding(start = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF49454F))
                                Text("Ledger Name", modifier = Modifier.weight(2f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF49454F))
                                Text("Amount", modifier = Modifier.width(120.dp).padding(end = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = Color(0xFF49454F))
                                Spacer(Modifier.width(48.dp))
                            }
                            
                            Spacer(Modifier.height(8.dp))

                            entries.forEachIndexed { index, row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Dr/Cr Toggle
                                    Box(modifier = Modifier.width(80.dp)) {
                                        InventoryDropdown("", listOf("Debit", "Credit"), row.entryType) { type ->
                                            entries[index] = row.copy(entryType = type)
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(8.dp))

                                    // Ledger Selection
                                    Box(modifier = Modifier.weight(2f)) {
                                        TallySearchableInput(
                                            label = "",
                                            options = ledgers.map { it.ledger_name },
                                            selected = ledgers.find { it.id == row.ledgerId }?.ledger_name ?: "",
                                            onCreate = { showAddLedger = true }
                                        ) { name ->
                                            val ledger = ledgers.find { it.ledger_name == name }
                                            if (ledger != null) {
                                                entries[index] = row.copy(ledgerId = ledger.id!!)
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(8.dp))

                                    // Amount
                                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                                    OutlinedTextField(
                                        value = row.amount,
                                        onValueChange = { entries[index] = row.copy(amount = it) },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .onPreviewKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                                    focusManager.moveFocus(FocusDirection.Next)
                                                    true
                                                } else false
                                            },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WptColors.PrimaryAccent,
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )

                                    IconButton(onClick = { entries.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                    }
                                }
                            }

                            TextButton(
                                onClick = { entries.add(AccountingRow(entryType = if (difference > 0) "Credit" else "Debit", amount = kotlin.math.abs(difference).toString())) },
                                colors = ButtonDefaults.textButtonColors(contentColor = WptColors.PrimaryAccent)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Row", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Narration Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InventoryField("Narration", narration, labelWidth = 100.dp) { narration = it }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Totals Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Difference: ", style = MaterialTheme.typography.bodySmall, color = Color(0xFF49454F))
                                        Text(difference.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (difference != 0.0) Color(0xFFD32F2F) else Color(0xFF43A047))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Grand Total: ", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1D1B20))
                                        Text(totalDebit.format(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = WptColors.PrimaryAccent)
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(it, color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                    }

                    Button(
                        onClick = {
                            if (entries.isEmpty() || entries.any { it.ledgerId.isEmpty() } || difference != 0.0) {
                                errorMessage = "Voucher not balanced or ledger missing"
                                return@Button
                            }

                            try {
                                val dbVoucherDate = date.toDbDate()
                                if (dbVoucherDate < period.startDate || dbVoucherDate > period.endDate) {
                                    errorMessage = "Voucher Date is outside the current period (${period.startDate.toDisplayDate()} to ${period.endDate.toDisplayDate()})"
                                    return@Button
                                }
                            } catch (_: Exception) {}
                            
                            val billByBillIndices = entries.indices.filter { idx ->
                                val ledger = ledgers.find { it.id == entries[idx].ledgerId }
                                ledger?.bill_by_bill == true
                            }

                            if (billByBillIndices.isNotEmpty()) {
                                activeRefIndex = billByBillIndices.first()
                            } else {
                                saveVoucher(scope, company, voucherType, voucherNo, date, narration, totalDebit, entries, { isSaving = it }, { errorMessage = it }, onBack, initialVoucher?.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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

    activeRefIndex?.let { index ->
        val row = entries[index]
        val ledger = ledgers.find { it.id == row.ledgerId }
        
        BillWiseDetailsDialog(
            ledgerId = row.ledgerId,
            partyName = ledger?.ledger_name ?: "Party",
            totalAmount = row.amount.toDoubleOrNull() ?: 0.0,
            initialReferences = row.references,
            defaultReferenceNo = voucherNo,
            onDismiss = { activeRefIndex = null },
            onConfirm = { refs ->
                entries[index] = row.copy(references = refs)
                
                // Logic to move to next dialog or save
                val billByBillIndices = entries.indices.filter { idx ->
                    val l = ledgers.find { it.id == entries[idx].ledgerId }
                    l?.bill_by_bill == true
                }
                
                val currentOrderIdx = billByBillIndices.indexOf(index)
                if (currentOrderIdx < billByBillIndices.size - 1) {
                    // Move to next applicable ledger
                    activeRefIndex = billByBillIndices[currentOrderIdx + 1]
                } else {
                    // All collected, save
                    activeRefIndex = null
                    saveVoucher(scope, company, voucherType, voucherNo, date, narration, totalDebit, entries, { isSaving = it }, { errorMessage = it }, onBack, initialVoucher?.id)
                }
            }
        )
    }

    if (showAddLedger) {
        // reuse CompactAddLedgerDialog from voucherEntry.kt - for now just a placeholder logic
    }
}

private fun saveVoucher(
    scope: kotlinx.coroutines.CoroutineScope,
    company: Company,
    voucherType: String,
    voucherNo: String,
    date: String,
    narration: String,
    grandTotal: Double,
    entries: List<AccountingRow>,
    setSaving: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: () -> Unit,
    voucherIdToEdit: String? = null
) {
    scope.launch {
        try {
            setSaving(true)
            setError(null)
            
            withContext(NonCancellable) {
                val dbDate = date.toDbDate()

                // 1. Prepare Entries JSON
                val entriesJson = buildJsonArray {
                    entries.forEach { row ->
                        add(buildJsonObject {
                            put("ledger_id", row.ledgerId)
                            put("amount", row.amount.toDoubleOrNull() ?: 0.0)
                            put("entry_type", row.entryType)
                        })
                    }
                }

                // 2. Prepare References JSON
                val referencesJson = buildJsonArray {
                    entries.forEach { row ->
                        row.references.forEach { ref ->
                            add(buildJsonObject {
                                put("ledger_id", row.ledgerId)
                                put("reference_type", ref.reference_type)
                                put("reference_no", ref.reference_no)
                                put("amount", ref.amount)
                            })
                        }
                    }
                }

                // 3. Call Atomic RPC
                supabase.postgrest.rpc(
                    function = "save_voucher_v3",
                    parameters = buildJsonObject {
                        put("p_voucher_id", voucherIdToEdit)
                        put("p_company_id", company.id)
                        put("p_voucher_type", voucherType)
                        put("p_voucher_number", voucherNo)
                        put("p_invoice_no", null as String?)
                        put("p_invoice_date", null as String?)
                        put("p_party_ledger_id", null as String?) // Accounting vouchers don't strictly have one party
                        put("p_date", dbDate)
                        put("p_narration", narration)
                        put("p_total_amount", grandTotal)
                        put("p_entries", entriesJson)
                        put("p_stock_items", buildJsonArray { }) // No stock in accounting vouchers
                        put("p_references", referencesJson)
                    }
                )
            }
            onSuccess()
        } catch (e: Exception) {
            println("Accounting voucher save error: ${e.message}")
            setError("Failed to save: ${e.toUserFriendlyMessage()}")
        } finally {
            setSaving(false)
        }
    }
}
