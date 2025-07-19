package com.fahdev.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val _currentSupplierId = MutableStateFlow<Int?>(null)
    val currentSupplierId: StateFlow<Int?> = _currentSupplierId.asStateFlow()
    private val _currentShoppingDate = MutableStateFlow(0L)
    val shoppingListItems: StateFlow<List<ShoppingListItem>> = combine(
        _currentSupplierId,
        _currentShoppingDate
    ) { supplierId, shoppingDate ->
        Pair(supplierId, shoppingDate)
    }.flatMapLatest { (supplierId, shoppingDate) ->
        if (supplierId != null) {
            shoppingRepository.getShoppingListItemsForTrip(supplierId, shoppingDate)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSuppliers: Flow<List<Supplier>> = shoppingRepository.allSuppliers
    val allProducts: Flow<List<Product>> = shoppingRepository.allProducts
    init {
        viewModelScope.launch {
            allSuppliers.firstOrNull()?.firstOrNull()?.id?.let { firstSupplierId ->
                selectSupplier(firstSupplierId)
            }
        }
    }

    fun selectSupplier(supplierId: Int) {
        viewModelScope.launch {
            _currentSupplierId.value = supplierId
            val latestDate = shoppingRepository.getLatestShoppingDateForSupplier(supplierId)
            _currentShoppingDate.value = latestDate ?: System.currentTimeMillis()
        }
    }

    fun addShoppingItem(productId: Int, unit: String?, initialPlannedQuantity: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSupplier = _currentSupplierId.value ?: return@launch
            val currentTripDate = _currentShoppingDate.value.takeIf { it != 0L } ?: System.currentTimeMillis().also {
                _currentShoppingDate.value = it
            }

            val newItem = ShoppingListItem(
                productId = productId,
                unit = unit,
                plannedQuantity = initialPlannedQuantity,
                purchasedQuantity = 0.0,
                unitPrice = null,
                supplierId = currentSupplier,
                shoppingDate = currentTripDate
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