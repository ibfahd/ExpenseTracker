package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CategoryDao
import com.fahdev.expensetracker.data.CategorySpending
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ProductDao
import com.fahdev.expensetracker.data.ProductReportDetail
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.SupplierDao
import com.fahdev.expensetracker.data.SupplierSpending
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseDao: ExpenseDao = database.expenseDao()
    private val productDao: ProductDao = database.productDao()
    private val supplierDao: SupplierDao = database.supplierDao()
    private val categoryDao: CategoryDao = database.categoryDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: StateFlow<Long?> = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: StateFlow<Long?> = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId: StateFlow<Int?> = _selectedSupplierId.asStateFlow()

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
    val totalFilteredExpenses: StateFlow<Double> = combine(
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private data class FilterParams(
        val startDate: Long?,
        val endDate: Long?,
        val categoryId: Int?,
        val supplierId: Int?
    )

    val totalExpensesAllTime: StateFlow<Double> = expenseDao.getTotalExpensesAllTime()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val firstExpenseDate: StateFlow<Long?> = expenseDao.getFirstExpenseDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalTransactionCount: StateFlow<Int> = expenseDao.getTotalTransactionCount()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val spendingByCategory: StateFlow<List<CategorySpending>> = expenseDao.getSpendingByCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendingBySupplier: StateFlow<List<SupplierSpending>> = expenseDao.getSpendingBySupplier()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val averageDailyExpense: StateFlow<Double> = combine(totalExpensesAllTime, firstExpenseDate) { total, firstDateMs ->
        if (total > 0 && firstDateMs != null) {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstDateMs).coerceAtLeast(1)
            total / days
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageMonthlyExpense: StateFlow<Double> = combine(totalExpensesAllTime, firstExpenseDate) { total, firstDateMs ->
        if (total > 0 && firstDateMs != null) {
            val firstCal = Calendar.getInstance().apply { timeInMillis = firstDateMs }
            val currentCal = Calendar.getInstance()
            var months = (currentCal.get(Calendar.YEAR) - firstCal.get(Calendar.YEAR)) * 12
            months -= firstCal.get(Calendar.MONTH)
            months += currentCal.get(Calendar.MONTH)
            if (currentCal.get(Calendar.DAY_OF_MONTH) < firstCal.get(Calendar.DAY_OF_MONTH) && months > 0) {
                months--
            }
            total / max(1, months + 1)
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val productReportDetails: StateFlow<List<ProductReportDetail>> = combine(
        _selectedStartDate,
        _selectedEndDate,
        _refreshTrigger
    ) { startDate, endDate, _ ->
        Pair(startDate, endDate)
    }.flatMapLatest { (startDate, endDate) ->
        expenseDao.getProductReportDetails(startDate, endDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        viewModelScope.launch {
            if (allCategories.first().isEmpty()) {
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
            resetFilters() // Initialize filters to "All Time" and no category/supplier
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
            _refreshTrigger.value++
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
            _refreshTrigger.value++
        }
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            supplierDao.updateSupplier(supplier)
            _refreshTrigger.value++
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            supplierDao.deleteSupplier(supplier)
            _refreshTrigger.value++
        }
    }

    fun getSupplierById(id: Int): Flow<Supplier?> {
        return supplierDao.getSupplierById(id)
    }

    // Reverted to general names for use by both MainActivity and ReportingActivity (if applicable)
    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
        _refreshTrigger.value++ // Ensure dependent flows update
    }

    fun setSupplierFilter(supplierId: Int?) {
        _selectedSupplierId.value = supplierId
        _refreshTrigger.value++ // Ensure dependent flows update
    }

    fun setDateRangeFilter(rangeType: String) {
        val calendar = Calendar.getInstance()
        var startDate: Long? = null
        var endDate: Long? = System.currentTimeMillis()

        when (rangeType) {
            "ThisMonth" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "Last7Days" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "LastMonth" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
                val endOfLastMonth = Calendar.getInstance().apply {
                    timeInMillis = startDate
                    add(Calendar.MONTH, 1)
                    add(Calendar.DAY_OF_MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }
                endDate = endOfLastMonth.timeInMillis
            }
            "ThisYear" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "All" -> {
                startDate = null
                endDate = null
            }
        }
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
        _refreshTrigger.value++
    }
    fun setCustomDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
        _refreshTrigger.value++
    }

    // General reset for all filters, used by MainActivity and can be used by ReportingActivity
    fun resetFilters() {
        setDateRangeFilter("All") // This sets dates to null and triggers refresh
        _selectedCategoryId.value = null
        _selectedSupplierId.value = null
        // _refreshTrigger.value++ // setDateRangeFilter already triggers it
    }
}
