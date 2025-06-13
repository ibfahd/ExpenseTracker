package com.fahdev.expensetracker

import android.app.Application
import com.fahdev.expensetracker.data.LocaleHelper
import com.fahdev.expensetracker.data.UserPreferencesRepository

/**
 * Custom Application class to handle app-wide initialization.
 */
class MyExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize user preferences repository
        val userPrefsRepo = UserPreferencesRepository.getInstance(this)

        // Set the app locale based on the saved preference when the app starts.
        // This ensures the language is correct from the very beginning.
        val languageTag = userPrefsRepo.language.value
        LocaleHelper.updateAppLocale(languageTag)
    }
}
