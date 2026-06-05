package com.example.hackathonproject.domain

/** A person taking part in the current (unsaved) bill being edited on screen. */
data class Person(
    val id: Int,
    val name: String,
    val personalAmount: String
)

/** A shared expense (tea, juice, ...) split equally between its [participants]. */
data class SharedExpense(
    val id: Int,
    val name: String,
    val amount: String,
    val participants: Set<Int>
)

/** Per-person money breakdown for a bill. */
data class PersonBreakdown(
    val personalAmount: Double,
    val sharedAmount: Double,
    val serviceChargeAmount: Double,
    val total: Double
)

private val amountRegex = Regex("^\\d+$")

/** True when [this] is a valid amount input — empty or digits only (no decimal point). */
fun String.isAmountInput(): Boolean = isEmpty() || matches(amountRegex)

/** Returns this set with [item] removed if present, otherwise added. */
fun <T> Set<T>.toggle(item: T): Set<T> = if (contains(item)) this - item else this + item
