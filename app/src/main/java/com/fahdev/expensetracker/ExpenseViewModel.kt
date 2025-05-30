package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CategoryDao
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ProductDao
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.SupplierDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao: ExpenseDao
    private val productDao: ProductDao
    private val supplierDao: SupplierDao
    private val categoryDao: CategoryDao

    val allProducts: Flow<List<Product>>
    val allSuppliers: Flow<List<Supplier>>
    val allCategories: Flow<List<Category>>

    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: Flow<Long?> = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: Flow<Long?> = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: Flow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId: Flow<Int?> = _selectedSupplierId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredExpenses: Flow<List<ExpenseWithDetails>> = combine(
        _selectedStartDate,
        _selectedEndDate,
        _selectedCategoryId,
        _selectedSupplierId,
        _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseDao.getFilteredExpensesWithDetails(
            params.startDate,
            params.endDate,
            params.categoryId,
            params.supplierId
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalFilteredExpenses: Flow<Double> = combine(
        _selectedStartDate,
        _selectedEndDate,
        _selectedCategoryId,
        _selectedSupplierId,
        _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseDao.getTotalFilteredExpenses(
            params.startDate,
            params.endDate,
            params.categoryId,
            params.supplierId
        )
    }.map { it ?: 0.0 }

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

        allProducts = productDao.getAllProducts()
        allSuppliers = supplierDao.getAllSuppliers()
        allCategories = categoryDao.getAllCategories()

        viewModelScope.launch {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                val initialCategories = listOf(
                    application.getString(R.string.category_food_drinks),
                    application.getString(R.string.category_housing),
                    application.getString(R.string.category_transportation),
                    application.getString(R.string.category_utilities),
                    application.getString(R.string.category_healthcare),
                    application.getString(R.string.category_personal_care),
                    application.getString(R.string.category_shopping),
                    application.getString(R.string.category_entertainment),
                    application.getString(R.string.category_travel),
                    application.getString(R.string.category_education),
                    application.getString(R.string.category_savings_investments),
                    application.getString(R.string.category_miscellaneous)
                )
                initialCategories.forEach { name ->
                    categoryDao.insertCategory(Category(name = name))
                }
            }
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.insertExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> {
        return expenseDao.getExpenseWithDetailsById(id)
    }

    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun getProductByName(name: String): Product? {
        return productDao.getProductByName(name)
    }

    suspend fun addSupplier(supplier: Supplier): Long {
        return supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierByName(name: String): Supplier? {
        return supplierDao.getSupplierByName(name)
    }

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
        _refreshTrigger.value++
    }
}