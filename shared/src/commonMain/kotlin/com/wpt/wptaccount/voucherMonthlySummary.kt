package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val accentColor = when(voucherType) {
        "Sale" -> Color(0xFF1E88E5)
        "Purchase" -> Color(0xFF7E57C2)
        "Payment" -> Color(0xFF546E7A)
        "Receipt" -> Color(0xFFE53935)
        "Contra" -> Color(0xFF00897B)
        "Journal" -> Color(0xFFFFB300)
        else -> Color(0xFF7C4DFF)
    }

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
                    if (voucher.date < period.startDate || voucher.date > period.endDate) return@forEach
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
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Column {
                        Text(
                            text = "$voucherType Monthly Summary", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = "FY ${period.startDate.toDisplayDate().takeLast(4)}-${period.endDate.toDisplayDate().takeLast(2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF49454F)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF7C4DFF))
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C4DFF))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Particulars", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Text("Vch Count", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Text("Total Value", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(monthSequence) { m ->
                        val data = monthlyDataMap[m]!!
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { onMonthClick(m) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left accent strip
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(accentColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(data.monthName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                    Text(data.count.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                    Text(data.totalAmount.format(), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D1B20))
                                }
                            }
                        }
                    }
                    
                    // Grand Total Row
                    item {
                        val totalCount = monthlyDataMap.values.sumOf { it.count }
                        val grandTotal = monthlyDataMap.values.sumOf { it.totalAmount }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3F8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D1B20))
                                Text(totalCount.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                Text(grandTotal.format(), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
