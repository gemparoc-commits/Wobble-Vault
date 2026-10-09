package com.wobble.vault.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val phCurrency: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("PH").build())

private val countFormat: NumberFormat =
    NumberFormat.getIntegerInstance(Locale.US)

private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun formatCurrency(value: Double?): String = phCurrency.format(value ?: 0.0)

fun formatCount(value: Long?): String = countFormat.format(value ?: 0L)

fun formatPHP(value: Double?): String = "PHP %.2f".format(Locale.US, value ?: 0.0)

fun formatSlashDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val raw = iso.substring(0, minOf(10, iso.length))
    val parts = raw.split("-")
    if (parts.size != 3) return raw
    return "${parts[1]}/${parts[2]}/${parts[0]}"
}

fun formatDateCreated(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val date = try {
        isoDateFormat.parse(iso.substring(0, minOf(10, iso.length)))
    } catch (_: Exception) {
        null
    } ?: return iso
    return java.text.DateFormat.getDateInstance().format(date)
}

fun todayISO(): String = isoDateFormat.format(Date())

fun parseISODate(iso: String?): Date? = try {
    if (iso.isNullOrBlank()) null else isoDateFormat.parse(iso.substring(0, minOf(10, iso.length)))
} catch (_: Exception) {
    null
}
