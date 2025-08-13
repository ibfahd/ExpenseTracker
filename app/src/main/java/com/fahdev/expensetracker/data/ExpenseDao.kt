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
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpensesWithDetails(): Flow<List<ExpenseWithDetails>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', datetime(date / 1000, 'unixepoch')) = strftime('%Y-%m', datetime(:currentTimestamp / 1000, 'unixepoch'))")
    fun getTotalMonthlyExpenses(currentTimestamp: Long): Flow<Double?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        INNER JOIN categories c ON p.categoryId = c.id
        INNER JOIN suppliers s ON e.supplierId = s.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
        ORDER BY e.date DESC
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
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
    """)
    fun getTotalFilteredExpenses(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<Double?>

    // --- Reporting Queries ---

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpensesAllTime(): Flow<Double?>

    @Query("SELECT MIN(date) FROM expenses")
    fun getFirstExpenseDate(): Flow<Long?>

    @Query("SELECT COUNT(id) FROM expenses")
    fun getTotalTransactionCount(): Flow<Int?>

    @Query("""
        SELECT COUNT(e.id) FROM expenses e
        INNER JOIN products p ON e.productId = p.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
    """)
    fun getFilteredTransactionCount(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<Int?>

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

    // --- NEW Filtered Reporting Queries ---
    @Query("""
        SELECT c.name as categoryName, SUM(e.amount) as totalAmount
        FROM Expenses e
        INNER JOIN Products p ON e.productId = p.id
        INNER JOIN Categories c ON p.categoryId = c.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
        GROUP BY c.name
        ORDER BY totalAmount DESC
    """)
    fun getSpendingByCategoryFiltered(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<CategorySpending>>

    @Query("""
        SELECT s.name as supplierName, SUM(e.amount) as totalAmount
        FROM Expenses e
        INNER JOIN Products p ON e.productId = p.id
        INNER JOIN Suppliers s ON e.supplierId = s.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
        GROUP BY s.name
        ORDER BY totalAmount DESC
    """)
    fun getSpendingBySupplierFiltered(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<SupplierSpending>>


    // --- NEW, STABLE Product Detail Report Queries ---

    // Intermediate data class for the first step of the report
    data class ProductSpendingInfo(
        val productId: Int,
        val productName: String,
        val categoryId: Int,
        val categoryName: String,
        val totalAmountSpent: Double?
    )

    // Intermediate data class for the second step
    data class LowestPriceInfo(
        val amount: Double?,
        val supplierId: Int?
    )

    // Step 1: Get the total spending for each product in the given date range.
    @Query("""
        SELECT
            p.id as productId,
            p.name as productName,
            c.id as categoryId,
            c.name as categoryName,
            SUM(e.amount) as totalAmountSpent
        FROM Products p
        JOIN Categories c ON p.categoryId = c.id
        JOIN Expenses e ON e.productId = p.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:supplierId IS NULL OR e.supplierId = :supplierId)
        GROUP BY p.id, p.name, c.id, c.name
        ORDER BY c.name ASC, p.name ASC
    """)
    fun getProductSpendingReport(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<ProductSpendingInfo>>

    // Step 2: Get the lowest price details for a single product within the date range.
    @Query("""
        SELECT e.amount, e.supplierId
        FROM Expenses e
        WHERE e.productId = :productId
          AND (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
        ORDER BY e.amount ASC, e.date ASC
        LIMIT 1
    """)
    suspend fun getLowestPriceForProduct(productId: Int, startDate: Long?, endDate: Long?): LowestPriceInfo?

    @Query("SELECT COUNT(id) FROM expenses WHERE productId = :productId")
    suspend fun getExpenseCountForProduct(productId: Int): Int

    @Query("SELECT COUNT(id) FROM expenses WHERE supplierId = :supplierId")
    suspend fun getExpenseCountForSupplier(supplierId: Int): Int

    // --- Trend Analysis Queries ---
    data class TrendDataPoint(val timestamp: Long, val amount: Double)

    @Query("""
        SELECT date as timestamp, SUM(amount) as amount
        FROM expenses
        WHERE (:startDate IS NULL OR date >= :startDate) AND (:endDate IS NULL OR date <= :endDate)
        GROUP BY date / (24 * 60 * 60 * 1000) -- Group by day
        ORDER BY timestamp ASC
    """)
    fun getSpendingByDay(startDate: Long?, endDate: Long?): Flow<List<TrendDataPoint>>

    @Query("""
        SELECT (strftime('%s', date(date / 1000, 'unixepoch', 'weekday 0', '-6 days')) * 1000) as timestamp, SUM(amount) as amount
        FROM expenses
        WHERE (:startDate IS NULL OR date >= :startDate) AND (:endDate IS NULL OR date <= :endDate)
        GROUP BY strftime('%Y-%W', date / 1000, 'unixepoch')
        ORDER BY timestamp ASC
    """)
    fun getSpendingByWeek(startDate: Long?, endDate: Long?): Flow<List<TrendDataPoint>>

    @Query("""
        SELECT (strftime('%s', date(date / 1000, 'unixepoch', 'start of month')) * 1000) as timestamp, SUM(amount) as amount
        FROM expenses
        WHERE (:startDate IS NULL OR date >= :startDate) AND (:endDate IS NULL OR date <= :endDate)
        GROUP BY strftime('%Y-%m', date / 1000, 'unixepoch')
        ORDER BY timestamp ASC
    """)
    fun getSpendingByMonth(startDate: Long?, endDate: Long?): Flow<List<TrendDataPoint>>

    @Query("DELETE FROM expenses WHERE supplierId = :supplierId")
    suspend fun deleteExpensesBySupplierId(supplierId: Int)
}