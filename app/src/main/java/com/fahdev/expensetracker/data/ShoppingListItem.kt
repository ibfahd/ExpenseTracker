package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // <<< ADD THIS IMPORT
import androidx.room.PrimaryKey

@Entity(
    tableName = "ShoppingListItem",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Add indices to the foreign key columns for performance
    indices = [
        Index(value = ["productId"]),
        Index(value = ["supplierId"])
    ]
)
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val unit: String?,
    val plannedQuantity: Double,
    val purchasedQuantity: Double,
    val unitPrice: Double?,
    val supplierId: Int?,
    val shoppingDate: Long
)
