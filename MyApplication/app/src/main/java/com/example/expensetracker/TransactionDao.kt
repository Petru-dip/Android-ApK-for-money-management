package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TransactionEntityRoom>)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    suspend fun all(): List<TransactionEntityRoom>
}
