package com.example.hackathonproject

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.hackathonproject.ui.billsplitter.BillSplitterViewModel
import com.example.hackathonproject.ui.history.HistoryViewModel
import com.example.hackathonproject.ui.people.PeopleViewModel

/** Builds the app's ViewModels by pulling repositories from the [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            BillSplitterViewModel(
                billApp().container.billRepository,
                billApp().container.personRepository
            )
        }
        initializer {
            PeopleViewModel(billApp().container.personRepository)
        }
        initializer {
            HistoryViewModel(billApp().container.billRepository)
        }
    }
}

private fun CreationExtras.billApp(): BillApp = this[APPLICATION_KEY] as BillApp
