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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AccountingRow(
    var ledgerId: String = "",
    var amount: String = "0",
    var entryType: String = "Debit",
    var references: List<VoucherReference> = emptyList()
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
    onBack: () -> Unit
) {
    var date by remember { mutableStateOf("17-08-2024") }
    var voucherNo by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }
    
    val entries = remember { mutableStateListOf(AccountingRow()) }
    var narration by remember { mutableStateOf("") }
    
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var showAddLedger by remember { mutableStateOf(false) }
    var activeRefIndex by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()

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
                            
                            // Check for Party ledgers to show Bill-wise details
                            val partyIndex = if (voucherType == "Payment") {
                                entries.indexOfFirst { it.entryType == "Debit" }
                            } else if (voucherType == "Receipt") {
                                entries.indexOfFirst { it.entryType == "Credit" }
                            } else -1

                            if (partyIndex != -1) {
                                activeRefIndex = partyIndex
                            } else {
                                // Save immediately if no party logic needed
                                saveVoucher(scope, company, voucherType, voucherNo, date, narration, totalDebit, entries, { isSaving = it }, { errorMessage = it }, onBack)
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
            partyName = ledger?.ledger_name ?: "Party",
            totalAmount = row.amount.toDoubleOrNull() ?: 0.0,
            initialReferences = row.references,
            onDismiss = { activeRefIndex = null },
            onConfirm = { refs ->
                entries[index] = row.copy(references = refs)
                activeRefIndex = null
                // Trigger save after references confirmed
                saveVoucher(scope, company, voucherType, voucherNo, date, narration, totalDebit, entries, { isSaving = it }, { errorMessage = it }, onBack)
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
    onSuccess: () -> Unit
) {
    scope.launch {
        try {
            setSaving(true)
            setError(null)
            
            withContext(NonCancellable) {
                val dbDate = try {
                    val parts = date.split("-")
                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else date
                } catch (e: Exception) { date }

                val voucher = Voucher(
                    company_id = company.id!!,
                    voucher_type = voucherType,
                    voucher_number = voucherNo.ifEmpty { null },
                    date = dbDate,
                    narration = narration,
                    total_amount = grandTotal
                )
                val savedVoucher = supabase.from("vouchers").insert(voucher) { select() }.decodeSingle<Voucher>()
                val voucherId = savedVoucher.id!!

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
