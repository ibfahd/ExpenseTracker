package com.fahdev.expensetracker

import androidx.room.Embedded
import androidx.room.Relation
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Product

data class ProductWithCategory(
    @Embedded val product: Product, // Embeds all fields from the Product entity
    @Relation(
        parentColumn = "categoryId", // Column in the Product entity
        entityColumn = "id"         // Column in the Category entity
    )
    val category: Category          // The related Category object
)