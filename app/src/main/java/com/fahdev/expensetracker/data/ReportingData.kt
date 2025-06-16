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
    // Changed to nullable Double to prevent crashes if SUM is null
    val totalAmountSpent: Double?,
    // Changed to nullable Double to prevent crashes if LEFT JOIN results in null
    val lowestTransactionAmount: Double?,
    val cheapestSupplierName: String?
)
