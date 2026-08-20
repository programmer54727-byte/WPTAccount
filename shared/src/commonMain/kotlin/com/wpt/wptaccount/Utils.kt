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
    return name.contains("asset") || 
           name.contains("expense") || 
           name.contains("cash") || 
           name.contains("bank") || 
           name.contains("purchase") || 
           name.contains("stock") || 
           name.contains("investments") || 
           name.contains("advances") || 
           name.contains("deposits")
}

fun Double.formatWithSign(isDebitNature: Boolean): String {
    val formatted = kotlin.math.abs(this).format(2)
    return if (isDebitNature) {
        if (this >= 0) "$formatted Dr" else "$formatted Cr"
    } else {
        if (this >= 0) "$formatted Cr" else "$formatted Dr"
    }
}
