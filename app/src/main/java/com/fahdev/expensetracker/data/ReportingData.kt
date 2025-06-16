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

// Data class for the final, combined product report details.
// Nullable fields are essential to prevent crashes when data is missing.
data class ProductReportDetail(
    val productId: Int,
    val productName: String,
    val categoryId: Int,
    val categoryName: String,
    val totalAmountSpent: Double?,
    val lowestTransactionAmount: Double?,
    val cheapestSupplierName: String?
)
