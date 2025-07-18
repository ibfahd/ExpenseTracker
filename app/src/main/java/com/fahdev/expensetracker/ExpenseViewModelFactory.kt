package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fahdev.expensetracker.data.ExpenseRepository
import com.fahdev.expensetracker.data.UserPreferencesRepository

/**
 * Factory for creating [ExpenseViewModel] instances.
 * This is necessary because the ViewModel has a constructor that takes dependencies.
 */
class ExpenseViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            // Instantiate the database and DAOs
            val database = AppDatabase.getDatabase(application)
            val expenseDao = database.expenseDao()
            val productDao = database.productDao()
            val supplierDao = database.supplierDao()
            val categoryDao = database.categoryDao()

            // Create the repository instance
            val expenseRepository = ExpenseRepository(expenseDao, productDao, supplierDao, categoryDao)

            // Provide the repository instance to the ViewModel
            val userPrefsRepo = UserPreferencesRepository.getInstance(application)
            return ExpenseViewModel(application, expenseRepository, userPrefsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
