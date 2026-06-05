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

    // A money amount, tolerant to OCR noise:
    //  - grouped thousands ("7 800") with an OPTIONAL 2-digit fraction, or
    //  - a plain number that MUST carry a 2-digit fraction (so the quantity column isn't matched).
    // The fraction separator may be . , or ; (OCR confuses them) and the fraction digits may
    // include the letter O/о, which OCR reads instead of 0.
    private val amountRegex = Regex(
        "(?:\\d{1,3}(?: \\d{3})+(?:[.,;][\\dOoОо]{2})?|\\d+[.,;][\\dOoОо]{2})"
    )
    private val trailingIntRegex = Regex("(\\d+)\\s*$")
    private val fractionTail = Regex("[.,;]\\d{2}$")
    private val whitespace = Regex("\\s+")

    fun parse(rows: List<String>): List<ParsedItem> = rows.mapNotNull(::parseRow)

    private fun parseRow(row: String): ParsedItem? {
        val text = row.replace(nbsp, ' ').replace(whitespace, " ").trim()
        if (text.isEmpty()) return null
        if (text.contains('%')) return null // skip the service-charge line

        val amountMatch = amountRegex.findAll(text).lastOrNull() ?: return null
        val amount = parseAmount(amountMatch.value) ?: return null
        if (amount <= 0) return null

        val before = text.substring(0, amountMatch.range.first).trim()
        val qtyMatch = trailingIntRegex.find(before)
        val quantity = qtyMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val name = (qtyMatch?.let { before.substring(0, it.range.first) } ?: before).trim()

        return ParsedItem(name = name.ifBlank { "Позиция" }, quantity = quantity, amount = amount)
    }

    private fun parseAmount(raw: String): Int? {
        val digits = raw
            .replace('O', '0').replace('o', '0').replace('О', '0').replace('о', '0')
            .replace(" ", "")
            .replace(fractionTail, "") // drop a trailing 2-digit fraction if present
        return digits.toIntOrNull()
    }
}
