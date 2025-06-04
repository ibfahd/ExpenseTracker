package com.fahdev.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import com.fahdev.expensetracker.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Transaction
    @RewriteQueriesToDropUnusedColumns // Keep this if it's helping with other queries
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesWithDetails(): Flow<List<ExpenseWithDetails>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?>

    // Existing query, might be useful for specific period totals
    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch')) = strftime('%Y-%m', datetime(:currentTimestamp / 1000, 'unixepoch'))")
    fun getTotalMonthlyExpenses(currentTimestamp: Long): Flow<Double?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH) // Keep if necessary
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN categories c ON p.categoryId = c.id
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

    @Query("""
        SELECT SUM(e.amount) FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN categories c ON p.categoryId = c.id
        INNER JOIN suppliers s ON e.supplierId = s.id
        WHERE (:startDate IS NULL OR e.timestamp >= :startDate)
          AND (:endDate IS NULL OR e.timestamp <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
    """)
    fun getTotalFilteredExpenses(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<Double?>

    // --- New Reporting Queries ---

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpensesAllTime(): Flow<Double?>

    @Query("SELECT MIN(timestamp) FROM expenses")
    fun getFirstExpenseDate(): Flow<Long?>

    @Query("SELECT COUNT(id) FROM expenses")
    fun getTotalTransactionCount(): Flow<Int?>

    @Query("""
        SELECT c.name as categoryName, SUM(e.amount) as totalAmount
        FROM Expenses e
        INNER JOIN Products p ON e.productId = p.id
        INNER JOIN Categories c ON p.categoryId = c.id
        GROUP BY c.name
        ORDER BY totalAmount DESC
    """)
    fun getSpendingByCategory(): Flow<List<CategorySpending>>

    @Query("""
        SELECT s.name as supplierName, SUM(e.amount) as totalAmount
        FROM Expenses e
        INNER JOIN Suppliers s ON e.supplierId = s.id
        GROUP BY s.name
        ORDER BY totalAmount DESC
    """)
    fun getSpendingBySupplier(): Flow<List<SupplierSpending>>
}
