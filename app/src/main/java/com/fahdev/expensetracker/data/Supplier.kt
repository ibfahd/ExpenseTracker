package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String // e.g., "Carrefour", "Local Market", "Amazon"
)