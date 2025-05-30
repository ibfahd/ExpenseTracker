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

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    // Corrected to lateinit var for proper initialization
    private lateinit var shoppingListItemDao: ShoppingListItemDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var productDao: ProductDao
    private lateinit var supplierDao: SupplierDao

    private val _currentSupplierId = MutableStateFlow<Int?>(null)
    private val _currentShoppingDate = MutableStateFlow<Long>(0L)

    // Expose flows for UI observation
    val currentSupplierId: StateFlow<Int?> = _currentSupplierId.asStateFlow()
    val currentShoppingDate: StateFlow<Long> = _currentShoppingDate.asStateFlow()

    // Combined flow for the actual shopping list items for the current trip
    val shoppingListItems: Flow<List<ShoppingListItem>> = combine(
        _currentSupplierId,
        _currentShoppingDate
    ) { supplierId, shoppingDate ->
        if (supplierId != null && shoppingDate != 0L) {
            shoppingListItemDao.getShoppingListItemsForTrip(supplierId, shoppingDate).first()
        } else {
            emptyList()
        }
    }.distinctUntilChanged()

    val allSuppliers: Flow<List<Supplier>>
    val allProducts: Flow<List<Product>>

    init {
        val database = AppDatabase.getDatabase(application)
        shoppingListItemDao = database.shoppingListItemDao()
        expenseDao = database.expenseDao() // Get your ExpenseDao instance
        productDao = database.productDao() // Get your ProductDao instance
        supplierDao = database.supplierDao() // Get your SupplierDao instance

        // Initialize allSuppliers and allProducts here
        allSuppliers = supplierDao.getAllSuppliers()
        allProducts = productDao.getAllProducts()

        // Initial load: Try to select the first supplier or default
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

            if (oldLatestDate != null) {
                val oldItems = shoppingListItemDao.getItemsForLatestTrip(currentSupplier, oldLatestDate)
                val newItemsToInsert = mutableListOf<ShoppingListItem>()

                oldItems.forEach { item ->
                    val newItem = if (item.quantity > 0.0 && item.unitPrice != null) { // Use 0.0 for Double
                        item.copy(id = 0, shoppingDate = newShoppingDate, quantity = 0.0, unitPrice = null) // Use 0.0 for Double
                    } else {
                        item.copy(id = 0, shoppingDate = newShoppingDate)
                    }
                    newItemsToInsert.add(newItem)
                }
                newItemsToInsert.forEach { shoppingListItemDao.insert(it) }
            }
            _currentShoppingDate.value = newShoppingDate
        }
    }

    fun addShoppingItem(productId: Int, unit: String?, plannedQuantity: Double) { // Changed plannedQuantity to Double
        viewModelScope.launch(Dispatchers.IO) {
            val currentSupplier = _currentSupplierId.value ?: return@launch
            val currentTripDate = _currentShoppingDate.value
            val newItem = ShoppingListItem(
                productId = productId,
                unit = unit,
                quantity = plannedQuantity,
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

            if (item.quantity > 0.0 && item.unitPrice != null) { // Use 0.0 for Double
                val amount = item.quantity * item.unitPrice
                // Ensure Expense.supplierId is nullable or handle non-null case
                // Assuming Expense.supplierId is Int? or you handle nulls
                val expense = Expense(
                    productId = item.productId,
                    supplierId = item.supplierId!!, // This will be Int? as per ShoppingListItem
                    amount = amount,
                    timestamp = item.shoppingDate
                )
                expenseDao.insertExpense(expense)
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingListItemDao.delete(item)
        }
    }
}