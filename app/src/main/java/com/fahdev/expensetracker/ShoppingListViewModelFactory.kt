package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fahdev.expensetracker.data.ShoppingRepository

/**
 * Factory for creating [ShoppingListViewModel] instances.
 */
class ShoppingListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            // Instantiate the database and DAOs
            val database = AppDatabase.getDatabase(application)
            val shoppingListItemDao = database.shoppingListItemDao()
            val expenseDao = database.expenseDao()
            val productDao = database.productDao()
            val supplierDao = database.supplierDao()

            // Create the repository instance
            val shoppingRepository = ShoppingRepository(shoppingListItemDao, expenseDao, productDao, supplierDao)

            return ShoppingListViewModel(application, shoppingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
