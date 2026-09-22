package com.example.cashflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM transactions
        ORDER BY transactionTime DESC
    """)
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amountFen), 0)
        FROM transactions
        WHERE type = 'income'
          AND transactionTime >= :startTime
          AND transactionTime < :endTime
    """)
    fun observeIncome(startTime: Long, endTime: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountFen), 0)
        FROM transactions
        WHERE type = 'expense'
          AND transactionTime >= :startTime
          AND transactionTime < :endTime
    """)
    fun observeExpense(startTime: Long, endTime: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long
}