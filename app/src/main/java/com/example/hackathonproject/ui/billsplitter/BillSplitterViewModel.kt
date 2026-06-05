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
import com.example.hackathonproject.domain.PersonItem
import com.example.hackathonproject.domain.ScannedItem
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
import kotlin.math.roundToInt

/** Editable state of the bill currently being split. Survives configuration changes. */
data class BillUiState(
    val totalBillAmount: String = "",
    val serviceChargePercent: String = "",
    val people: List<Person> = emptyList(),
    val sharedExpenses: List<SharedExpense> = emptyList(),
    val scannedItems: List<ScannedItem> = emptyList(),
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
    private var nextPersonItemId = 0
    private var nextScannedId = 0

    fun setTotalBillAmount(value: String) {
        if (value.isAmountInput()) _uiState.update { it.copy(totalBillAmount = value) }
    }

    fun setServiceCharge(value: String) {
        if (value.isAmountInput()) _uiState.update { it.copy(serviceChargePercent = value) }
    }

    // --- People -----------------------------------------------------------

    fun addPerson(name: String = "") = _uiState.update {
        it.copy(people = it.people + Person(nextPersonId++, name))
    }

    fun addSavedPeople(people: List<PersonEntity>) {
        if (people.isEmpty()) return
        val additions = people.map { Person(nextPersonId++, it.name) }
        _uiState.update { state -> state.copy(people = state.people + additions) }
    }

    fun updatePersonName(id: Int, name: String) = _uiState.update { state ->
        state.copy(people = state.people.map { if (it.id == id) it.copy(name = name) else it })
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

    // --- Per-person items -------------------------------------------------

    fun addPersonItem(personId: Int, name: String = "", amount: String = "", sourceItemId: Int? = null) {
        val item = PersonItem(nextPersonItemId++, name, amount, sourceItemId)
        _uiState.update { state ->
            state.copy(people = state.people.map { if (it.id == personId) it.copy(items = it.items + item) else it })
        }
    }

    /** Assigns one unit of a scanned item to a person, copying its name and unit price. */
    fun addScannedItemToPerson(personId: Int, scannedItemId: Int) {
        val scanned = _uiState.value.scannedItems.firstOrNull { it.id == scannedItemId } ?: return
        addPersonItem(personId, scanned.name, scanned.unitPrice.toString(), scanned.id)
    }

    fun updatePersonItemName(personId: Int, itemId: Int, name: String) = _uiState.update { state ->
        state.copy(people = state.people.map { person ->
            if (person.id != personId) person
            else person.copy(items = person.items.map { if (it.id == itemId) it.copy(name = name) else it })
        })
    }

    fun updatePersonItemAmount(personId: Int, itemId: Int, amount: String) {
        if (!amount.isAmountInput()) return
        _uiState.update { state ->
            state.copy(people = state.people.map { person ->
                if (person.id != personId) person
                else person.copy(items = person.items.map { if (it.id == itemId) it.copy(amount = amount) else it })
            })
        }
    }

    fun removePersonItem(personId: Int, itemId: Int) = _uiState.update { state ->
        state.copy(people = state.people.map { person ->
            if (person.id != personId) person
            else person.copy(items = person.items.filterNot { it.id == itemId })
        })
    }

    // --- Shared expenses --------------------------------------------------

    fun addSharedExpense() = _uiState.update {
        it.copy(sharedExpenses = it.sharedExpenses + SharedExpense(nextExpenseId++, "", "", emptySet()))
    }

    /**
     * Adds [units] of a scanned item as a SINGLE shared expense (e.g. 5 teas in one line),
     * expanded so participants can be chosen.
     */
    fun addSharedExpenseFromScanned(scannedItemId: Int, units: Int) {
        val scanned = _uiState.value.scannedItems.firstOrNull { it.id == scannedItemId } ?: return
        val count = units.coerceAtLeast(1)
        val amount = scanned.unitPrice * count
        val expense = SharedExpense(
            id = nextExpenseId++,
            name = scanned.name,
            amount = amount.toString(),
            participants = emptySet(),
            sourceItemId = scanned.id,
            sourceUnits = count
        )
        _uiState.update {
            it.copy(
                sharedExpenses = it.sharedExpenses + expense,
                expandedExpenses = it.expandedExpenses + expense.id
            )
        }
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

    /** Imports scanned receipt items into the pool, keeping their quantity and unit price. */
    fun addParsedItems(items: List<ParsedItem>) {
        if (items.isEmpty()) return
        val additions = items.map { parsed ->
            val quantity = parsed.quantity.coerceAtLeast(1)
            val unitPrice = (parsed.amount.toDouble() / quantity).roundToInt()
            ScannedItem(nextScannedId++, parsed.name, unitPrice, quantity)
        }
        _uiState.update { it.copy(scannedItems = it.scannedItems + additions) }
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
