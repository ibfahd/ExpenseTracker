package com.fahdev.expensetracker

import androidx.lifecycle.ViewModel
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.LocaleHelper
import com.fahdev.expensetracker.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val selectedCurrencyCode: StateFlow<String> = userPreferencesRepository.currencyCode
    val availableCurrencies: List<CurrencyHelper.CurrencyInfo> = CurrencyHelper.getAvailableCurrencies()
    fun setCurrency(currencyCode: String) {
        userPreferencesRepository.setCurrency(currencyCode)
    }
    val selectedLanguage: StateFlow<String> = userPreferencesRepository.language
    val availableLanguages: List<LocaleHelper.Language> = LocaleHelper.supportedLanguages
    fun setLanguage(languageTag: String) {
        userPreferencesRepository.setLanguage(languageTag)
        LocaleHelper.updateAppLocale(languageTag)
    }
    val selectedTheme: StateFlow<String> = userPreferencesRepository.theme
    fun setTheme(theme: String) {
        userPreferencesRepository.setTheme(theme)
    }
    val selectedCardStyle: StateFlow<String> = userPreferencesRepository.cardStyle
    fun setCardStyle(style: String) {
        userPreferencesRepository.setCardStyle(style)
    }
}