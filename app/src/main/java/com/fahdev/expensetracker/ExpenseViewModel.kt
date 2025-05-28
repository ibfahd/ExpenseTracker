package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine // Import combine
import java.util.Calendar // Import Calendar for date calculations
import java.util.concurrent.TimeUnit // Import TimeUnit for date calculations

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val expenseDao: ExpenseDao
    private val productDao: ProductDao
    private val supplierDao: SupplierDao
    private val categoryDao: CategoryDao

    // Exposed properties for UI to observe
    val allProducts: Flow<List<Product>>
    val allSuppliers: Flow<List<Supplier>>
    val allCategories: Flow<List<Category>>
    val totalMonthlyExpenses: Flow<Double?> // Still useful for a quick monthly summary

    // --- NEW: Filter States ---
    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: Flow<Long?> = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: Flow<Long?> = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: Flow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId: Flow<Int?> = _selectedSupplierId.asStateFlow()

    // NEW: Combined Flow for filtered expenses
    // This flow will react to changes in any of the filter states
    val filteredExpenses: Flow<List<ExpenseWithDetails>> = combine(
        _selectedStartDate,
        _selectedEndDate,
        _selectedCategoryId,
        _selectedSupplierId
    ) { startDate, endDate, categoryId, supplierId ->
        // When any filter state changes, this block is re-executed
        expenseDao.getFilteredExpensesWithDetails(startDate, endDate, categoryId, supplierId).first()
    }
    // --- END NEW: Filter States ---

    init {
        val database = AppDatabase.getDatabase(application)
        expenseDao = database.expenseDao()
        productDao = database.productDao()
        supplierDao = database.supplierDao()
        categoryDao = database.categoryDao()

        // Initialize Flow properties
        allProducts = productDao.getAllProducts()
        allSuppliers = supplierDao.getAllSuppliers()
        allCategories = categoryDao.getAllCategories()
        totalMonthlyExpenses = expenseDao.getTotalMonthlyExpenses(System.currentTimeMillis())

        // Pre-populate some common categories if the database is new/empty
        viewModelScope.launch {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                val initialCategories = listOf(
                    "Groceries", "Dairy", "Bakery", "Meat", "Fruits", "Vegetables",
                    "Drinks", "Snacks", "Household", "Personal Care", "Electronics",
                    "Clothing", "Transport", "Utilities", "Entertainment", "Shopping", "Other"
                )
                initialCategories.forEach { name ->
                    categoryDao.insertCategory(Category(name = name))
                }
            }
        }
    }

    // --- CRUD operations for Expenses ---
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }

    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> {
        return expenseDao.getExpenseWithDetailsById(id)
    }

    // --- CRUD operations for Products ---
    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun getProductByName(name: String): Product? {
        return productDao.getProductByName(name)
    }

    // --- CRUD operations for Suppliers ---
    suspend fun addSupplier(supplier: Supplier): Long {
        return supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierByName(name: String): Supplier? {
        return supplierDao.getSupplierByName(name)
    }

    // --- CRUD operations for Categories ---
    suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    // --- CRUD operations for Suppliers (continued) ---
    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            supplierDao.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            supplierDao.deleteSupplier(supplier)
        }
    }

    fun getSupplierById(id: Int): Flow<Supplier?> {
        return supplierDao.getSupplierById(id)
    }

    // --- NEW: Filter Control Functions ---

    /**
     * Sets the category filter. Pass null to clear the category filter.
     */
    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Sets the supplier filter. Pass null to clear the supplier filter.
     */
    fun setSupplierFilter(supplierId: Int?) {
        _selectedSupplierId.value = supplierId
    }

    /**
     * Sets a custom date range filter. Pass null for both to clear the date filter.
     */
    fun setCustomDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    /**
     * Sets predefined date range filters.
     * @param rangeType "ThisMonth", "Last7Days", "LastMonth", "ThisYear", "All"
     */
    fun setDateRangeFilter(rangeType: String) {
        val calendar = Calendar.getInstance()
        var startDate: Long? = null
        var endDate: Long? = System.currentTimeMillis() // Default end date to now

        when (rangeType) {
            "ThisMonth" -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "Last7Days" -> {
                startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            }
            "LastMonth" -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.MONTH, -1) // Go back one month
                calendar.set(Calendar.DAY_OF_MONTH, 1) // Set to the 1st of last month
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                val endOfLastMonth = Calendar.getInstance()
                endOfLastMonth.timeInMillis = System.currentTimeMillis()
                endOfLastMonth.set(Calendar.DAY_OF_MONTH, 1) // Set to 1st of current month
                endOfLastMonth.add(Calendar.DAY_OF_MONTH, -1) // Go back one day to get end of last month
                endOfLastMonth.set(Calendar.HOUR_OF_DAY, 23)
                endOfLastMonth.set(Calendar.MINUTE, 59)
                endOfLastMonth.set(Calendar.SECOND, 59)
                endOfLastMonth.set(Calendar.MILLISECOND, 999)
                endDate = endOfLastMonth.timeInMillis
            }
            "ThisYear" -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "All" -> {
                // Set both to null to get all expenses
                startDate = null
                endDate = null
            }
        }
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    /**
     * Clears all active filters.
     */
    fun resetFilters() {
        _selectedStartDate.value = null
        _selectedEndDate.value = null
        _selectedCategoryId.value = null
        _selectedSupplierId.value = null
    }

    // --- END NEW: Filter Control Functions ---
}