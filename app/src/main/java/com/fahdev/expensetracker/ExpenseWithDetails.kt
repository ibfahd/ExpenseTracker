package com.fahdev.expensetracker

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithDetails(
    @Embedded(prefix = "expense_") val expense: Expense,
    @Relation(
        parentColumn = "expense_productId", // <<< CHANGE THIS! Was "productId"
        entityColumn = "id",
        entity = Product::class
    )
    val productWithCategory: ProductWithCategory,
    @Relation(
        parentColumn = "expense_supplierId", // <<< CHANGE THIS! Was "supplierId"
        entityColumn = "id"
    )
    val supplier: Supplier
)