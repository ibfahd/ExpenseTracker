package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // <<< ADD THIS IMPORT
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    // It's best practice to declare foreign keys here
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT // Prevents deleting a product if expenses are linked to it
        ),
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT // Prevents deleting a supplier if expenses are linked to it
        )
    ],
    // Add indices to the foreign key columns for performance
    indices = [
        Index(value = ["productId"]),
        Index(value = ["supplierId"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val productId: Int,
    val supplierId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
