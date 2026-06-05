package com.example.hackathonproject.domain

/** A person taking part in the current (unsaved) bill being edited on screen. */
data class Person(
    val id: Int,
    val name: String,
    val items: List<PersonItem> = emptyList()
)

/** A single thing a person ordered — a named meal with its price. */
data class PersonItem(
    val id: Int,
    val name: String,
    val amount: String,
    /** The [ScannedItem] this came from, so a unit can be returned to the pool when removed. */
    val sourceItemId: Int? = null
)

/** An item recognised from a receipt, available to assign to people. */
data class ScannedItem(
    val id: Int,
    val name: String,
    val unitPrice: Int,
    val quantity: Int
)

/** A shared expense (tea, juice, ...) split equally between its [participants]. */
data class SharedExpense(
    val id: Int,
    val name: String,
    val amount: String,
    val participants: Set<Int>,
    /** The [ScannedItem] this came from, so units can be returned to the pool when removed. */
    val sourceItemId: Int? = null,
    /** How many pool units this expense represents (e.g. 5 teas in one shared line). */
    val sourceUnits: Int = 1
)

/** Per-person money breakdown for a bill. */
data class PersonBreakdown(
    val personalAmount: Double,
    val sharedAmount: Double,
    val serviceChargeAmount: Double,
    val total: Double
)

/** Units of [this] scanned item not yet assigned to a person or a shared expense. */
fun ScannedItem.remainingQuantity(
    people: List<Person>,
    sharedExpenses: List<SharedExpense>
): Int {
    val usedByPeople = people.sumOf { person -> person.items.count { it.sourceItemId == id } }
    val usedByShared = sharedExpenses.filter { it.sourceItemId == id }.sumOf { it.sourceUnits }
    return quantity - usedByPeople - usedByShared
}

private val amountRegex = Regex("^\\d+$")

/** True when [this] is a valid amount input — empty or digits only (no decimal point). */
fun String.isAmountInput(): Boolean = isEmpty() || matches(amountRegex)

/** Returns this set with [item] removed if present, otherwise added. */
fun <T> Set<T>.toggle(item: T): Set<T> = if (contains(item)) this - item else this + item
