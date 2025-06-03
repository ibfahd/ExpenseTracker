package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ProductDao
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.data.ShoppingListItemDao
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.SupplierDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    private val shoppingListItemDao: ShoppingListItemDao
    private val expenseDao: ExpenseDao
    private val productDao: ProductDao
    private val supplierDao: SupplierDao

    private val _currentSupplierId = MutableStateFlow<Int?>(null)
    private val _currentShoppingDate = MutableStateFlow<Long>(0L)

    val currentSupplierId: StateFlow<Int?> = _currentSupplierId.asStateFlow()
    val currentShoppingDate: StateFlow<Long> = _currentShoppingDate.asStateFlow()

    val shoppingListItems: Flow<List<ShoppingListItem>> = combine(
        _currentSupplierId,
        _currentShoppingDate
    ) { supplierId, shoppingDate ->
        if (supplierId != null && shoppingDate != 0L) {
            shoppingListItemDao.getShoppingListItemsForTrip(supplierId, shoppingDate)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }

    val allSuppliers: Flow<List<Supplier>>
    val allProducts: Flow<List<Product>>

    init {
        val database = AppDatabase.getDatabase(application)
        shoppingListItemDao = database.shoppingListItemDao()
        expenseDao = database.expenseDao()
        productDao = database.productDao()
        supplierDao = database.supplierDao()

        allSuppliers = supplierDao.getAllSuppliers()
        allProducts = productDao.getAllProducts()

        viewModelScope.launch {
            allSuppliers.firstOrNull()?.let { suppliers ->
                suppliers.firstOrNull()?.id?.let { firstSupplierId ->
                    selectSupplier(firstSupplierId)
                }
            }
        }
    }

    fun selectSupplier(supplierId: Int) {
        viewModelScope.launch {
            _currentSupplierId.value = supplierId
            loadOrCreateLatestListForSupplier(supplierId)
        }
    }

    private suspend fun loadOrCreateLatestListForSupplier(supplierId: Int) {
        val latestDate = shoppingListItemDao.getLatestShoppingDateForSupplier(supplierId)
        _currentShoppingDate.value = latestDate ?: System.currentTimeMillis()
    }

    fun startNewTripForSupplier() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSupplier = _currentSupplierId.value ?: return@launch
            val oldLatestDate = shoppingListItemDao.getLatestShoppingDateForSupplier(currentSupplier)
            val newShoppingDate = System.currentTimeMillis()

            if (oldLatestDate != null && oldLatestDate != 0L) {
                val oldItems = shoppingListItemDao.getItemsForLatestTrip(currentSupplier, oldLatestDate)
                val newItemsToInsert = oldItems.map { item ->
                    // When starting a new trip, copy the planned quantity.
                    // Purchased quantity and unit price are reset for the new trip.
                    item.copy(
                        id = 0, // New item for Room to auto-generate ID
                        shoppingDate = newShoppingDate,
                        plannedQuantity = item.plannedQuantity, // Preserve planned quantity
                        purchasedQuantity = 0.0, // Reset purchased quantity for the new trip
                        unitPrice = null // Reset unit price for the new trip
                    )
                }
                newItemsToInsert.forEach { shoppingListItemDao.insert(it) }
            }
            _currentShoppingDate.value = newShoppingDate
        }
    }

    /**
     * Adds a new shopping list item to the current trip.
     *
     * @param productId The ID of the product.
     * @param unit The unit of measurement (e.g., "kg", "pcs"), nullable.
     * @param initialPlannedQuantity The initially planned quantity of the product.
     */
    fun addShoppingItem(productId: Int, unit: String?, initialPlannedQuantity: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSupplier = _currentSupplierId.value
            val currentTripDate = _currentShoppingDate.value

            if (currentSupplier == null || currentTripDate == 0L) {
                return@launch
            }

            val newItem = ShoppingListItem(
                productId = productId,
                unit = unit,
                plannedQuantity = initialPlannedQuantity, // Set the planned quantity
                purchasedQuantity = 0.0, // Purchased quantity starts at 0
                unitPrice = null,
                supplierId = currentSupplier,
                shoppingDate = currentTripDate
            )
            shoppingListItemDao.insert(newItem)
        }
    }

    fun updateShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingListItemDao.update(item)
        }
    }

    /**
     * Records expenses for all purchased items in the current shopping list.
     * After recording, the purchased quantity and unit price of validated items are reset.
     * The planned quantity remains unchanged.
     *
     * @return The number of expenses successfully recorded.
     */
    suspend fun recordAllPurchases(): Int {
        var recordedCount = 0
        val itemsToProcess = shoppingListItems.first()

        // Use a new coroutine scope to ensure all DB operations complete before returning.
        kotlinx.coroutines.coroutineScope {
            itemsToProcess.forEach { item ->
                // Only record if there's a purchased quantity and a unit price
                if (item.purchasedQuantity > 0.0 && item.unitPrice != null) {
                    val amount = item.purchasedQuantity * item.unitPrice
                    val expense = Expense(
                        productId = item.productId,
                        supplierId = item.supplierId ?: 0, // Assuming 0 is a safe default if supplierId is somehow null
                        amount = amount,
                        timestamp = System.currentTimeMillis() // Or use item.shoppingDate if appropriate
                    )
                    launch(Dispatchers.IO) { expenseDao.insertExpense(expense) }
                    recordedCount++

                    // After recording, reset purchased quantity and unit price for this item in the list.
                    // Planned quantity remains.
                    val updatedItem = item.copy(
                        purchasedQuantity = 0.0,
                        unitPrice = null
                    )
                    launch(Dispatchers.IO) { shoppingListItemDao.update(updatedItem) }
                }
            }
        }
        return recordedCount
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingListItemDao.delete(item)
        }
    }
}
