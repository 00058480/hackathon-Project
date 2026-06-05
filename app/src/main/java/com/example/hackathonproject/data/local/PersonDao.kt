package com.example.hackathonproject.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): PersonEntity?

    @Upsert
    suspend fun upsert(person: PersonEntity): Long

    @Delete
    suspend fun delete(person: PersonEntity)
}
