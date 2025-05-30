package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // e.g., "Milk", "Bread", "Eggs"
    val categoryId: Int // Link to Category entity
)