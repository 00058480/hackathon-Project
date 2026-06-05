package com.example.hackathonproject.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackathonproject.data.local.BillEntity
import com.example.hackathonproject.data.local.BillWithDetails
import com.example.hackathonproject.data.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: BillRepository) : ViewModel() {

    val bills: StateFlow<List<BillWithDetails>> = repository.bills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bill(id: Long): Flow<BillWithDetails?> = repository.bill(id)

    fun delete(bill: BillEntity) = viewModelScope.launch {
        repository.delete(bill)
    }
}
