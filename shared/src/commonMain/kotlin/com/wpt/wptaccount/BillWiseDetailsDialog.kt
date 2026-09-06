package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from

import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillWiseDetailsDialog(
    ledgerId: String,
    partyName: String,
    totalAmount: Double,
    initialReferences: List<VoucherReference> = emptyList(),
    defaultReferenceNo: String = "",
    onDismiss: () -> Unit,
    onConfirm: (List<VoucherReference>) -> Unit
) {
    val references = rememberSaveable(saver = VoucherReferenceListSaver) { 
        mutableStateListOf(*initialReferences.toTypedArray()) 
    }
    var pendingReferences by remember { mutableStateOf<List<VoucherReference>>(emptyList()) }
    
    // Fetch existing "New Reference" items for this ledger
    LaunchedEffect(ledgerId) {
        try {
            pendingReferences = supabase.from("voucher_references").select {
                filter {
                    eq("ledger_id", ledgerId)
                    eq("reference_type", "New Reference")
                }
            }.decodeList<VoucherReference>()
        } catch (e: Exception) {
            println("Error fetching pending refs: ${e.message}")
        }
    }

    // Auto-add first row if empty
    LaunchedEffect(Unit) {
        if (references.isEmpty()) {
            references.add(VoucherReference(
                ledger_id = ledgerId,
                reference_type = "New Reference", 
                reference_no = defaultReferenceNo, 
                amount = totalAmount
            ))
        }
    }

    val currentTotal = references.sumOf { it.amount }
    val remaining = totalAmount - currentTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bill-wise Details for $partyName", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Type of Ref", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Name", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Amount", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Spacer(Modifier.width(48.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(references) { index, ref ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1.5f)) {
                                InventoryDropdown(
                                    label = "",
                                    options = listOf("Advance", "Against Reference", "New Reference", "On Account"),
                                    selected = ref.reference_type
                                ) { type ->
                                    val newNo = if (type == "On Account") "On Account" else ref.reference_no
                                    references[index] = ref.copy(reference_type = type, reference_no = newNo)
                                }
                            }

                            Box(modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp)) {
                                if (ref.reference_type == "Against Reference") {
                                    InventoryDropdown(
                                        label = "",
                                        options = pendingReferences.map { it.reference_no }.distinct(),
                                        selected = ref.reference_no
                                    ) { name ->
                                        val existing = pendingReferences.find { it.reference_no == name }
                                        references[index] = ref.copy(reference_no = name, amount = existing?.amount ?: ref.amount)
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = ref.reference_no,
                                        onValueChange = { references[index] = ref.copy(reference_no = it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        enabled = ref.reference_type != "On Account"
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = if (ref.amount == 0.0) "" else ref.amount.format(2),
                                onValueChange = { 
                                    val amt = it.toDoubleOrNull() ?: 0.0
                                    references[index] = ref.copy(amount = amt)
                                },
                                modifier = Modifier.width(100.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                                singleLine = true
                            )

                            IconButton(onClick = { references.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                TextButton(onClick = { 
                    references.add(VoucherReference(ledger_id = ledgerId, reference_type = "Against Reference", reference_no = "", amount = remaining))
                }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Row")
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("Total: ", fontWeight = FontWeight.Bold)
                    Text(currentTotal.format(), fontWeight = FontWeight.Bold, color = if (remaining == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                if (remaining != 0.0) {
                    Text(
                        "Difference: ${remaining.format()}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(references.toList()) },
                enabled = remaining == 0.0 && references.all { it.reference_no.isNotEmpty() }
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
