package com.example.hackathonproject.data.repository

import com.example.hackathonproject.data.local.BillDao
import com.example.hackathonproject.data.local.BillEntity
import com.example.hackathonproject.data.local.BillPersonEntity
import com.example.hackathonproject.data.local.BillSharedExpenseEntity
import com.example.hackathonproject.data.local.BillWithDetails
import kotlinx.coroutines.flow.Flow

class BillRepository(private val billDao: BillDao) {

    val bills: Flow<List<BillWithDetails>> = billDao.observeBills()

    fun bill(id: Long): Flow<BillWithDetails?> = billDao.observeBill(id)

    suspend fun saveBill(
        bill: BillEntity,
        people: List<BillPersonEntity>,
        expenses: List<BillSharedExpenseEntity>
    ) = billDao.saveBill(bill, people, expenses)

    suspend fun delete(bill: BillEntity) = billDao.deleteBill(bill)
}
