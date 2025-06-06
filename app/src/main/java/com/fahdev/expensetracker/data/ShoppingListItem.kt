package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val unit: String?,
    val plannedQuantity: Double, // NEW: Stores the initially planned quantity
    val purchasedQuantity: Double, // RENAMED: Stores the actual purchased quantity for a trip (was 'quantity')
    val unitPrice: Double?,
    val supplierId: Int?,
    val shoppingDate: Long // Timestamp of the specific shopping trip
)
