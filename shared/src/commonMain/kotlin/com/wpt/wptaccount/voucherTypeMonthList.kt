package com.wpt.wptaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherTypeMonthList(
    company: Company,
    voucherType: String,
    monthInt: Int,
    period: AccountPeriod,
    onVoucherClick: (Voucher) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit
) {
    var vouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    val monthName = when (monthInt) {
        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
        5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
        9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
        else -> ""
    }

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                vouchers = supabase.from("vouchers").select {
                    filter { 
                        eq("company_id", company.id!!)
                        eq("voucher_type", voucherType)
                        gte("date", period.startDate)
                        lte("date", period.endDate)
                    }
                    order("date", order = Order.DESCENDING)
                }.decodeList<Voucher>().filter {
                    val dateParts = it.date.split("-")
                    dateParts.size == 3 && dateParts[1].toInt() == monthInt
                }

                ledgers = supabase.from("ledgers").select {
                    filter { eq("company_id", company.id!!) }
                }.decodeList<Ledger>()
            } catch (e: Exception) {
                println("Fetch vouchers error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(voucherType, monthInt, period) { fetchData() }

    val filteredVouchers = vouchers.filter { voucher ->
        val partyName = ledgers.find { it.id == voucher.party_ledger_id }?.ledger_name ?: "Direct Entry"
        partyName.contains(searchQuery, ignoreCase = true) || 
        (voucher.voucher_number ?: "").contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("$voucherType: $monthName", style = MaterialTheme.typography.titleMedium)
                        Text("Day Book", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, "Add $voucherType")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search by Particulars or Vch No.") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                    val contentWidth = 900.dp
                    
                    // Header
                    Row(
                        modifier = Modifier.width(contentWidth).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("Vch No.", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("Particulars", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                        items(filteredVouchers) { voucher ->
                            val partyName = ledgers.find { it.id == voucher.party_ledger_id }?.ledger_name ?: "Direct Entry"
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVoucherClick(voucher) },
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(voucher.date.toDisplayDate(), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(voucher.voucher_number ?: "-", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(partyName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(voucher.total_amount.format(), modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
