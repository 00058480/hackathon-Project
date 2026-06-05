package com.example.hackathonproject.ui.billsplitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackathonproject.data.local.BillEntity
import com.example.hackathonproject.data.local.BillPersonEntity
import com.example.hackathonproject.data.local.BillSharedExpenseEntity
import com.example.hackathonproject.data.local.PersonEntity
import com.example.hackathonproject.data.repository.BillRepository
import com.example.hackathonproject.data.repository.PersonRepository
import com.example.hackathonproject.domain.BillCalculator
import com.example.hackathonproject.domain.ParsedItem
import com.example.hackathonproject.domain.Person
import com.example.hackathonproject.domain.SharedExpense
import com.example.hackathonproject.domain.isAmountInput
import com.example.hackathonproject.domain.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable state of the bill currently being split. Survives configuration changes. */
data class BillUiState(
    val totalBillAmount: String = "",
    val serviceChargePercent: String = "",
    val people: List<Person> = emptyList(),
    val sharedExpenses: List<SharedExpense> = emptyList(),
    val expandedPeople: Set<Int> = emptySet(),
    val expandedExpenses: Set<Int> = emptySet()
)

class BillSplitterViewModel(
    private val billRepository: BillRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillUiState())
    val uiState: StateFlow<BillUiState> = _uiState.asStateFlow()

    /** People saved in the address book, offered for quick-add. */
    val savedPeople: StateFlow<List<PersonEntity>> = personRepository.people
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var nextPersonId = 0
    private var nextExpenseId = 0

    fun setTotalBillAmount(value: String) {
        if (value.isAmountInput()) _uiState.update { it.copy(totalBillAmount = value) }
    }

    fun setServiceCharge(value: String) {
        if (value.isAmountInput()) _uiState.update { it.copy(serviceChargePercent = value) }
    }

    // --- People -----------------------------------------------------------

    fun addPerson(name: String = "") = _uiState.update {
        it.copy(people = it.people + Person(nextPersonId++, name, ""))
    }

    fun addSavedPeople(people: List<PersonEntity>) {
        if (people.isEmpty()) return
        val additions = people.map { Person(nextPersonId++, it.name, "") }
        _uiState.update { state -> state.copy(people = state.people + additions) }
    }

    fun updatePersonName(id: Int, name: String) = _uiState.update { state ->
        state.copy(people = state.people.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun updatePersonAmount(id: Int, amount: String) {
        if (!amount.isAmountInput()) return
        _uiState.update { state ->
            state.copy(people = state.people.map { if (it.id == id) it.copy(personalAmount = amount) else it })
        }
    }

    fun removePerson(id: Int) = _uiState.update { state ->
        state.copy(
            people = state.people.filterNot { it.id == id },
            sharedExpenses = state.sharedExpenses.map { it.copy(participants = it.participants - id) }
        )
    }

    fun togglePersonExpanded(id: Int) = _uiState.update {
        it.copy(expandedPeople = it.expandedPeople.toggle(id))
    }

    // --- Shared expenses --------------------------------------------------

    fun addSharedExpense() = _uiState.update {
        it.copy(sharedExpenses = it.sharedExpenses + SharedExpense(nextExpenseId++, "", "", emptySet()))
    }

    fun updateExpenseName(id: Int, name: String) = _uiState.update { state ->
        state.copy(sharedExpenses = state.sharedExpenses.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun updateExpenseAmount(id: Int, amount: String) {
        if (!amount.isAmountInput()) return
        _uiState.update { state ->
            state.copy(sharedExpenses = state.sharedExpenses.map { if (it.id == id) it.copy(amount = amount) else it })
        }
    }

    fun toggleExpenseParticipant(expenseId: Int, personId: Int, checked: Boolean) = _uiState.update { state ->
        state.copy(sharedExpenses = state.sharedExpenses.map { expense ->
            if (expense.id != expenseId) {
                expense
            } else {
                expense.copy(
                    participants = if (checked) expense.participants + personId else expense.participants - personId
                )
            }
        })
    }

    fun setAllParticipants(expenseId: Int, checked: Boolean) = _uiState.update { state ->
        val allIds = state.people.map { it.id }.toSet()
        state.copy(sharedExpenses = state.sharedExpenses.map { expense ->
            if (expense.id != expenseId) expense
            else expense.copy(participants = if (checked) allIds else emptySet())
        })
    }

    fun removeExpense(id: Int) = _uiState.update { state ->
        state.copy(sharedExpenses = state.sharedExpenses.filterNot { it.id == id })
    }

    fun toggleExpenseExpanded(id: Int) = _uiState.update {
        it.copy(expandedExpenses = it.expandedExpenses.toggle(id))
    }

    /** Imports scanned receipt items as shared expenses (participants chosen later). */
    fun addParsedItems(items: List<ParsedItem>) {
        if (items.isEmpty()) return
        val additions = items.map { SharedExpense(nextExpenseId++, it.name, it.amount.toString(), emptySet()) }
        _uiState.update { it.copy(sharedExpenses = it.sharedExpenses + additions) }
    }

    // --- Persistence ------------------------------------------------------

    /** Saves the current bill to history and remembers its people for next time. */
    fun saveBill(title: String, onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.people.isEmpty()) return
        val serviceCharge = state.serviceChargePercent.toDoubleOrNull() ?: 0.0
        val now = System.currentTimeMillis()

        val billPeople = state.people.mapIndexed { index, person ->
            val breakdown = BillCalculator.breakdown(person, state.sharedExpenses, serviceCharge)
            BillPersonEntity(
                billId = 0,
                name = person.name.ifBlank { "Человек ${index + 1}" },
                personalAmount = breakdown.personalAmount,
                sharedAmount = breakdown.sharedAmount,
                serviceChargeAmount = breakdown.serviceChargeAmount,
                total = breakdown.total
            )
        }
        val grandTotal = billPeople.sumOf { it.total }

        val expenses = state.sharedExpenses.mapIndexedNotNull { index, expense ->
            val amount = expense.amount.toDoubleOrNull() ?: 0.0
            if (amount <= 0.0) {
                null
            } else {
                BillSharedExpenseEntity(
                    billId = 0,
                    name = expense.name.ifBlank { "Общий расход ${index + 1}" },
                    amount = amount,
                    participantCount = expense.participants.size
                )
            }
        }

        val bill = BillEntity(
            title = title.ifBlank { "Чек" },
            totalAmount = state.totalBillAmount.toDoubleOrNull() ?: grandTotal,
            serviceChargePercent = serviceCharge,
            grandTotal = grandTotal,
            createdAt = now
        )

        viewModelScope.launch {
            billRepository.saveBill(bill, billPeople, expenses)
            state.people.forEach { personRepository.saveIfNew(it.name, now) }
            onSaved()
        }
    }

    fun reset() = _uiState.update { BillUiState() }
}
