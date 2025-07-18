package com.fahdev.expensetracker.data

import com.fahdev.expensetracker.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository module for handling data operations.
 * This class abstracts the data sources (DAOs) from the ViewModels.
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val categoryDao: CategoryDao
) {

    // Product, Supplier, and Category flows
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    // Expense-related functions
    suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> {
        return expenseDao.getExpenseWithDetailsById(id)
    }

    fun getFilteredExpensesWithDetails(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<List<ExpenseWithDetails>> {
        return expenseDao.getFilteredExpensesWithDetails(startDate, endDate, categoryId, supplierId)
    }

    fun getTotalFilteredExpenses(
        startDate: Long?,
        endDate: Long?,
        categoryId: Int?,
        supplierId: Int?
    ): Flow<Double?> {
        return expenseDao.getTotalFilteredExpenses(startDate, endDate, categoryId, supplierId)
    }

    // Product-related functions
    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun getProductByName(name: String): Product? {
        return productDao.getProductByName(name)
    }

    fun getProductsForCategory(categoryId: Int): Flow<List<Product>> {
        return productDao.getProductsForCategory(categoryId)
    }

    suspend fun getProductByNameInCategory(name: String, categoryId: Int): Product? {
        return productDao.getProductByNameInCategory(name, categoryId)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun productHasExpenses(productId: Int): Boolean {
        return expenseDao.getExpenseCountForProduct(productId) > 0
    }

    // Supplier-related functions
    suspend fun addSupplier(supplier: Supplier): Long {
        return supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierByName(name: String): Supplier? {
        return supplierDao.getSupplierByName(name)
    }

    suspend fun updateSupplier(supplier: Supplier) {
        supplierDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        supplierDao.deleteSupplier(supplier)
    }

    suspend fun supplierHasExpenses(supplierId: Int): Boolean {
        return expenseDao.getExpenseCountForSupplier(supplierId) > 0
    }

    suspend fun deleteSupplierAndExpenses(supplier: Supplier) {
        expenseDao.deleteExpensesBySupplierId(supplier.id)
        supplierDao.deleteSupplier(supplier)
    }

    fun getSupplierById(id: Int): Flow<Supplier?> {
        return supplierDao.getSupplierById(id)
    }


    // Category-related functions
    suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun hasProductsInCategory(categoryId: Int): Boolean {
        return productDao.getProductCountForCategory(categoryId) > 0
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertInitialCategories(categories: List<Category>) {
        if (categoryDao.getAllCategories().first().isEmpty()) {
            categories.forEach { categoryDao.insertCategory(it) }
        }
    }

    // Reporting data
    val totalExpensesAllTime: Flow<Double?> = expenseDao.getTotalExpensesAllTime()
    val firstExpenseDate: Flow<Long?> = expenseDao.getFirstExpenseDate()
    val totalTransactionCount: Flow<Int?> = expenseDao.getTotalTransactionCount()
    val spendingByCategory: Flow<List<CategorySpending>> = expenseDao.getSpendingByCategory()
    val spendingBySupplier: Flow<List<SupplierSpending>> = expenseDao.getSpendingBySupplier()

    fun getProductSpendingReport(startDate: Long?, endDate: Long?): Flow<List<ExpenseDao.ProductSpendingInfo>> {
        return expenseDao.getProductSpendingReport(startDate, endDate)
    }

    suspend fun getLowestPriceForProduct(productId: Int, startDate: Long?, endDate: Long?): ExpenseDao.LowestPriceInfo? {
        return expenseDao.getLowestPriceForProduct(productId, startDate, endDate)
    }
}
