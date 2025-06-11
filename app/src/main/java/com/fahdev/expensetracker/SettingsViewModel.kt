package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.UserPreferencesRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Settings screen.
 *
 * @param application The application context, used to get the UserPreferencesRepository instance.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    // Get the singleton instance of the repository.
    private val userPreferencesRepository = UserPreferencesRepository.getInstance(application)

    /**
     * A flow that emits the currently selected currency code.
     */
    val selectedCurrencyCode: StateFlow<String> = userPreferencesRepository.currencyCode

    /**
     * A list of all available currencies to be displayed in the settings.
     */
    val availableCurrencies: List<CurrencyHelper.CurrencyInfo> = CurrencyHelper.getAvailableCurrencies()

    /**
     * Updates the user's preferred currency.
     *
     * @param currencyCode The new currency code to be saved.
     */
    fun setCurrency(currencyCode: String) {
        userPreferencesRepository.setCurrency(currencyCode)
    }
}

/**
 * Factory for creating [SettingsViewModel] instances.
 */
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
