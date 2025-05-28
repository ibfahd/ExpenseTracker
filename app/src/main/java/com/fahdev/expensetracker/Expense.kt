package com.fahdev.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val productId: Int, // Link to Product entity
    val supplierId: Int, // Link to Supplier entity
    val timestamp: Long = System.currentTimeMillis()
)