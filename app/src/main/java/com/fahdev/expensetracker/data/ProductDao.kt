package com.fahdev.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE means if product name exists, don't re-insert
    suspend fun insertProduct(product: Product): Long // Returns rowId or -1 if ignored

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun getProductByName(name: String): Product?

    @Query("SELECT COUNT(id) FROM products WHERE categoryId = :categoryId")
    suspend fun getProductCountForCategory(categoryId: Int): Int
}