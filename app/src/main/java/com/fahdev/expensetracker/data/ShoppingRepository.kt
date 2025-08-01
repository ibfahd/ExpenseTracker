package com.fahdev.expensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for handling all data operations related to the Shopping List feature.
 */
class ShoppingRepository(
    private val shoppingListItemDao: ShoppingListItemDao,
    private val expenseDao: ExpenseDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val categorySupplierDao: CategorySupplierDao
) {

    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    fun getShoppingListItemsForTrip(supplierId: Int, shoppingDate: Long): Flow<List<ShoppingListItem>> {
        return if (supplierId != 0 && shoppingDate != 0L) {
            shoppingListItemDao.getShoppingListItemsForTrip(supplierId, shoppingDate)
        } else {
            flowOf(emptyList())
        }
    }

    fun getCategoriesForSupplier(supplierId: Int): Flow<List<Category>> {
        return categorySupplierDao.getCategoriesForSupplier(supplierId)
    }

    suspend fun getLatestShoppingDateForSupplier(supplierId: Int): Long? {
        return shoppingListItemDao.getLatestShoppingDateForSupplier(supplierId)
    }

    suspend fun addShoppingItem(newItem: ShoppingListItem) {
        shoppingListItemDao.insert(newItem)
    }

    suspend fun updateShoppingItem(item: ShoppingListItem) {
        shoppingListItemDao.update(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingListItem) {
        shoppingListItemDao.delete(item)
    }

    /**
     * Records expenses for all purchased items in a given list.
     *
     * @param itemsToProcess The list of shopping items to validate and record.
     * @return The number of expenses successfully recorded.
     */
    suspend fun recordAllPurchases(itemsToProcess: List<ShoppingListItem>): Int {
        var recordedCount = 0
        withContext(Dispatchers.IO) {
            itemsToProcess.forEach { item ->
                if (item.purchasedQuantity > 0.0 && item.unitPrice != null) {
                    val amount = item.purchasedQuantity * item.unitPrice
                    val expense = Expense(
                        productId = item.productId,
                        supplierId = item.supplierId ?: 0,
                        amount = amount,
                        timestamp = System.currentTimeMillis()
                    )
                    launch { expenseDao.insertExpense(expense) }
                    recordedCount++

                    // Reset the item in the list after it has been recorded
                    val updatedItem = item.copy(
                        purchasedQuantity = 0.0,
                        unitPrice = null
                    )
                    launch { shoppingListItemDao.update(updatedItem) }
                }
            }
        }
        return recordedCount
    }
}
