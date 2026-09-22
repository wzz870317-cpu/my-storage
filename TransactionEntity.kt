package com.example.cashflow.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["uniqueKey"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val source: String,
    val type: String,
    val amountFen: Long,
    val merchant: String?,
    val title: String,
    val originalText: String,
    val transactionTime: Long,
    val uniqueKey: String,
    val category: String = "其他",
    val createdAt: Long = System.currentTimeMillis()
)