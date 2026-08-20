package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ItemRow(
    var stockItemId: String = "",
    var qty: String = "1",
    var rate: String = "0",
    var amount: Double = 0.0
)

@Serializable
data class TaxRow(
    var ledgerId: String = "",
    var taxRate: Double = 0.0,
    var amount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEntryScreen(
    company: Company,
    voucherType: String, // "Sale" or "Purchase"
    onBack: () -> Unit
) {
    var date by remember { mutableStateOf("17-08-2024") }
    var refNo by remember { mutableStateOf("") }
    var selectedPartyId by remember { mutableStateOf<String?>(null) }
    var selectedLedgerId by remember { mutableStateOf<String?>(null) } // Sales or Purchase A/c
    
    val items = remember { mutableStateListOf(ItemRow()) }
    val taxEntries = remember { mutableStateListOf<TaxRow>() }
    var narration by remember { mutableStateOf("") }
    
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AccountingGroup>>(emptyList()) }
    var stockItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog States for Alt+C
    var showAddLedger by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun updateLedgerBalance(ledgerId: String, amount: Double, entryType: String) {
        val ledger = supabase.from("ledgers").select {
            filter { eq("id", ledgerId) }
        }.decodeSingle<Ledger>()
        
        val group = groups.find { it.id == ledger.group_id }
        val groupName = group?.group_name ?: ""
        
        val isDebitNature = isDebitNature(groupName)

        val adjustment = if (isDebitNature) {
            if (entryType == "Debit") amount else -amount
        } else {
            if (entryType == "Credit") amount else -amount
        }

        val newBalance = ledger.current_balance + adjustment
        
        supabase.from("ledgers").update(buildJsonObject {
            put("current_balance", newBalance)
        }) {
            filter { eq("id", ledgerId) }
        }
    }

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()
                
                groups = supabase.from("groups").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<AccountingGroup>()

                stockItems = supabase.from("stock_items").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<StockItem>()
            } catch (e: Exception) {
                println("Fetch error: ${e.message}")
                errorMessage = "Failed to load data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val itemSubTotal = items.sumOf { it.amount }
    val taxTotal = taxEntries.sumOf { it.amount }
    val grandTotal = itemSubTotal + taxTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$voucherType Creation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InventoryField("Date", date, modifier = Modifier.weight(1f)) { date = it }
                    InventoryField(if (voucherType == "Sale") "Ref No." else "Supplier Inv No.", refNo, modifier = Modifier.weight(1f)) { refNo = it }
                }

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

                // Items Table
                Text("Inventory Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Name of Item", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text("Quantity", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text("Rate", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text("Amount", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Spacer(Modifier.width(48.dp))
                }

                items.forEachIndexed { index, row ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(2f)) {
                            TallySearchableInput(
                                label = "",
                                options = stockItems.map { it.item_name },
                                selected = stockItems.find { it.id == row.stockItemId }?.item_name ?: "",
                                onCreate = { showAddItem = true }
                            ) { name ->
                                val item = stockItems.find { it.item_name == name }
                                if (item != null) {
                                    items[index] = row.copy(
                                        stockItemId = item.id!!,
                                        rate = if (row.rate == "0") item.opening_rate.toString() else row.rate
                                    ).apply {
                                        amount = (qty.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)
                                    }
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = row.qty,
                            onValueChange = { 
                                items[index] = row.copy(qty = it).apply {
                                    amount = (it.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = row.rate,
                            onValueChange = { 
                                items[index] = row.copy(rate = it).apply {
                                    amount = (qty.toDoubleOrNull() ?: 0.0) * (it.toDoubleOrNull() ?: 0.0)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                            singleLine = true
                        )
                        
                        Text(
                            text = row.amount.format(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { items.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                TextButton(onClick = { items.add(ItemRow()) }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Item")
                }

                HorizontalDivider()

                // Additional Ledgers (Taxes, Freight, etc.)
                Text("Ledger Details (Taxes/Charges)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                taxEntries.forEachIndexed { index, row ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                        
                        Spacer(Modifier.weight(2f)) // Skip Qty/Rate columns for tax
                        
                        OutlinedTextField(
                            value = row.amount.toString(),
                            onValueChange = { 
                                taxEntries[index] = row.copy(amount = it.toDoubleOrNull() ?: 0.0)
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                            singleLine = true
                        )

                        IconButton(onClick = { taxEntries.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                TextButton(onClick = { taxEntries.add(TaxRow()) }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Ledger")
                }

                HorizontalDivider()

                // Totals
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

                InventoryField("Narration", narration) { narration = it }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (selectedPartyId == null || selectedLedgerId == null || items.all { it.stockItemId.isEmpty() }) {
                            errorMessage = "Please fill all mandatory fields"
                            return@Button
                        }
                        
                        scope.launch {
                            try {
                                isSaving = true
                                
                                // Safe date conversion: DD-MM-YYYY -> YYYY-MM-DD
                                val dbDate = try {
                                    val parts = date.split("-")
                                    if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else date
                                } catch (e: Exception) { date }

                                // 1. Create Voucher
                                val voucher = Voucher(
                                    company_id = company.id!!,
                                    voucher_type = voucherType,
                                    reference_no = refNo.ifEmpty { null },
                                    party_ledger_id = selectedPartyId,
                                    date = dbDate,
                                    narration = narration,
                                    total_amount = grandTotal
                                )
                                val savedVoucher = supabase.from("vouchers").insert(voucher) {
                                    select()
                                }.decodeSingle<Voucher>()
                                
                                val voucherId = savedVoucher.id!!

                                // 2. Save Stock Items & Update Quantities
                                items.forEach { row ->
                                    if (row.stockItemId.isNotEmpty()) {
                                        val qtyVal = row.qty.toDoubleOrNull() ?: 0.0
                                        supabase.from("voucher_stock_items").insert(VoucherStockItem(
                                            voucher_id = voucherId,
                                            stock_item_id = row.stockItemId,
                                            quantity = qtyVal,
                                            rate = row.rate.toDoubleOrNull() ?: 0.0,
                                            amount = row.amount
                                        ))
                                        
                                        // Update actual stock
                                        val stockItem = stockItems.find { it.id == row.stockItemId }
                                        if (stockItem != null) {
                                            val adjustment = if (voucherType == "Purchase") qtyVal else -qtyVal
                                            val newQty = stockItem.current_quantity + adjustment
                                            supabase.from("stock_items").update(buildJsonObject {
                                                put("current_quantity", newQty)
                                            }) { filter { eq("id", stockItem.id!!) } }
                                        }
                                    }
                                }

                                // 3. Save Accounting Entries & Update Balances
                                // Party Entry
                                val partyEntryType = if (voucherType == "Sale") "Debit" else "Credit"
                                supabase.from("voucher_entries").insert(VoucherEntry(
                                    voucher_id = voucherId,
                                    ledger_id = selectedPartyId!!,
                                    amount = grandTotal,
                                    entry_type = partyEntryType
                                ))
                                updateLedgerBalance(selectedPartyId!!, grandTotal, partyEntryType)
                                
                                // Sales/Purchase Entry
                                val ledgerEntryType = if (voucherType == "Sale") "Credit" else "Debit"
                                supabase.from("voucher_entries").insert(VoucherEntry(
                                    voucher_id = voucherId,
                                    ledger_id = selectedLedgerId!!,
                                    amount = itemSubTotal,
                                    entry_type = ledgerEntryType
                                ))
                                updateLedgerBalance(selectedLedgerId!!, itemSubTotal, ledgerEntryType)

                                // Tax Ledger Entries
                                taxEntries.forEach { tax ->
                                    if (tax.ledgerId.isNotEmpty()) {
                                        supabase.from("voucher_entries").insert(VoucherEntry(
                                            voucher_id = voucherId,
                                            ledger_id = tax.ledgerId,
                                            amount = tax.amount,
                                            entry_type = ledgerEntryType
                                        ))
                                        updateLedgerBalance(tax.ledgerId, tax.amount, ledgerEntryType)
                                    }
                                }
                                
                                onBack()
                            } catch (e: Exception) {
                                println("Save error details: ${e.message}")
                                errorMessage = "Failed to save: ${e.message?.take(100) ?: "Unknown error"}"
                            } finally {
                                isSaving = false
                            }
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

    if (showAddLedger) {
        CompactAddLedgerDialog(company, groups, onDismiss = { showAddLedger = false }) {
            fetchData()
            showAddLedger = false
        }
    }

    if (showAddItem) {
        CompactAddItemDialog(company, onDismiss = { showAddItem = false }) {
            fetchData()
            showAddItem = false
        }
    }
}

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

@Composable
fun CompactAddItemDialog(
    company: Company,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Simplified for now, just creating a primary item
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Item") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    // This requires a valid unit_id. Fetching default unit or assuming one.
                    // For simplicity, let's assume 'Pcs' unit exists or handled.
                    // Real implementation should be more robust.
                    // supabase.from("stock_items").insert(...) 
                    onSuccess() 
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
