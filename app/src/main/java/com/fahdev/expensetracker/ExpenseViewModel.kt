package com.fahdev.expensetracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahdev.expensetracker.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // --- Data Flows for UI ---
    val allProducts: Flow<List<Product>> = expenseRepository.allProducts
    val allSuppliers: Flow<List<Supplier>> = expenseRepository.allSuppliers
    val allCategories: Flow<List<Category>> = expenseRepository.allCategories

    // --- Filtering State for Main Screen ---
    private val _selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedStartDate: StateFlow<Long?> = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow<Long?>(null)
    val selectedEndDate: StateFlow<Long?> = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId: StateFlow<Int?> = _selectedSupplierId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private data class FilterParams(
        val startDate: Long?,
        val endDate: Long?,
        val categoryId: Int?,
        val supplierId: Int?
    )

    // --- Combined Flows for Filtered Data ---
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
        expenseRepository.getFilteredExpensesWithDetails(
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
        expenseRepository.getTotalFilteredExpenses(
            params.startDate,
            params.endDate,
            params.categoryId,
            params.supplierId
        )
    }.map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    // --- Reporting Data ---
    val totalExpensesAllTime: StateFlow<Double> = expenseRepository.totalExpensesAllTime
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val firstExpenseDate: StateFlow<Long?> = expenseRepository.firstExpenseDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalTransactionCount: StateFlow<Int> = expenseRepository.totalTransactionCount
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val spendingByCategory: StateFlow<List<CategorySpending>> = expenseRepository.spendingByCategory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendingBySupplier: StateFlow<List<SupplierSpending>> = expenseRepository.spendingBySupplier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val averageDailyExpense: StateFlow<Double> = combine(totalExpensesAllTime, firstExpenseDate) { total, firstDateMs ->
        if (total > 0 && firstDateMs != null) {
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstDateMs).coerceAtLeast(1)
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
        expenseRepository.getProductSpendingReport(startDate, endDate).map { spendingList ->
            spendingList.map { spendingInfo ->
                val lowestPriceInfo = expenseRepository.getLowestPriceForProduct(spendingInfo.productId, startDate, endDate)
                val cheapestSupplierName = lowestPriceInfo?.supplierId?.let { id ->
                    expenseRepository.getSupplierById(id).first()?.name
                }
                ProductReportDetail(
                    productId = spendingInfo.productId,
                    productName = spendingInfo.productName,
                    categoryId = spendingInfo.categoryId,
                    categoryName = spendingInfo.categoryName,
                    totalAmountSpent = spendingInfo.totalAmountSpent,
                    lowestTransactionAmount = lowestPriceInfo?.amount,
                    cheapestSupplierName = cheapestSupplierName
                )
            }
        }
    }.catch { e ->
        Log.e("ExpenseViewModel", "Error fetching product report details", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- State and Logic for the NEW Add Expense Screen Flow ---
    private val _selectedSupplierForAdd = MutableStateFlow<Supplier?>(null)
    val selectedSupplierForAdd: StateFlow<Supplier?> = _selectedSupplierForAdd.asStateFlow()

    private val _selectedCategoryForAdd = MutableStateFlow<Category?>(null)
    val selectedCategoryForAdd: StateFlow<Category?> = _selectedCategoryForAdd.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsForAddScreen: StateFlow<List<Product>> = _selectedCategoryForAdd
        .flatMapLatest { category ->
            if (category != null) {
                expenseRepository.getProductsForCategory(category.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSupplierSelected(supplier: Supplier) {
        if (_selectedSupplierForAdd.value?.id == supplier.id) {
            _selectedSupplierForAdd.value = null
            _selectedCategoryForAdd.value = null
        } else {
            _selectedSupplierForAdd.value = supplier
            _selectedCategoryForAdd.value = null
        }
    }

    fun onCategorySelected(category: Category) {
        if (_selectedCategoryForAdd.value?.id == category.id) {
            _selectedCategoryForAdd.value = null
        } else {
            _selectedCategoryForAdd.value = category
        }
    }

    fun resetAddExpenseFlow() {
        _selectedSupplierForAdd.value = null
        _selectedCategoryForAdd.value = null
    }


    // --- ViewModel Initialization ---
    init {
        viewModelScope.launch {
            val defaultFilter = userPreferencesRepository.homeScreenDefaultFilter.first()
            setDateRangeFilter(defaultFilter)
        }
    }

    // --- Public Functions to Modify Data (delegated to repository) ---
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            _refreshTrigger.value++
        }
    }

    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> {
        return expenseRepository.getExpenseWithDetailsById(id)
    }

    suspend fun addProduct(product: Product): Long {
        return expenseRepository.addProduct(product)
    }

    suspend fun getProductByName(name: String): Product? {
        return expenseRepository.getProductByName(name)
    }

    suspend fun addSupplier(supplier: Supplier): Long {
        return expenseRepository.addSupplier(supplier)
    }

    suspend fun getSupplierByName(name: String): Supplier? {
        return expenseRepository.getSupplierByName(name)
    }

    suspend fun addCategory(category: Category): Long {
        return expenseRepository.addCategory(category)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return expenseRepository.getCategoryByName(name)
    }

    suspend fun hasProductsInCategory(categoryId: Int): Boolean {
        return expenseRepository.hasProductsInCategory(categoryId)
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            expenseRepository.updateCategory(category)
            _refreshTrigger.value++
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            expenseRepository.deleteCategory(category)
            _refreshTrigger.value++
        }
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return expenseRepository.getCategoryById(id)
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            expenseRepository.updateSupplier(supplier)
            _refreshTrigger.value++
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            expenseRepository.deleteSupplier(supplier)
            _refreshTrigger.value++
        }
    }

    fun getSupplierById(id: Int): Flow<Supplier?> {
        return expenseRepository.getSupplierById(id)
    }

    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSupplierFilter(supplierId: Int?) {
        _selectedSupplierId.value = supplierId
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
    }
    fun setCustomDateRangeFilter(startDate: Long?, endDate: Long?) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    fun resetFilters() {
        setDateRangeFilter("All")
        _selectedCategoryId.value = null
        _selectedSupplierId.value = null
    }

    fun getProductsForCategory(categoryId: Int): Flow<List<Product>> {
        return expenseRepository.getProductsForCategory(categoryId)
    }

    suspend fun getProductByNameInCategory(name: String, categoryId: Int): Product? {
        return expenseRepository.getProductByNameInCategory(name, categoryId)
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            expenseRepository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            expenseRepository.deleteProduct(product)
        }
    }

    suspend fun productHasExpenses(productId: Int): Boolean {
        return expenseRepository.productHasExpenses(productId)
    }

    suspend fun supplierHasExpenses(supplierId: Int): Boolean {
        return expenseRepository.supplierHasExpenses(supplierId)
    }

    fun deleteSupplierAndExpenses(supplier: Supplier) {
        viewModelScope.launch {
            expenseRepository.deleteSupplierAndExpenses(supplier)
            _refreshTrigger.value++
        }
    }
}
