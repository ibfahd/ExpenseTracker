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
import kotlinx.coroutines.ExperimentalCoroutinesApi // Import the annotation

/**
 * ViewModel for managing shopping list data.
 * Provides access to shopping list items, products, and suppliers,
 * and handles operations like adding, updating, deleting items,
 * and managing shopping trips.
 */
@OptIn(ExperimentalCoroutinesApi::class) // Apply the opt-in annotation to the class
class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    // Data Access Objects (DAOs) for interacting with the Room database
    private val shoppingListItemDao: ShoppingListItemDao
    private val expenseDao: ExpenseDao
    private val productDao: ProductDao
    private val supplierDao: SupplierDao

    // MutableStateFlows to hold the currently selected supplier and shopping date.
    // These are private to ensure state modifications are controlled by the ViewModel.
    private val _currentSupplierId = MutableStateFlow<Int?>(null)
    private val _currentShoppingDate = MutableStateFlow<Long>(0L) // 0L indicates no date selected initially

    // Publicly exposed StateFlows for UI observation.
    // UI components can collect these flows to react to changes in selected supplier or date.
    val currentSupplierId: StateFlow<Int?> = _currentSupplierId.asStateFlow()
    val currentShoppingDate: StateFlow<Long> = _currentShoppingDate.asStateFlow()

    /**
     * A Flow that emits the list of [ShoppingListItem]s for the currently selected supplier and shopping date.
     * This flow combines [_currentSupplierId] and [_currentShoppingDate] to react to changes
     * in either and fetch the corresponding shopping list items from the database.
     *
     * Using `flatMapLatest` ensures that if either `supplierId` or `shoppingDate` changes,
     * the previous database query is cancelled and a new one is started, preventing stale data.
     */
    val shoppingListItems: Flow<List<ShoppingListItem>> = combine(
        _currentSupplierId,
        _currentShoppingDate
    ) { supplierId, shoppingDate ->
        // Only fetch items if both supplierId and shoppingDate are valid
        if (supplierId != null && shoppingDate != 0L) {
            // Directly return the Flow from the DAO, allowing for real-time updates.
            // DO NOT use .first() here, as it would only emit the initial list and then complete.
            shoppingListItemDao.getShoppingListItemsForTrip(supplierId, shoppingDate)
        } else {
            // If no valid supplier or date, emit an empty list
            flowOf(emptyList())
        }
    }.flatMapLatest { it } // flatMapLatest flattens the Flow<Flow<List<ShoppingListItem>>> to Flow<List<ShoppingListItem>>

    // Flows to expose all available suppliers and products from the database.
    val allSuppliers: Flow<List<Supplier>>
    val allProducts: Flow<List<Product>>

    /**
     * Initializes the ViewModel.
     * Sets up database DAOs and initializes flows for all suppliers and products.
     * Also attempts to select the first supplier if available on startup.
     */
    init {
        // Get the singleton instance of the AppDatabase
        val database = AppDatabase.getDatabase(application)
        // Initialize DAOs
        shoppingListItemDao = database.shoppingListItemDao()
        expenseDao = database.expenseDao()
        productDao = database.productDao()
        supplierDao = database.supplierDao()

        // Initialize flows for all suppliers and products
        allSuppliers = supplierDao.getAllSuppliers()
        allProducts = productDao.getAllProducts()

        // Launch a coroutine to perform initial data loading (e.g., selecting a default supplier)
        viewModelScope.launch {
            // Collect the first list of suppliers and attempt to select the first one.
            // `firstOrNull()` is used to get the initial value and then stop collecting.
            allSuppliers.firstOrNull()?.let { suppliers ->
                suppliers.firstOrNull()?.id?.let { firstSupplierId ->
                    selectSupplier(firstSupplierId)
                }
            }
        }
    }

    /**
     * Selects a supplier and loads or creates the latest shopping list for that supplier.
     *
     * @param supplierId The ID of the supplier to select.
     */
    fun selectSupplier(supplierId: Int) {
        viewModelScope.launch {
            _currentSupplierId.value = supplierId
            loadOrCreateLatestListForSupplier(supplierId)
        }
    }

    /**
     * Loads the latest shopping date for a given supplier, or sets the current time
     * if no previous shopping date exists for that supplier.
     *
     * @param supplierId The ID of the supplier.
     */
    private suspend fun loadOrCreateLatestListForSupplier(supplierId: Int) {
        // Get the latest shopping date for the given supplier
        val latestDate = shoppingListItemDao.getLatestShoppingDateForSupplier(supplierId)
        // Update _currentShoppingDate. If latestDate is null, use the current time.
        _currentShoppingDate.value = latestDate ?: System.currentTimeMillis()
    }

    /**
     * Starts a new shopping trip for the currently selected supplier.
     * If there are existing items from the previous latest trip, they are copied
     * to the new trip with quantities reset to 0 and unit prices cleared (for re-planning).
     */
    fun startNewTripForSupplier() {
        viewModelScope.launch(Dispatchers.IO) { // Perform database operations on IO dispatcher
            val currentSupplier = _currentSupplierId.value
            // If no supplier is selected, cannot start a new trip
            if (currentSupplier == null) {
                // Optionally, show a message to the user that a supplier must be selected
                return@launch
            }

            // Get the latest shopping date for the current supplier
            val oldLatestDate = shoppingListItemDao.getLatestShoppingDateForSupplier(currentSupplier)
            // Define the timestamp for the new shopping trip
            val newShoppingDate = System.currentTimeMillis()

            // If there was a previous trip, copy its items to the new trip
            if (oldLatestDate != null && oldLatestDate != 0L) { // Ensure oldLatestDate is valid
                val oldItems = shoppingListItemDao.getItemsForLatestTrip(currentSupplier, oldLatestDate)
                val newItemsToInsert = mutableListOf<ShoppingListItem>()

                oldItems.forEach { item ->
                    // Copy existing item to a new one for the new trip.
                    // Reset ID to 0 for auto-generation, set new shoppingDate,
                    // and reset quantity/unitPrice if it was a purchased item.
                    val newItem = if (item.quantity > 0.0 && item.unitPrice != null) {
                        item.copy(id = 0, shoppingDate = newShoppingDate, quantity = 0.0, unitPrice = null)
                    } else {
                        item.copy(id = 0, shoppingDate = newShoppingDate)
                    }
                    newItemsToInsert.add(newItem)
                }
                // Insert all copied items into the database
                newItemsToInsert.forEach { shoppingListItemDao.insert(it) }
            }
            // Update the current shopping date to the new trip's timestamp
            _currentShoppingDate.value = newShoppingDate
        }
    }

    /**
     * Adds a new shopping list item to the current trip.
     *
     * @param productId The ID of the product.
     * @param unit The unit of measurement (e.g., "kg", "pcs"), nullable.
     * @param plannedQuantity The planned quantity of the product.
     */
    fun addShoppingItem(productId: Int, unit: String?, plannedQuantity: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSupplier = _currentSupplierId.value
            val currentTripDate = _currentShoppingDate.value

            // Ensure a supplier and a valid trip date are selected
            if (currentSupplier == null || currentTripDate == 0L) {
                // Optionally, provide user feedback if prerequisites are not met
                return@launch
            }

            val newItem = ShoppingListItem(
                productId = productId,
                unit = unit,
                quantity = plannedQuantity, // This is the planned quantity
                unitPrice = null, // Unit price is initially null for planned items
                supplierId = currentSupplier,
                shoppingDate = currentTripDate
            )
            shoppingListItemDao.insert(newItem)
        }
    }

    /**
     * Updates an existing shopping list item in the database.
     * This function is responsible for persisting changes to the item's properties
     * (like quantity or unit price) as the user types, but does NOT create an expense.
     *
     * @param item The [ShoppingListItem] to update.
     */
    fun updateShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingListItemDao.update(item)
        }
    }

    /**
     * Records expenses for all purchased items in the current shopping list.
     * After recording, the quantity and unit price of validated items are reset.
     *
     * @return The number of expenses successfully recorded.
     */
    suspend fun recordAllPurchases(): Int {
        var recordedCount = 0
        // Collect the current state of shopping list items once
        val itemsToProcess = shoppingListItems.first() // Use .first() to get the current list and complete the flow

        viewModelScope.launch(Dispatchers.IO) {
            itemsToProcess.forEach { item ->
                if (item.quantity > 0.0 && item.unitPrice != null) {
                    val amount = item.quantity * item.unitPrice
                    val expense = Expense(
                        productId = item.productId,
                        supplierId = item.supplierId ?: 0,
                        amount = amount,
                        timestamp = System.currentTimeMillis()
                    )
                    expenseDao.insertExpense(expense)
                    recordedCount++

                    // Reset quantity and unitPrice after recording expense
                    val updatedItem = item.copy(quantity = 0.0, unitPrice = null)
                    shoppingListItemDao.update(updatedItem)
                }
            }
        }.join() // Wait for all insertions and updates to complete
        return recordedCount
    }

    /**
     * Deletes a shopping list item.
     *
     * @param item The [ShoppingListItem] to delete.
     */
    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingListItemDao.delete(item)
        }
    }
}
