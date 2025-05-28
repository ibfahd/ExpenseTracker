package com.fahdev.expensetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import androidx.room.RewriteQueriesToDropUnusedColumns

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT
            e.id AS expense_id,
            e.amount AS expense_amount,
            e.productId AS expense_productId,
            e.supplierId AS expense_supplierId,
            e.timestamp AS expense_timestamp,
            p.id AS product_id,
            p.name AS product_name,
            p.categoryId AS product_categoryId,
            s.id AS supplier_id,
            s.name AS supplier_name
        FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN suppliers s ON e.supplierId = s.id
        ORDER BY e.timestamp DESC
    """)
    fun getAllExpensesWithDetails(): Flow<List<ExpenseWithDetails>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT
            e.id AS expense_id,
            e.amount AS expense_amount,
            e.productId AS expense_productId,
            e.supplierId AS expense_supplierId,
            e.timestamp AS expense_timestamp,
            p.id AS product_id,
            p.name AS product_name,
            p.categoryId AS product_categoryId,
            s.id AS supplier_id,
            s.name AS supplier_name
        FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN suppliers s ON e.supplierId = s.id
        WHERE e.id = :id
    """)
    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch')) = strftime('%Y-%m', datetime(:currentTimestamp / 1000, 'unixepoch'))")
    fun getTotalMonthlyExpenses(currentTimestamp: Long): Flow<Double?>

    @Transaction
    @RewriteQueriesToDropUnusedColumns // Keep this for consistency
    @Query("""
        SELECT
            e.id AS expense_id,
            e.amount AS expense_amount,
            e.productId AS expense_productId,
            e.supplierId AS expense_supplierId,
            e.timestamp AS expense_timestamp,
            p.id AS product_id,
            p.name AS product_name,
            p.categoryId AS product_categoryId,
            s.id AS supplier_id,
            s.name AS supplier_name
        FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN categories c ON p.categoryId = c.id -- Need categories for filtering by categoryName
        INNER JOIN suppliers s ON e.supplierId = s.id
        WHERE (:startDate IS NULL OR e.timestamp >= :startDate)
          AND (:endDate IS NULL OR e.timestamp <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
        ORDER BY e.timestamp DESC
    """)
    fun getFilteredExpensesWithDetails(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<List<ExpenseWithDetails>>
}