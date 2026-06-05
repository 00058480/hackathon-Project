package com.example.hackathonproject.data.repository

import com.example.hackathonproject.data.local.PersonDao
import com.example.hackathonproject.data.local.PersonEntity
import kotlinx.coroutines.flow.Flow

class PersonRepository(private val personDao: PersonDao) {

    val people: Flow<List<PersonEntity>> = personDao.observeAll()

    suspend fun save(person: PersonEntity): Long = personDao.upsert(person)

    suspend fun delete(person: PersonEntity) = personDao.delete(person)

    /** Stores a name in the address book if it isn't already there (case-insensitive). */
    suspend fun saveIfNew(name: String, createdAt: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (personDao.findByName(trimmed) == null) {
            personDao.upsert(PersonEntity(name = trimmed, createdAt = createdAt))
        }
    }
}
