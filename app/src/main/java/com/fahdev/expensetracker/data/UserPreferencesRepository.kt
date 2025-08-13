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

    // --- Display Preferences ---
    private val _theme = MutableStateFlow(sharedPreferences.getString(KEY_THEME, "system") ?: "system")
    val theme = _theme.asStateFlow()

    fun setTheme(theme: String) {
        sharedPreferences.edit {
            putString(KEY_THEME, theme)
        }
        _theme.value = theme
    }

    private val _cardStyle = MutableStateFlow(sharedPreferences.getString(KEY_CARD_STYLE, "rounded") ?: "rounded")
    val cardStyle = _cardStyle.asStateFlow()

    fun setCardStyle(style: String) {
        sharedPreferences.edit {
            putString(KEY_CARD_STYLE, style)
        }
        _cardStyle.value = style
    }

    // --- Filter State ---
    private val _selectedStartDate = MutableStateFlow(sharedPreferences.getLong(KEY_START_DATE, -1L).let { if (it == -1L) null else it })
    val selectedStartDate = _selectedStartDate.asStateFlow()

    private val _selectedEndDate = MutableStateFlow(sharedPreferences.getLong(KEY_END_DATE, -1L).let { if (it == -1L) null else it })
    val selectedEndDate = _selectedEndDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(sharedPreferences.getInt(KEY_CATEGORY_ID, -1).let { if (it == -1) null else it })
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow(sharedPreferences.getInt(KEY_SUPPLIER_ID, -1).let { if (it == -1) null else it })
    val selectedSupplierId = _selectedSupplierId.asStateFlow()

    fun updateSelectedDates(startDate: Long?, endDate: Long?) {
        sharedPreferences.edit {
            if (startDate == null) remove(KEY_START_DATE) else putLong(KEY_START_DATE, startDate)
            if (endDate == null) remove(KEY_END_DATE) else putLong(KEY_END_DATE, endDate)
        }
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    fun updateSelectedCategoryId(categoryId: Int?) {
        sharedPreferences.edit {
            if (categoryId == null) remove(KEY_CATEGORY_ID) else putInt(KEY_CATEGORY_ID, categoryId)
        }
        _selectedCategoryId.value = categoryId
    }

    fun updateSelectedSupplierId(supplierId: Int?) {
        sharedPreferences.edit {
            if (supplierId == null) remove(KEY_SUPPLIER_ID) else putInt(KEY_SUPPLIER_ID, supplierId)
        }
        _selectedSupplierId.value = supplierId
    }

    companion object {
        private const val KEY_CURRENCY = "currency_code"
        private const val KEY_LANGUAGE = "language_tag"
        private const val KEY_THEME = "app_theme"
        private const val KEY_CARD_STYLE = "card_style"
        private const val KEY_START_DATE = "selected_start_date"
        private const val KEY_END_DATE = "selected_end_date"
        private const val KEY_CATEGORY_ID = "selected_category_id"
        private const val KEY_SUPPLIER_ID = "selected_supplier_id"

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
