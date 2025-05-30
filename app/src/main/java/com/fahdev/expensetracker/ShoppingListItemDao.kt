package com.fahdev.expensetracker

import androidx.room.*
import com.fahdev.expensetracker.data.ShoppingListItem
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

    @Query("SELECT * FROM ShoppingListItem")
    fun getAllShoppingListItems(): Flow<List<ShoppingListItem>>
}