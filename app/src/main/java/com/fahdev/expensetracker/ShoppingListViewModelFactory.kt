package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating [ShoppingListViewModel] instances.
 * This is necessary because [ShoppingListViewModel] has a constructor that takes an [Application] parameter.
 */
class ShoppingListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    /**
     * Creates a new instance of the given [modelClass].
     *
     * @param modelClass The Class of the ViewModel to create.
     * @param <T> The type of the ViewModel.
     * @return A new instance of the ViewModel.
     * @throws IllegalArgumentException If the [modelClass] is not assignable from [ShoppingListViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is assignable from ShoppingListViewModel
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            // Suppress the unchecked cast warning as we've already checked the type
            @Suppress("UNCHECKED_CAST")
            return ShoppingListViewModel(application) as T
        }
        // If the ViewModel class is not recognized, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
