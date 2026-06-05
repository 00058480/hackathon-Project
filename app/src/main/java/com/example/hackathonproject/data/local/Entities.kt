package com.example.hackathonproject.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A reusable person in the address book, so you don't retype names for every bill. */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/** A saved bill (history record). */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val serviceChargePercent: Double,
    val grandTotal: Double,
    val createdAt: Long
)

/** A snapshot of one person's share within a saved bill. */
@Entity(
    tableName = "bill_people",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("billId")]
)
data class BillPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val name: String,
    val personalAmount: Double,
    val sharedAmount: Double,
    val serviceChargeAmount: Double,
    val total: Double
)

/** A snapshot of one shared expense within a saved bill. */
@Entity(
    tableName = "bill_shared_expenses",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("billId")]
)
data class BillSharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val name: String,
    val amount: Double,
    val participantCount: Int
)

/** A saved bill together with its people and shared expenses. */
data class BillWithDetails(
    @Embedded val bill: BillEntity,
    @Relation(parentColumn = "id", entityColumn = "billId")
    val people: List<BillPersonEntity>,
    @Relation(parentColumn = "id", entityColumn = "billId")
    val sharedExpenses: List<BillSharedExpenseEntity>
)
