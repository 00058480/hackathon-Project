package com.example.hackathonproject.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Transaction
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun observeBills(): Flow<List<BillWithDetails>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeBill(id: Long): Flow<BillWithDetails?>

    @Insert
    suspend fun insertBill(bill: BillEntity): Long

    @Insert
    suspend fun insertPeople(people: List<BillPersonEntity>)

    @Insert
    suspend fun insertSharedExpenses(expenses: List<BillSharedExpenseEntity>)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    /** Persists a bill and its children atomically, wiring child rows to the new bill id. */
    @Transaction
    suspend fun saveBill(
        bill: BillEntity,
        people: List<BillPersonEntity>,
        expenses: List<BillSharedExpenseEntity>
    ) {
        val billId = insertBill(bill)
        insertPeople(people.map { it.copy(billId = billId) })
        insertSharedExpenses(expenses.map { it.copy(billId = billId) })
    }
}
