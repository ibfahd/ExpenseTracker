package com.fahdev.expensetracker.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A repository for managing user preferences, such as the default currency, language,
 * and default home screen filter.
 */
class UserPreferencesRepository private constructor(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Currency Preference
    private val _currencyCode = MutableStateFlow(
        sharedPreferences.getString(KEY_CURRENCY, "USD") ?: "USD"
    )
    val currencyCode = _currencyCode.asStateFlow()

    fun setCurrency(currencyCode: String) {
        sharedPreferences.edit {
            putString(KEY_CURRENCY, currencyCode)
        }
        _currencyCode.value = currencyCode
    }

    // Language Preference
    private val _language = MutableStateFlow(
        sharedPreferences.getString(KEY_LANGUAGE, "system") ?: "system"
    )
    val language = _language.asStateFlow()

    fun setLanguage(languageTag: String) {
        sharedPreferences.edit {
            putString(KEY_LANGUAGE, languageTag)
        }
        _language.value = languageTag
    }

    // Default Home Filter Preference
    private val _homeScreenDefaultFilter = MutableStateFlow(
        sharedPreferences.getString(KEY_HOME_FILTER, "All") ?: "All"
    )
    val homeScreenDefaultFilter = _homeScreenDefaultFilter.asStateFlow()

    fun setHomeScreenDefaultFilter(filterKey: String) {
        sharedPreferences.edit {
            putString(KEY_HOME_FILTER, filterKey)
        }
        _homeScreenDefaultFilter.value = filterKey
    }

    companion object {
        private const val KEY_CURRENCY = "currency_code"
        private const val KEY_LANGUAGE = "language_tag"
        private const val KEY_HOME_FILTER = "home_screen_filter"

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
