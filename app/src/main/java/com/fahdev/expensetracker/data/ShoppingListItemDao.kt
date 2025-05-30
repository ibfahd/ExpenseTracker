package com.fahdev.expensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListItemDao {
    @Insert
    suspend fun insert(item: ShoppingListItem)

    @Update
    suspend fun update(item: ShoppingListItem)

    @Delete
    suspend fun delete(item: ShoppingListItem)

    @Query("DELETE FROM ShoppingListItem WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Get all items for a specific supplier and shopping date (a unique shopping trip)
    @Query("SELECT * FROM ShoppingListItem WHERE supplierId = :supplierId AND shoppingDate = :shoppingDate ORDER BY productId ASC")
    fun getShoppingListItemsForTrip(supplierId: Int, shoppingDate: Long): Flow<List<ShoppingListItem>>

    // Get the latest shopping date for a given supplier to find the most recent list
    @Query("SELECT MAX(shoppingDate) FROM ShoppingListItem WHERE supplierId = :supplierId")
    suspend fun getLatestShoppingDateForSupplier(supplierId: Int): Long?

    // Get all items belonging to the latest trip for a supplier (used for "reusing" a list)
    @Query("SELECT * FROM ShoppingListItem WHERE supplierId = :supplierId AND shoppingDate = :latestDate")
    suspend fun getItemsForLatestTrip(supplierId: Int, latestDate: Long): List<ShoppingListItem>
}