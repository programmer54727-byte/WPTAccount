package com.wpt.wptaccount

import kotlin.math.roundToInt

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
