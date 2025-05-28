package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao: ExpenseDao
    private val productDao: ProductDao
    private val supplierDao: SupplierDao
    private val categoryDao: CategoryDao

    // Exposed properties for UI to observe
    val allProducts: Flow<List<Product>>
    val allSuppliers: Flow<List<Supplier>>
    val allCategories: Flow<List<Category>>
    val totalMonthlyExpenses: Flow<Double?>

    // Filter States
    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: Flow<Long?> = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: Flow<Long?> = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: Flow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId: Flow<Int?> = _selectedSupplierId.asStateFlow()

    // Refresh trigger for expense list
    private val _refreshTrigger = MutableStateFlow(0)

    // Combined Flow for filtered expenses
    @OptIn(ExperimentalCoroutinesApi::class) // Added for flatMapLatest
    val filteredExpenses: Flow<List<ExpenseWithDetails>> = combine(
        _selectedStartDate,
        _selectedEndDate,
        _selectedCategoryId,
        _selectedSupplierId,
        _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        // Return a data class or tuple to avoid intersection type
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseDao.getFilteredExpensesWithDetails(
            params.startDate,
            params.endDate,
            params.categoryId,
            params.supplierId
        )
    }

    // Helper data class for filter parameters
    private data class FilterParams(
        val startDate: Long?,
        val endDate: Long?,
        val categoryId: Int?,
        val supplierId: Int?
    )

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

        // Pre-populate categories if empty
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

    // CRUD operations for Expenses
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.insertExpense(expense)
            _refreshTrigger.value++ // Trigger list refresh
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
            _refreshTrigger.value++ // Trigger list refresh
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            _refreshTrigger.value++ // Trigger list refresh
        }
    }

    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> {
        return expenseDao.getExpenseWithDetailsById(id)
    }

    // CRUD operations for Products
    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun getProductByName(name: String): Product? {
        return productDao.getProductByName(name)
    }

    // CRUD operations for Suppliers
    suspend fun addSupplier(supplier: Supplier): Long {
        return supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierByName(name: String): Supplier? {
        return supplierDao.getSupplierByName(name)
    }

    // CRUD operations for Categories
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

    // CRUD operations for Suppliers (continued)
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

    // Filter Control Functions
    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSupplierFilter(supplierId: Int?) {
        _selectedSupplierId.value = supplierId
    }

    fun setCustomDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    fun setDateRangeFilter(rangeType: String) {
        val calendar = Calendar.getInstance()
        var startDate: Long? = null
        var endDate: Long? = System.currentTimeMillis()

        when (rangeType) {
            "ThisMonth" -> {
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
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis

                val endOfLastMonth = Calendar.getInstance()
                endOfLastMonth.set(Calendar.DAY_OF_MONTH, 1)
                endOfLastMonth.add(Calendar.DAY_OF_MONTH, -1)
                endOfLastMonth.set(Calendar.HOUR_OF_DAY, 23)
                endOfLastMonth.set(Calendar.MINUTE, 59)
                endOfLastMonth.set(Calendar.SECOND, 59)
                endOfLastMonth.set(Calendar.MILLISECOND, 999)
                endDate = endOfLastMonth.timeInMillis
            }
            "ThisYear" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "All" -> {
                startDate = null
                endDate = null
            }
        }
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    fun resetFilters() {
        _selectedStartDate.value = null
        _selectedEndDate.value = null
        _selectedCategoryId.value = null
        _selectedSupplierId.value = null
        _refreshTrigger.value++ // Trigger list refresh
    }
}