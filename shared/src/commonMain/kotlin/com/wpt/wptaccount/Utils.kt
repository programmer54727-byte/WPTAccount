package com.wpt.wptaccount

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

val CompanySaver = Saver<Company?, String>(
    save = { company -> if (company != null) Json.encodeToString(company) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<Company>(value) else null }
)

val LedgerSaver = Saver<Ledger?, String>(
    save = { ledger -> if (ledger != null) Json.encodeToString(ledger) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<Ledger>(value) else null }
)

val StockItemSaver = Saver<StockItem?, String>(
    save = { item -> if (item != null) Json.encodeToString(item) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<StockItem>(value) else null }
)

val StockGroupSaver = Saver<StockGroup?, String>(
    save = { group -> if (group != null) Json.encodeToString(group) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<StockGroup>(value) else null }
)

val UnitOfMeasureSaver = Saver<UnitOfMeasure?, String>(
    save = { unit -> if (unit != null) Json.encodeToString(unit) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<UnitOfMeasure>(value) else null }
)

val GroupSaver = Saver<AccountingGroup?, String>(
    save = { group -> if (group != null) Json.encodeToString(group) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<AccountingGroup>(value) else null }
)

val VoucherSaver = Saver<Voucher?, String>(
    save = { voucher -> if (voucher != null) Json.encodeToString(voucher) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<Voucher>(value) else null }
)

val PeriodSaver = Saver<AccountPeriod?, String>(
    save = { period -> if (period != null) Json.encodeToString(period) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<AccountPeriod>(value) else null }
)

val GstDetailsSaver = Saver<GstDetails?, String>(
    save = { details -> if (details != null) Json.encodeToString(details) else "" },
    restore = { value -> if (value.isNotEmpty()) Json.decodeFromString<GstDetails>(value) else null }
)

fun Double.format(digits: Int = 2): String {
    var res = 1L
    repeat(digits) { res *= 10 }
    val factor = res.toDouble()
    val rounded = (this * factor).roundToInt() / factor
    val s = rounded.toString()
    if (digits == 0) return s.split(".")[0]
    val parts = s.split(".")
    val integral = parts[0]
    val decimal = if (parts.size > 1) parts[1] else ""
    return if (decimal.length < digits) {
        "$integral.${decimal.padEnd(digits, '0')}"
    } else {
        "$integral.${decimal.substring(0, digits)}"
    }
}

fun isDebitNature(groupName: String): Boolean {
    val name = groupName.lowercase()
    
    // Check for explicit markers first
    if (name.contains("(asset)")) return true
    if (name.contains("(liability)")) return false

    // Explicit exclusions for Credit nature groups (Liabilities/Income)
    if (name.contains("creditor") || 
        name.contains("provision") || 
        name.contains("reserve") || 
        name.contains("capital") || 
        name.contains("sales") || 
        name.contains("income") || 
        name.contains("duty") || 
        name.contains("tax") ||
        name.contains("loan")) return false

    // Explicit inclusions for Debit nature groups (Assets/Expenses)
    return name.contains("asset") || 
           name.contains("expense") || 
           name.contains("cash") || 
           name.contains("bank") || 
           name.contains("purchase") || 
           name.contains("stock") || 
           name.contains("investment") || 
           name.contains("advance") || 
           name.contains("deposit") ||
           name.contains("debtor") ||
           name.contains("branch") ||
           name.contains("division")
}

fun Double.formatWithSign(): String {
    val formatted = kotlin.math.abs(this).format(2)
    // Professional Signed Balance Logic:
    // Positive values are Debit, Negative values are Credit.
    return if (this >= 0) "$formatted Dr" else "$formatted Cr"
}

/**
 * Standardizes date formatting across the application.
 * Internal (DB) Format: YYYY-MM-DD
 * Display (UI) Format: DD/MM/YYYY
 */

fun String.toDisplayDate(): String {
    return try {
        val parts = this.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else this
    } catch (e: Exception) { this }
}

fun String.toDbDate(): String {
    return try {
        // Handle both / and - as separators for user input
        val parts = if (this.contains("/")) this.split("/") else this.split("-")
        if (parts.size == 3) {
            val d = parts[0].padStart(2, '0')
            val m = parts[1].padStart(2, '0')
            val y = parts[2]
            "$y-$m-$d"
        } else this
    } catch (e: Exception) { this }
}

fun String.toMonthYearLabel(): String {
    return try {
        val parts = if (this.contains("/")) this.split("/") else this.split("-")
        // Assumes index 1 is month, index 2 is year in DD/MM/YYYY or index 0 is year, 1 is month in YYYY-MM-DD
        val month = if (this.contains("/")) parts[1] else parts[1]
        val year = if (this.contains("/")) parts[2] else parts[0]
        val monthName = when (month.toIntOrNull()) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> month
        }
        "$monthName $year"
    } catch (e: Exception) { this }
}

fun Company.getDefaultPeriod(): AccountPeriod {
    val start = financial_year_beginning ?: "2024-04-01"
    val parts = start.split("-")
    if (parts.size == 3) {
        val startYear = parts[0].toInt()
        val nextYear = startYear + 1
        return AccountPeriod(start, "$nextYear-03-31")
    }
    return AccountPeriod("2024-04-01", "2025-03-31")
}
