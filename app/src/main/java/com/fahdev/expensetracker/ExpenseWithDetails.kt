package com.fahdev.expensetracker

import androidx.room.Embedded
import androidx.room.Relation
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier

data class ExpenseWithDetails(
    @Embedded val expense: Expense, // Removed prefix to match query columns
    @Relation(
        parentColumn = "productId", // Match column from Expense entity
        entityColumn = "id",
        entity = Product::class
    )
    val productWithCategory: ProductWithCategory,
    @Relation(
        parentColumn = "supplierId", // Match column from Expense entity
        entityColumn = "id",
        entity = Supplier::class
    )
    val supplier: Supplier
)