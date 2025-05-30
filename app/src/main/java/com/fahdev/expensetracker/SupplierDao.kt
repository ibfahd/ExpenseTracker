package com.fahdev.expensetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.fahdev.expensetracker.data.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE name = :name LIMIT 1")
    suspend fun getSupplierByName(name: String): Supplier?

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    // MODIFIED: Changed return type to Flow<Supplier?>
    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun getSupplierById(id: Int): Flow<Supplier?> // <<< CHANGED HERE
}