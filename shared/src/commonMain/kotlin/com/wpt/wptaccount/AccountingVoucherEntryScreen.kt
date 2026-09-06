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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    var date by rememberSaveable { mutableStateOf(initialVoucher?.date?.toDisplayDate() ?: "17/08/2024") }
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
                    // Fetch last voucher number to auto-increment
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
                ScreenType.Stock -> onStockSummaryClick()
                ScreenType.Gst -> onGstDetailsClick()
                ScreenType.DayBook -> onVoucherListClick()
                else -> {}
            }
        }
    ) { _, onToggleDrawer, isDesktop ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$voucherType Creation") },
                    navigationIcon = {
                        IconButton(onClick = if (isDesktop) onBack else onToggleDrawer) {
                            Icon(if (isDesktop) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, contentDescription = null)
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
                    // Header Info
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InventoryField("Date", date, Modifier.width(200.dp), labelWidth = 60.dp) { date = it }
                        InventoryField("Voucher No.", voucherNo, Modifier.width(150.dp), labelWidth = 80.dp) { voucherNo = it }
                    }

                    HorizontalDivider()

                    // Entries Table
                    Text("Particulars", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    entries.forEachIndexed { index, row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            OutlinedTextField(
                                value = row.amount,
                                onValueChange = { entries[index] = row.copy(amount = it) },
                                modifier = Modifier.width(120.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                singleLine = true
                            )

                            IconButton(onClick = { entries.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    TextButton(onClick = { entries.add(AccountingRow(entryType = if (difference > 0) "Credit" else "Debit", amount = kotlin.math.abs(difference).toString())) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Row")
                    }

                    HorizontalDivider()

                    // Totals
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(modifier = Modifier.width(300.dp)) {
                            Text("Total Debit:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(totalDebit.format(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.width(300.dp)) {
                            Text("Total Credit:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(totalCredit.format(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                        }
                        if (difference != 0.0) {
                            Text("Difference: ${difference.format()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    InventoryField("Narration", narration) { narration = it }

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (entries.isEmpty() || entries.any { it.ledgerId.isEmpty() } || difference != 0.0) {
                                errorMessage = "Voucher not balanced or ledger missing"
                                return@Button
                            }
                            
                            // Sequential Bill-wise Dialog Logic
                            // Find all indices where ledger has bill_by_bill enabled
                            val billByBillIndices = entries.indices.filter { idx ->
                                val ledger = ledgers.find { it.id == entries[idx].ledgerId }
                                ledger?.bill_by_bill == true
                            }

                            if (billByBillIndices.isNotEmpty()) {
                                // Start showing dialogs from the first applicable index
                                activeRefIndex = billByBillIndices.first()
                            } else {
                                // Save immediately if no bill-by-bill logic needed
                                saveVoucher(scope, company, voucherType, voucherNo, date, narration, totalDebit, entries, { isSaving = it }, { errorMessage = it }, onBack, initialVoucher?.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save $voucherType")
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

                if (voucherIdToEdit != null) {
                    deleteVoucherData(voucherIdToEdit, voucherType)
                }

                val voucher = Voucher(
                    id = voucherIdToEdit,
                    company_id = company.id!!,
                    voucher_type = voucherType,
                    voucher_number = voucherNo.ifEmpty { null },
                    date = dbDate,
                    narration = narration,
                    total_amount = grandTotal
                )
                
                val voucherId = if (voucherIdToEdit != null) {
                    supabase.from("vouchers").update(voucher) {
                        filter { eq("id", voucherIdToEdit) }
                    }
                    voucherIdToEdit
                } else {
                    val savedVoucher = supabase.from("vouchers").insert(voucher) { select() }.decodeSingle<Voucher>()
                    savedVoucher.id!!
                }

                entries.forEach { row ->
                    supabase.from("voucher_entries").insert(VoucherEntry(
                        voucher_id = voucherId,
                        ledger_id = row.ledgerId,
                        amount = row.amount.toDoubleOrNull() ?: 0.0,
                        entry_type = row.entryType
                    ))
                    
                    // Update Ledger Balance
                    updateLedgerBalanceInternal(row.ledgerId, row.amount.toDoubleOrNull() ?: 0.0, row.entryType)

                    // Save References
                    row.references.forEach { ref ->
                        supabase.from("voucher_references").insert(ref.copy(voucher_id = voucherId))
                    }
                }
            }
            onSuccess()
        } catch (e: Exception) {
            setError("Failed to save: ${e.message}")
        } finally {
            setSaving(false)
        }
    }
}
