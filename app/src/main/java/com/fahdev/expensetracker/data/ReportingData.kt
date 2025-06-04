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

data class ProductReportDetail(
    val productId: Int,
    val productName: String,
    val categoryId: Int,
    val categoryName: String,
    val totalAmountSpent: Double,
    val lowestTransactionAmount: Double, // The smallest amount recorded for this product in a single expense
    val cheapestSupplierName: String? // The supplier for that lowest transaction amount
)
