package com.fahdev.expensetracker

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.graphics.SolidColor

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

    private data class FilterParams(val startDate: Long?, val endDate: Long?, val categoryId: Int?, val supplierId: Int?)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredExpenses: Flow<List<ExpenseWithDetails>> = combine(_selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger) { startDate, endDate, categoryId, supplierId, _ -> FilterParams(startDate, endDate, categoryId, supplierId) }.flatMapLatest { params -> expenseRepository.getFilteredExpensesWithDetails(params.startDate, params.endDate, params.categoryId, params.supplierId) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalFilteredExpenses: StateFlow<Double> = combine(_selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger) { startDate, endDate, categoryId, supplierId, _ -> FilterParams(startDate, endDate, categoryId, supplierId) }.flatMapLatest { params -> expenseRepository.getTotalFilteredExpenses(params.startDate, params.endDate, params.categoryId, params.supplierId) }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Reporting Data ---
    val totalExpensesAllTime: StateFlow<Double> = expenseRepository.totalExpensesAllTime.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val firstExpenseDate: StateFlow<Long?> = expenseRepository.firstExpenseDate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val totalTransactionCount: StateFlow<Int> = expenseRepository.totalTransactionCount.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val spendingByCategory: StateFlow<List<CategorySpending>> = expenseRepository.spendingByCategory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val spendingBySupplier: StateFlow<List<SupplierSpending>> = expenseRepository.spendingBySupplier.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val averageDailyExpense: StateFlow<Double> = combine(totalExpensesAllTime, firstExpenseDate) { total, firstDateMs -> if (total > 0 && firstDateMs != null) { val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstDateMs).coerceAtLeast(1); total / days } else { 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val averageMonthlyExpense: StateFlow<Double> = combine(totalExpensesAllTime, firstExpenseDate) { total, firstDateMs -> if (total > 0 && firstDateMs != null) { val firstCal = Calendar.getInstance().apply { timeInMillis = firstDateMs }; val currentCal = Calendar.getInstance(); var months = (currentCal.get(Calendar.YEAR) - firstCal.get(Calendar.YEAR)) * 12; months -= firstCal.get(Calendar.MONTH); months += currentCal.get(Calendar.MONTH); if (currentCal.get(Calendar.DAY_OF_MONTH) < firstCal.get(Calendar.DAY_OF_MONTH) && months > 0) { months-- }; total / max(1, months + 1) } else { 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalFilteredTransactionCount: StateFlow<Int> = combine(_selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger) { startDate, endDate, categoryId, supplierId, _ ->
        FilterParams(startDate, endDate, categoryId, supplierId)
    }.flatMapLatest { params ->
        expenseRepository.getFilteredTransactionCount(params.startDate, params.endDate, params.categoryId, params.supplierId)
    }.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageDailyExpenseFiltered: StateFlow<Double> = combine(totalFilteredExpenses, _selectedStartDate, _selectedEndDate) { total, startDate, endDate ->
        if (total > 0 && startDate != null) {
            val end = endDate ?: System.currentTimeMillis()
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(end - startDate).coerceAtLeast(1)
            total / days
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageMonthlyExpenseFiltered: StateFlow<Double> = combine(totalFilteredExpenses, _selectedStartDate, _selectedEndDate) { total, startDate, endDate ->
        if (total > 0 && startDate != null) {
            val firstCal = Calendar.getInstance().apply { timeInMillis = startDate }
            val endCal = Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }
            var months = (endCal.get(Calendar.YEAR) - firstCal.get(Calendar.YEAR)) * 12
            months -= firstCal.get(Calendar.MONTH)
            months += endCal.get(Calendar.MONTH)
            if (endCal.get(Calendar.DAY_OF_MONTH) < firstCal.get(Calendar.DAY_OF_MONTH) && months > 0) {
                months--
            }
            total / max(1, months + 1)
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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


    // --- Trend Analysis State ---
    enum class TrendGrouping { DAY, WEEK, MONTH }
    private val _trendGrouping = MutableStateFlow(TrendGrouping.DAY)
    val trendGrouping: StateFlow<TrendGrouping> = _trendGrouping.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val trendData: StateFlow<List<ExpenseDao.TrendDataPoint>> = combine(
        _selectedStartDate, _selectedEndDate, _trendGrouping, _refreshTrigger
    ) { startDate, endDate, grouping, _ ->
        Triple(startDate, endDate, grouping)
    }.flatMapLatest { (startDate, endDate, grouping) ->
        when (grouping) {
            TrendGrouping.DAY -> expenseRepository.getSpendingByDay(startDate, endDate)
            TrendGrouping.WEEK -> expenseRepository.getSpendingByWeek(startDate, endDate)
            TrendGrouping.MONTH -> expenseRepository.getSpendingByMonth(startDate, endDate)
        }
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
     * Weekly spending trend data
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val spendingByWeek: Flow<List<ExpenseDao.TrendDataPoint>> = combine(
        _selectedStartDate,
        _selectedEndDate
    ) { startDate, endDate ->
        expenseRepository.getSpendingByWeek(startDate, endDate)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val previousPeriodTotal: StateFlow<Double> = combine(
        _selectedStartDate, _selectedEndDate, _selectedCategoryId, _selectedSupplierId, _refreshTrigger
    ) { sDate, eDate, catId, supId, _ ->
        if (sDate == null) return@combine FilterParams(null, null, null, null)

        val end = eDate ?: System.currentTimeMillis()
        val duration = end - sDate
        val previousEndDate = sDate - 1
        val previousStartDate = previousEndDate - duration

        FilterParams(previousStartDate, previousEndDate, catId, supId)
    }.flatMapLatest { params ->
        if (params.startDate != null && params.startDate < 0) flowOf(0.0) else expenseRepository.getTotalFilteredExpenses(params.startDate, params.endDate, params.categoryId, params.supplierId)
    }.map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


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

    fun setTrendGrouping(grouping: TrendGrouping) {
        _trendGrouping.value = grouping
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
    fun getCategoryById(id: Int): Flow<Category?> = expenseRepository.getCategoryById(id)
    fun updateSupplier(supplier: Supplier) { viewModelScope.launch { expenseRepository.updateSupplier(supplier); _refreshTrigger.value++ } }
    fun deleteSupplier(supplier: Supplier) { viewModelScope.launch { expenseRepository.deleteSupplier(supplier); _refreshTrigger.value++ } }
    fun getSupplierById(id: Int): Flow<Supplier?> = expenseRepository.getSupplierById(id)
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

// Extension functions for chart data transformation
object ChartDataTransformations {

    /**
     * Transform spending data into chart-friendly format with color assignments
     */
    fun transformCategoryDataForPieChart(
        data: List<CategorySpending>,
        colors: List<Color>
    ): List<ir.ehsannarmani.compose_charts.models.Pie> {
        return data.take(8).mapIndexed { index, spending ->
            ir.ehsannarmani.compose_charts.models.Pie(
                label = spending.categoryName,
                data = spending.totalAmount,
                color = colors[index % colors.size]
            )
        }
    }

    /**
     * Transform trend data for line chart with proper time formatting
     */
    fun transformTrendDataForLineChart(
        data: List<ExpenseDao.TrendDataPoint>,
        color: Color
    ): ir.ehsannarmani.compose_charts.models.Line {
        return ir.ehsannarmani.compose_charts.models.Line(
            label = "Spending Trend",
            values = data.map { it.amount },
            color = SolidColor(color),
            firstGradientFillColor = color.copy(alpha = 0.5f),
            secondGradientFillColor = Color.Transparent,
            strokeAnimationSpec = androidx.compose.animation.core.tween(2000),
            gradientAnimationDelay = 1000,
            drawStyle = ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke(width = 3.dp)
        )
    }

    /**
     * Get optimal time period for trend analysis based on data range
     */
    fun getOptimalTrendPeriod(startDate: Long?, endDate: Long?): TrendPeriod {
        if (startDate == null || endDate == null) return TrendPeriod.MONTHLY

        val daysDiff = (endDate - startDate) / (24 * 60 * 60 * 1000)
        return when {
            daysDiff <= 31 -> TrendPeriod.DAILY
            daysDiff <= 90 -> TrendPeriod.WEEKLY
            else -> TrendPeriod.MONTHLY
        }
    }
}

enum class TrendPeriod {
    DAILY, WEEKLY, MONTHLY
}

// Chart color palettes
object ChartColors {
    val categoryColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFF84CC16)  // Lime
    )

    val supplierColors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899)  // Pink
    )
}