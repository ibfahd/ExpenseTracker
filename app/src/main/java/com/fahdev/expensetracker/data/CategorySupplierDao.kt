package com.fahdev.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorySupplierDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: CategorySupplierCrossRef)

    // --- Category -> Suppliers ---

    @Query("DELETE FROM category_supplier_cross_ref WHERE categoryId = :categoryId")
    suspend fun deleteSuppliersForCategory(categoryId: Int)

    @Query("SELECT supplierId FROM category_supplier_cross_ref WHERE categoryId = :categoryId")
    fun getSupplierIdsForCategory(categoryId: Int): Flow<List<Int>>

    @Transaction
    suspend fun updateSuppliersForCategory(categoryId: Int, supplierIds: List<Int>) {
        deleteSuppliersForCategory(categoryId)
        supplierIds.forEach { supplierId ->
            insert(CategorySupplierCrossRef(categoryId = categoryId, supplierId = supplierId))
        }
    }

    // --- Supplier -> Categories ---

    @Query("DELETE FROM category_supplier_cross_ref WHERE supplierId = :supplierId")
    suspend fun deleteCategoriesForSupplier(supplierId: Int)

    @Query("SELECT categoryId FROM category_supplier_cross_ref WHERE supplierId = :supplierId")
    fun getCategoryIdsForSupplier(supplierId: Int): Flow<List<Int>>

    @Query(
        """
        SELECT c.* FROM categories c
        INNER JOIN category_supplier_cross_ref cs ON c.id = cs.categoryId
        WHERE cs.supplierId = :supplierId
        ORDER BY c.name ASC
        """
    )
    fun getCategoriesForSupplier(supplierId: Int): Flow<List<Category>>

    @Transaction
    suspend fun updateCategoriesForSupplier(supplierId: Int, categoryIds: List<Int>) {
        deleteCategoriesForSupplier(supplierId)
        categoryIds.forEach { categoryId ->
            insert(CategorySupplierCrossRef(categoryId = categoryId, supplierId = supplierId))
        }
    }
}
