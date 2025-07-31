package com.fahdev.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents the many-to-many relationship between Categories and Suppliers.
 * Each row in this table is a link between one category and one supplier.
 */
@Entity(
    tableName = "category_supplier_cross_ref",
    primaryKeys = ["categoryId", "supplierId"],
    indices = [Index(value = ["supplierId"])],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategorySupplierCrossRef(
    val categoryId: Int,
    val supplierId: Int
)
