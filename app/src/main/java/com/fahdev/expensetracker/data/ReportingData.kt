package com.fahdev.expensetracker.data

// Data class to hold spending sum for a category
data class CategorySpending(
    val categoryName: String,
    val totalAmount: Double
)

// Data class to hold spending sum for a supplier
data class SupplierSpending(
    val supplierName: String,
    val totalAmount: Double
)
