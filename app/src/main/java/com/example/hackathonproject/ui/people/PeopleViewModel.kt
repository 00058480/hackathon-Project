package com.example.hackathonproject.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackathonproject.data.local.PersonEntity
import com.example.hackathonproject.data.repository.PersonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeopleViewModel(private val repository: PersonRepository) : ViewModel() {

    val people: StateFlow<List<PersonEntity>> = repository.people
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(person: PersonEntity) = viewModelScope.launch {
        repository.save(person)
    }

    fun delete(person: PersonEntity) = viewModelScope.launch {
        repository.delete(person)
    }
}
