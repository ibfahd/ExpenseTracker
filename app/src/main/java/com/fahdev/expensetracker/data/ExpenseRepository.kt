package com.fahdev.expensetracker.data

import com.fahdev.expensetracker.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val categoryDao: CategoryDao,
    private val categorySupplierDao: CategorySupplierDao
) {

    // --- Data Flows ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    // --- Expense, Product, Supplier, Category Functions ---
    suspend fun addExpense(expense: Expense) { expenseDao.insertExpense(expense) }
    suspend fun updateExpense(expense: Expense) { expenseDao.updateExpense(expense) }
    suspend fun deleteExpense(expense: Expense) { expenseDao.deleteExpense(expense) }
    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> = expenseDao.getExpenseWithDetailsById(id)
    fun getFilteredExpensesWithDetails(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<ExpenseWithDetails>> = expenseDao.getFilteredExpensesWithDetails(startDate, endDate, categoryId, supplierId)
    fun getTotalFilteredExpenses(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<Double?> = expenseDao.getTotalFilteredExpenses(startDate, endDate, categoryId, supplierId)
    suspend fun addProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun getProductByName(name: String): Product? = productDao.getProductByName(name)
    fun getProductsForCategory(categoryId: Int): Flow<List<Product>> = productDao.getProductsForCategory(categoryId)
    suspend fun getProductByNameInCategory(name: String, categoryId: Int): Product? = productDao.getProductByNameInCategory(name, categoryId)
    suspend fun updateProduct(product: Product) { productDao.updateProduct(product) }
    suspend fun deleteProduct(product: Product) { productDao.deleteProduct(product) }
    suspend fun productHasExpenses(productId: Int): Boolean = expenseDao.getExpenseCountForProduct(productId) > 0
    suspend fun addSupplier(supplier: Supplier): Long = supplierDao.insertSupplier(supplier)
    suspend fun getSupplierByName(name: String): Supplier? = supplierDao.getSupplierByName(name)
    suspend fun updateSupplier(supplier: Supplier) { supplierDao.updateSupplier(supplier) }
    suspend fun deleteSupplier(supplier: Supplier) { supplierDao.deleteSupplier(supplier) }
    suspend fun supplierHasExpenses(supplierId: Int): Boolean = expenseDao.getExpenseCountForSupplier(supplierId) > 0
    suspend fun deleteSupplierAndExpenses(supplier: Supplier) { expenseDao.deleteExpensesBySupplierId(supplier.id); supplierDao.deleteSupplier(supplier) }
    fun getSupplierById(id: Int): Flow<Supplier?> = supplierDao.getSupplierById(id)
    suspend fun addCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)
    suspend fun hasProductsInCategory(categoryId: Int): Boolean = productDao.getProductCountForCategory(categoryId) > 0
    suspend fun updateCategory(category: Category) { categoryDao.updateCategory(category) }
    suspend fun deleteCategory(category: Category) { categoryDao.deleteCategory(category) }
    fun getCategoryById(id: Int): Flow<Category?> = categoryDao.getCategoryById(id)

    // --- Category-Supplier Relationship Functions ---
    fun getSupplierIdsForCategory(categoryId: Int): Flow<List<Int>> = categorySupplierDao.getSupplierIdsForCategory(categoryId)
    suspend fun updateSuppliersForCategory(categoryId: Int, supplierIds: List<Int>) = categorySupplierDao.updateSuppliersForCategory(categoryId, supplierIds)
    fun getCategoriesForSupplier(supplierId: Int): Flow<List<Category>> = categorySupplierDao.getCategoriesForSupplier(supplierId)
    fun getLinkedCategoryIds(supplierId: Int): Flow<List<Int>> = categorySupplierDao.getCategoryIdsForSupplier(supplierId)
    suspend fun saveCategoryLinksForSupplier(supplierId: Int, categoryIds: List<Int>) = categorySupplierDao.updateCategoriesForSupplier(supplierId, categoryIds)

    // --- Reporting data ---
    val totalExpensesAllTime: Flow<Double?> = expenseDao.getTotalExpensesAllTime()
    val firstExpenseDate: Flow<Long?> = expenseDao.getFirstExpenseDate()
    val totalTransactionCount: Flow<Int?> = expenseDao.getTotalTransactionCount()
    fun getFilteredTransactionCount(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<Int?> = expenseDao.getFilteredTransactionCount(startDate, endDate, categoryId, supplierId)
    val spendingByCategory: Flow<List<CategorySpending>> = expenseDao.getSpendingByCategory()
    val spendingBySupplier: Flow<List<SupplierSpending>> = expenseDao.getSpendingBySupplier()

    // --- Filtered Reporting Data ---
    fun getSpendingByCategoryFiltered(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<CategorySpending>> = expenseDao.getSpendingByCategoryFiltered(startDate, endDate, categoryId, supplierId)
    fun getSpendingBySupplierFiltered(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<SupplierSpending>> = expenseDao.getSpendingBySupplierFiltered(startDate, endDate, categoryId, supplierId)

    fun getProductSpendingReport(startDate: Long?, endDate: Long?, categoryId: Int?, supplierId: Int?): Flow<List<ExpenseDao.ProductSpendingInfo>> = expenseDao.getProductSpendingReport(startDate, endDate, categoryId, supplierId)
    suspend fun getLowestPriceForProduct(productId: Int, startDate: Long?, endDate: Long?): ExpenseDao.LowestPriceInfo? = expenseDao.getLowestPriceForProduct(productId, startDate, endDate)

    // --- Trend Analysis ---
    fun getSpendingByDay(startDate: Long?, endDate: Long?): Flow<List<ExpenseDao.TrendDataPoint>> = expenseDao.getSpendingByDay(startDate, endDate)
    fun getSpendingByWeek(startDate: Long?, endDate: Long?): Flow<List<ExpenseDao.TrendDataPoint>> = expenseDao.getSpendingByWeek(startDate, endDate)
    fun getSpendingByMonth(startDate: Long?, endDate: Long?): Flow<List<ExpenseDao.TrendDataPoint>> = expenseDao.getSpendingByMonth(startDate, endDate)
}
