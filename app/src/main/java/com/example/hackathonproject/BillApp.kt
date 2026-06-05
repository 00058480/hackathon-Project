package com.example.hackathonproject

import android.app.Application
import android.content.Context
import com.example.hackathonproject.data.local.AppDatabase
import com.example.hackathonproject.data.repository.BillRepository
import com.example.hackathonproject.data.repository.PersonRepository

class BillApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Tiny manual service locator — holds the database and repositories for the app. */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val personRepository = PersonRepository(database.personDao())
    val billRepository = BillRepository(database.billDao())
}
