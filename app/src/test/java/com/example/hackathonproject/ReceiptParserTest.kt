package com.example.hackathonproject

import com.example.hackathonproject.domain.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun `parses name quantity and amount from a grouped row`() {
        val items = ReceiptParser.parse(listOf("ТУРЕЦКИЙ ЧАЙ ЧАЙНИК 6 7 800,00"))

        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("ТУРЕЦКИЙ ЧАЙ ЧАЙНИК", item.name)
        assertEquals(6, item.quantity)
        assertEquals(7800, item.amount)
    }

    @Test
    fun `does not let the quantity column bleed into the amount`() {
        // "5" is the qty, "6 000,00" is the line total — must not parse as 56000.
        val item = ReceiptParser.parse(listOf("КОЛА 1Л 5 6 000,00")).single()

        assertEquals("КОЛА 1Л", item.name)
        assertEquals(5, item.quantity)
        assertEquals(6000, item.amount)
    }

    @Test
    fun `defaults quantity to one when the column is absent`() {
        val item = ReceiptParser.parse(listOf("ЛЕПЕШКА 1ШТ 170,00")).single()

        assertEquals(1, item.quantity)
        assertEquals(170, item.amount)
    }

    @Test
    fun `skips the service-charge line and the header`() {
        val rows = listOf(
            "Наименование Кол-во Сумма",
            "Надбавка \"Обслуживание 10%\" +10 4 397,00"
        )

        assertTrue(ReceiptParser.parse(rows).isEmpty())
    }

    @Test
    fun `parses a representative receipt block`() {
        val rows = listOf(
            "ТУРЕЦКИЙ ЧАЙ ЧАЙНИК 6 7 800,00",
            "БЕЙТИ КЕБАБ 1 4 000,00",
            "КОЛА 1Л 5 6 000,00",
            "ЛЕПЕШКА 1ШТ 170,00",
            "Надбавка \"Обслуживание 10%\" 4 397,00"
        )

        val items = ReceiptParser.parse(rows)

        assertEquals(4, items.size)
        assertEquals(listOf(7800, 4000, 6000, 170), items.map { it.amount })
    }

    // --- Tolerance to OCR noise (these used to be dropped) -----------------

    @Test
    fun `captures grouped amount even when the fraction is missing`() {
        val item = ReceiptParser.parse(listOf("БЕЙТИ КЕБАБ 1 4 000")).single()
        assertEquals(4000, item.amount)
        assertEquals(1, item.quantity)
    }

    @Test
    fun `tolerates letter O in place of zero in the fraction`() {
        assertEquals(170, ReceiptParser.parse(listOf("ЛЕПЕШКА 1ШТ 170,О0")).single().amount)
        assertEquals(170, ReceiptParser.parse(listOf("ЛЕПЕШКА 1ШТ 170,Oo")).single().amount)
    }

    @Test
    fun `tolerates a semicolon used instead of a comma`() {
        assertEquals(2600, ReceiptParser.parse(listOf("СУП ЧЕЧЕВИЧНЫЙ 2 2 600;00")).single().amount)
    }

    @Test
    fun `tolerates doubled spaces in the thousands group`() {
        val item = ReceiptParser.parse(listOf("КОЛА 1Л 5 6  000,00")).single()
        assertEquals(6000, item.amount)
        assertEquals(5, item.quantity)
    }
}
