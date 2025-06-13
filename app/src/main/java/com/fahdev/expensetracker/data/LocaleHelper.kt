package com.fahdev.expensetracker.data

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * A helper object to manage app-wide locale settings.
 */
object LocaleHelper {
    /**
     * Represents a language option for the UI.
     * @param tag The BCP-47 language tag (e.g., "en", "ar").
     * @param nativeName The name of the language in its own script (e.g., "English", "العربية").
     */
    data class Language(val tag: String, val nativeName: String)

    val supportedLanguages = listOf(
        Language("system", "System Default"),
        Language("en", "English"),
        Language("es", "Español"),
        Language("fr", "Français"),
        Language("ar", "العربية") // Added Arabic
    )

    /**
     * Sets the application's locale.
     * @param languageTag The BCP-47 language tag to apply. If "system", the app-specific
     * locale is cleared, reverting to the system's language.
     */
    fun updateAppLocale(languageTag: String) {
        val localeList = if (languageTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        // This will update the application's locale and handle configuration changes.
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
