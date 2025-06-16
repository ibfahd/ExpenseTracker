package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
            // Provide the repository instance to the ViewModel
            val userPrefsRepo = UserPreferencesRepository.getInstance(application)
            return ExpenseViewModel(application, userPrefsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
