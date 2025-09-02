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

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

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

    private data class FilterParams(val startDate: Long?, val endDate: Long?, val categoryId: Int?, val supplierId: Int?)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredExpenses: Flow<List<ExpenseWithDetails>> = combine(_selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger) { startDate, endDate, categoryId, supplierId, _ -> FilterParams(startDate, endDate, categoryId, supplierId) }.flatMapLatest { params -> expenseRepository.getFilteredExpensesWithDetails(params.startDate, params.endDate, params.categoryId, params.supplierId) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalFilteredExpenses: StateFlow<Double> = combine(_selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger) { startDate, endDate, categoryId, supplierId, _ -> FilterParams(startDate, endDate, categoryId, supplierId) }.flatMapLatest { params -> expenseRepository.getTotalFilteredExpenses(params.startDate, params.endDate, params.categoryId, params.supplierId) }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalTransactionCount: StateFlow<Int> = expenseRepository.totalTransactionCount.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val productReportDetails: StateFlow<List<ProductReportDetail>> = combine(
        _selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseRepository.getProductSpendingReport(params.startDate, params.endDate, params.categoryId, params.supplierId)
            .map { spendingList ->
                spendingList.map { spendingInfo ->
                    val lowestPriceInfo = expenseRepository.getLowestPriceForProduct(spendingInfo.productId, params.startDate, params.endDate)
                    val cheapestSupplierName = lowestPriceInfo?.supplierId?.let { id -> expenseRepository.getSupplierById(id).first()?.name }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val spendingByCategoryFiltered: StateFlow<List<CategorySpending>> = combine(
        _selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseRepository.getSpendingByCategoryFiltered(params.startDate, params.endDate, params.categoryId, params.supplierId)
    }.catch { e ->
        Log.e("ExpenseViewModel", "Error fetching filtered spending by category", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val spendingBySupplierFiltered: StateFlow<List<SupplierSpending>> = combine(
        _selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger
    ) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseRepository.getSpendingBySupplierFiltered(params.startDate, params.endDate, params.categoryId, params.supplierId)
    }.catch { e ->
        Log.e("ExpenseViewModel", "Error fetching filtered spending by supplier", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Chart-specific data flows for the reporting charts tab ---
    /**
     * Monthly spending trend data for line charts
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val spendingByMonth: Flow<List<ExpenseDao.TrendDataPoint>> = combine(
        _selectedStartDate,
        _selectedEndDate
    ) { startDate, endDate ->
        expenseRepository.getSpendingByMonth(startDate, endDate)
    }.flatMapLatest { it }

    /**
     * Daily spending trend data
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val spendingByDay: Flow<List<ExpenseDao.TrendDataPoint>> = combine(
        _selectedStartDate,
        _selectedEndDate
    ) { startDate, endDate ->
        expenseRepository.getSpendingByDay(startDate, endDate)
    }.flatMapLatest { it }


    // --- State and Logic for the Add Expense Screen Flow ---
    private val _selectedSupplierForAdd = MutableStateFlow<Supplier?>(null)
    val selectedSupplierForAdd: StateFlow<Supplier?> = _selectedSupplierForAdd.asStateFlow()
    private val _selectedCategoryForAdd = MutableStateFlow<Category?>(null)
    val selectedCategoryForAdd: StateFlow<Category?> = _selectedCategoryForAdd.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesForSelectedSupplier: StateFlow<List<Category>> = _selectedSupplierForAdd
        .flatMapLatest { supplier ->
            if (supplier != null) {
                expenseRepository.getCategoriesForSupplier(supplier.id)
            } else {
                expenseRepository.allCategories
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- Functions for Managing Category-Supplier Links ---
    fun getLinkedSupplierIds(categoryId: Int): Flow<List<Int>> = expenseRepository.getSupplierIdsForCategory(categoryId)
    fun saveSupplierLinksForCategory(categoryId: Int, supplierIds: List<Int>) {
        viewModelScope.launch {
            expenseRepository.updateSuppliersForCategory(categoryId, supplierIds)
        }
    }
    fun getLinkedCategoryIds(supplierId: Int): Flow<List<Int>> = expenseRepository.getLinkedCategoryIds(supplierId)
    fun saveCategoryLinksForSupplier(supplierId: Int, categoryIds: List<Int>) {
        viewModelScope.launch {
            expenseRepository.saveCategoryLinksForSupplier(supplierId, categoryIds)
        }
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // --- ViewModel Initialization ---
    init {
        viewModelScope.launch {
            // Combine several flows that indicate readiness.
            // For example, wait until we have loaded user preferences.
            combine(
                userPreferencesRepository.selectedStartDate,
                userPreferencesRepository.selectedEndDate,
                userPreferencesRepository.selectedCategoryId,
                userPreferencesRepository.selectedSupplierId
            ) { startDate, endDate, categoryId, supplierId ->
                _selectedStartDate.value = startDate
                _selectedEndDate.value = endDate
                _selectedCategoryId.value = categoryId
                _selectedSupplierId.value = supplierId
                // Once the first set of preferences are loaded, we can consider the app "ready".
                _isReady.value = true
            }.collect() // Use collect() to start the flow processing
        }
    }

    // --- Public Functions to Modify Data (delegated to repository) ---
    fun addExpense(expense: Expense) { viewModelScope.launch { expenseRepository.addExpense(expense); _refreshTrigger.value++ } }
    fun updateExpense(expense: Expense) { viewModelScope.launch { expenseRepository.updateExpense(expense); _refreshTrigger.value++ } }
    fun deleteExpense(expense: Expense) { viewModelScope.launch { expenseRepository.deleteExpense(expense); _refreshTrigger.value++ } }
    fun getExpenseWithDetailsById(id: Int): Flow<ExpenseWithDetails?> = expenseRepository.getExpenseWithDetailsById(id)
    suspend fun addProduct(product: Product): Long = expenseRepository.addProduct(product)
    suspend fun getProductByName(name: String): Product? = expenseRepository.getProductByName(name)
    suspend fun addSupplier(supplier: Supplier): Long = expenseRepository.addSupplier(supplier)
    suspend fun getSupplierByName(name: String): Supplier? = expenseRepository.getSupplierByName(name)
    suspend fun addCategory(category: Category): Long = expenseRepository.addCategory(category)
    suspend fun getCategoryByName(name: String): Category? = expenseRepository.getCategoryByName(name)
    suspend fun hasProductsInCategory(categoryId: Int): Boolean = expenseRepository.hasProductsInCategory(categoryId)
    fun updateCategory(category: Category) { viewModelScope.launch { expenseRepository.updateCategory(category); _refreshTrigger.value++ } }
    fun deleteCategory(category: Category) { viewModelScope.launch { expenseRepository.deleteCategory(category); _refreshTrigger.value++ } }
    fun updateSupplier(supplier: Supplier) { viewModelScope.launch { expenseRepository.updateSupplier(supplier); _refreshTrigger.value++ } }
    fun deleteSupplier(supplier: Supplier) { viewModelScope.launch { expenseRepository.deleteSupplier(supplier); _refreshTrigger.value++ } }
    fun setCategoryFilter(categoryId: Int?) { userPreferencesRepository.updateSelectedCategoryId(categoryId) }
    fun setSupplierFilter(supplierId: Int?) { userPreferencesRepository.updateSelectedSupplierId(supplierId) }
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
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
            }
            "LastMonth" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
                val endOfLastMonth = Calendar.getInstance().apply {
                    timeInMillis = startDate
                    add(Calendar.MONTH, 1)
                    add(Calendar.DAY_OF_MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                endDate = endOfLastMonth.timeInMillis
            }
            "ThisYear" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
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
        userPreferencesRepository.updateSelectedDates(startDate, endDate)
    }
    fun setCustomDateRangeFilter(startDate: Long?, endDate: Long?) { userPreferencesRepository.updateSelectedDates(startDate, endDate) }
    fun resetFilters() {
        setDateRangeFilter("All")
        userPreferencesRepository.updateSelectedCategoryId(null)
        userPreferencesRepository.updateSelectedSupplierId(null)
    }
    fun getProductsForCategory(categoryId: Int): Flow<List<Product>> = expenseRepository.getProductsForCategory(categoryId)
    suspend fun getProductByNameInCategory(name: String, categoryId: Int): Product? = expenseRepository.getProductByNameInCategory(name, categoryId)
    fun updateProduct(product: Product) { viewModelScope.launch { expenseRepository.updateProduct(product) } }
    fun deleteProduct(product: Product) { viewModelScope.launch { expenseRepository.deleteProduct(product) } }
    suspend fun productHasExpenses(productId: Int): Boolean = expenseRepository.productHasExpenses(productId)
    suspend fun supplierHasExpenses(supplierId: Int): Boolean = expenseRepository.supplierHasExpenses(supplierId)
    fun deleteSupplierAndExpenses(supplier: Supplier) { viewModelScope.launch { expenseRepository.deleteSupplierAndExpenses(supplier); _refreshTrigger.value++ } }
}

