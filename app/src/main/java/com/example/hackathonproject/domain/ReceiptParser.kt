package com.example.hackathonproject.domain

/** One line item recognised from a receipt. [amount] is the line total in whole tenge. */
data class ParsedItem(
    val name: String,
    val quantity: Int,
    val amount: Int
)

/**
 * Turns reconstructed receipt rows (each roughly "НАЗВАНИЕ КОЛ-ВО СУММА") into line items.
 *
 * Pure and Android-free so it can be unit-tested. Names may be imperfect (the on-device OCR
 * has no Cyrillic model), but the amount/quantity columns parse reliably.
 */
object ReceiptParser {

    // Non-breaking space (U+00A0), used by the receipt as a thousands separator.
    private val nbsp = Char(0xA0)

    // A money amount: grouped thousands ("7 800,00") or a plain run ("7800,00" / "170,00"),
    // always ending in a 2-digit fraction. Requiring the fraction avoids matching the qty column.
    private val amountRegex = Regex("(?:\\d{1,3}(?: \\d{3})+|\\d+)[.,]\\d{2}")
    private val trailingIntRegex = Regex("(\\d+)\\s*$")

    fun parse(rows: List<String>): List<ParsedItem> = rows.mapNotNull(::parseRow)

    private fun parseRow(row: String): ParsedItem? {
        val text = row.replace(nbsp, ' ').trim()
        if (text.isEmpty()) return null
        if (text.contains('%')) return null // skip the service-charge line

        val amountMatch = amountRegex.findAll(text).lastOrNull() ?: return null
        val amount = amountMatch.value
            .replace(" ", "")
            .substringBefore(',')
            .substringBefore('.')
            .toIntOrNull() ?: return null
        if (amount <= 0) return null

        val before = text.substring(0, amountMatch.range.first).trim()
        val qtyMatch = trailingIntRegex.find(before)
        val quantity = qtyMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val name = (qtyMatch?.let { before.substring(0, it.range.first) } ?: before).trim()

        return ParsedItem(name = name.ifBlank { "Позиция" }, quantity = quantity, amount = amount)
    }
}
