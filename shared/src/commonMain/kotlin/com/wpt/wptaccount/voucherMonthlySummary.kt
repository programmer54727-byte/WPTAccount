package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

data class MonthlyVoucherData(
    val monthName: String,
    val monthInt: Int,
    var count: Int = 0,
    var totalAmount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherMonthlySummary(
    company: Company,
    voucherType: String,
    period: AccountPeriod,
    onMonthClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val months = listOf(
        "April", "May", "June", "July", "August", "September",
        "October", "November", "December", "January", "February", "March"
    )
    val monthSequence = listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3)

    var monthlyDataMap by remember {
        mutableStateOf(monthSequence.associateWith { m ->
            MonthlyVoucherData(months[monthSequence.indexOf(m)], m)
        })
    }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                val vouchers = supabase.from("vouchers").select {
                    filter { 
                        eq("company_id", company.id!!)
                        eq("voucher_type", voucherType)
                        gte("date", period.startDate)
                        lte("date", period.endDate)
                    }
                }.decodeList<Voucher>()

                val dataMap = monthSequence.associateWith { m ->
                    MonthlyVoucherData(months[monthSequence.indexOf(m)], m)
                }.toMutableMap()

                vouchers.forEach { voucher ->
                    val dateParts = voucher.date.split("-")
                    if (dateParts.size == 3) {
                        val month = dateParts[1].toInt()
                        val monthData = dataMap[month]
                        if (monthData != null) {
                            monthData.count++
                            monthData.totalAmount += voucher.total_amount
                        }
                    }
                }
                monthlyDataMap = dataMap
            } catch (e: Exception) {
                println("Error fetching voucher summary: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(voucherType, period) { fetchData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(voucherType, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Monthly Summary (${period.startDate.toDisplayDate()} - ${period.endDate.toDisplayDate()})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // Table Header
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Particulars", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Vch Count", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Total Value", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(thickness = 1.dp, color = Color.Black)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(monthSequence) { m ->
                        val data = monthlyDataMap[m]!!
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMonthClick(m) }
                                .padding(vertical = 4.dp),
                            color = Color.Transparent
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(data.monthName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                Text(data.count.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                Text(data.totalAmount.format(), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                    }
                    
                    // Grand Total
                    item {
                        val totalCount = monthlyDataMap.values.sumOf { it.count }
                        val grandTotal = monthlyDataMap.values.sumOf { it.totalAmount }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Grand Total", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(totalCount.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(grandTotal.format(), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
