package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntityRoom(
    @PrimaryKey val transactionId: String,
    val amount: Double,
    val currency: String,
    val description: String?,
    val merchant: String?,
    val bookingDate: String,
    val createdAt: String,
)
