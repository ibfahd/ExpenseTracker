package com.fahdev.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.LocaleHelper
import com.fahdev.expensetracker.data.UserPreferencesRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Settings screen.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferencesRepository = UserPreferencesRepository.getInstance(application)

    // Currency-related properties
    val selectedCurrencyCode: StateFlow<String> = userPreferencesRepository.currencyCode
    val availableCurrencies: List<CurrencyHelper.CurrencyInfo> = CurrencyHelper.getAvailableCurrencies()

    fun setCurrency(currencyCode: String) {
        userPreferencesRepository.setCurrency(currencyCode)
    }

    // Language-related properties
    val selectedLanguage: StateFlow<String> = userPreferencesRepository.language
    val availableLanguages: List<LocaleHelper.Language> = LocaleHelper.supportedLanguages

    fun setLanguage(languageTag: String) {
        // Persist the new language setting
        userPreferencesRepository.setLanguage(languageTag)
        // Apply the new locale to the app
        LocaleHelper.updateAppLocale(languageTag)
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
