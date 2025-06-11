package com.fahdev.expensetracker.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A repository for managing user preferences, such as the default currency.
 * This class uses SharedPreferences for persistence and follows a singleton pattern
 * to ensure all parts of the app use the same instance.
 */
class UserPreferencesRepository private constructor(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

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

    companion object {
        private const val KEY_CURRENCY = "currency_code"

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        /**
         * Returns the singleton instance of the repository.
         *
         * @param context The application context.
         * @return The singleton UserPreferencesRepository instance.
         */
        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
