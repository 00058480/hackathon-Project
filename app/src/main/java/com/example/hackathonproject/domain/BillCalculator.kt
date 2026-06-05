package com.example.hackathonproject.domain

/** Single source of truth for how a bill is divided between people. */
object BillCalculator {

    /** Computes what [person] owes: their order, their share of common expenses, and service charge. */
    fun breakdown(
        person: Person,
        sharedExpenses: List<SharedExpense>,
        serviceChargePercent: Double
    ): PersonBreakdown {
        val personal = person.items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val shared = sharedExpenses.sumOf { expense ->
            if (expense.participants.contains(person.id) && expense.participants.isNotEmpty()) {
                (expense.amount.toDoubleOrNull() ?: 0.0) / expense.participants.size
            } else {
                0.0
            }
        }
        val subtotal = personal + shared
        val serviceCharge = subtotal * serviceChargePercent / 100.0
        return PersonBreakdown(
            personalAmount = personal,
            sharedAmount = shared,
            serviceChargeAmount = serviceCharge,
            total = subtotal + serviceCharge
        )
    }

    /** Sum of every person's total — what the whole table pays. */
    fun grandTotal(
        people: List<Person>,
        sharedExpenses: List<SharedExpense>,
        serviceChargePercent: Double
    ): Double = people.sumOf { breakdown(it, sharedExpenses, serviceChargePercent).total }
}
