package com.fahdev.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.data.ShoppingRepository
import com.fahdev.expensetracker.data.Supplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {
    private val _currentSupplier = MutableStateFlow<Supplier?>(null)
    val currentSupplier: StateFlow<Supplier?> = _currentSupplier.asStateFlow()

    private val _shoppingDate = MutableStateFlow<Long?>(null)
    val shoppingDate: StateFlow<Long?> = _shoppingDate.asStateFlow()

    val shoppingListItems: StateFlow<List<ShoppingListItem>> = combine(
        _currentSupplier,
        _shoppingDate
    ) { supplier, date ->
        Pair(supplier, date)
    }.flatMapLatest { (supplier, date) ->
        if (supplier != null && date != null) {
            shoppingRepository.getShoppingListItemsForTrip(supplier.id, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: Flow<List<Supplier>> = shoppingRepository.allSuppliers
    val allProducts: Flow<List<Product>> = shoppingRepository.allProducts

    val categoriesForSupplier: StateFlow<List<Category>> = _currentSupplier.flatMapLatest { supplier ->
        if (supplier != null) {
            shoppingRepository.getCategoriesForSupplier(supplier.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSupplier(supplier: Supplier) {
        viewModelScope.launch {
            if (_currentSupplier.value == supplier) {
                // Deselect if the same supplier is clicked again
                _currentSupplier.value = null
                _shoppingDate.value = null
            } else {
                _currentSupplier.value = supplier
                val latestDate = shoppingRepository.getLatestShoppingDateForSupplier(supplier.id)
                _shoppingDate.value = latestDate ?: System.currentTimeMillis()
            }
        }
    }

    fun selectShoppingDate(date: Long) {
        _shoppingDate.value = date
    }

    fun addShoppingItem(productId: Int, unit: String?, initialPlannedQuantity: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val supplier = _currentSupplier.value ?: return@launch
            val date = _shoppingDate.value ?: return@launch

            val newItem = ShoppingListItem(
                productId = productId,
                unit = unit,
                plannedQuantity = initialPlannedQuantity,
                purchasedQuantity = 0.0,
                unitPrice = null,
                supplierId = supplier.id,
                shoppingDate = date
            )
            shoppingRepository.addShoppingItem(newItem)
        }
    }

    fun updateShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingRepository.updateShoppingItem(item)
        }
    }

    suspend fun recordAllPurchases(): Int {
        val itemsToProcess = shoppingListItems.first()
        return shoppingRepository.recordAllPurchases(itemsToProcess)
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingRepository.deleteShoppingItem(item)
        }
    }
}