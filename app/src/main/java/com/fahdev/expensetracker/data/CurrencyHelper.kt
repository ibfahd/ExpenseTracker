package com.fahdev.expensetracker.data

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * A helper object to manage currency-related logic.
 */
object CurrencyHelper {

    /**
     * Represents a currency with its code and display name.
     */
    data class CurrencyInfo(val code: String, val displayName: String)

    /**
     * Returns a list of supported currencies.
     */
    fun getAvailableCurrencies(): List<CurrencyInfo> {
        return listOf(
            CurrencyInfo("AED", "United Arab Emirates Dirham"),
            CurrencyInfo("AFN", "Afghan Afghani"),
            CurrencyInfo("ALL", "Albanian Lek"),
            CurrencyInfo("AMD", "Armenian Dram"),
            CurrencyInfo("AOA", "Angolan Kwanza"),
            CurrencyInfo("ARS", "Argentine Peso"),
            CurrencyInfo("AUD", "Australian Dollar"),
            CurrencyInfo("AZN", "Azerbaijani Manat"),
            CurrencyInfo("BAM", "Bosnia and Herzegovina Convertible Mark"),
            CurrencyInfo("BBD", "Barbadian Dollar"),
            CurrencyInfo("BDT", "Bangladeshi Taka"),
            CurrencyInfo("BGN", "Bulgarian Lev"),
            CurrencyInfo("BHD", "Bahraini Dinar"),
            CurrencyInfo("BIF", "Burundian Franc"),
            CurrencyInfo("BND", "Brunei Dollar"),
            CurrencyInfo("BOB", "Bolivian Boliviano"),
            CurrencyInfo("BRL", "Brazilian Real"),
            CurrencyInfo("BSD", "Bahamian Dollar"),
            CurrencyInfo("BTN", "Bhutanese Ngultrum"),
            CurrencyInfo("BWP", "Botswanan Pula"),
            CurrencyInfo("BYN", "Belarusian Ruble"),
            CurrencyInfo("BZD", "Belize Dollar"),
            CurrencyInfo("CAD", "Canadian Dollar"),
            CurrencyInfo("CDF", "Congolese Franc"),
            CurrencyInfo("CHF", "Swiss Franc"),
            CurrencyInfo("CLP", "Chilean Peso"),
            CurrencyInfo("CNY", "Chinese Yuan"),
            CurrencyInfo("COP", "Colombian Peso"),
            CurrencyInfo("CRC", "Costa Rican Colón"),
            CurrencyInfo("CUP", "Cuban Peso"),
            CurrencyInfo("CVE", "Cape Verdean Escudo"),
            CurrencyInfo("CZK", "Czech Koruna"),
            CurrencyInfo("DJF", "Djiboutian Franc"),
            CurrencyInfo("DKK", "Danish Krone"),
            CurrencyInfo("DOP", "Dominican Peso"),
            CurrencyInfo("DZD", "Algerian Dinar"),
            CurrencyInfo("EGP", "Egyptian Pound"),
            CurrencyInfo("ERN", "Eritrean Nakfa"),
            CurrencyInfo("ETB", "Ethiopian Birr"),
            CurrencyInfo("EUR", "Euro"),
            CurrencyInfo("FJD", "Fijian Dollar"),
            CurrencyInfo("GBP", "British Pound Sterling"),
            CurrencyInfo("GEL", "Georgian Lari"),
            CurrencyInfo("GHS", "Ghanaian Cedi"),
            CurrencyInfo("GMD", "Gambian Dalasi"),
            CurrencyInfo("GNF", "Guinean Franc"),
            CurrencyInfo("GTQ", "Guatemalan Quetzal"),
            CurrencyInfo("GYD", "Guyanese Dollar"),
            CurrencyInfo("HKD", "Hong Kong Dollar"),
            CurrencyInfo("HNL", "Honduran Lempira"),
            CurrencyInfo("HRK", "Croatian Kuna"),
            CurrencyInfo("HTG", "Haitian Gourde"),
            CurrencyInfo("HUF", "Hungarian Forint"),
            CurrencyInfo("IDR", "Indonesian Rupiah"),
            CurrencyInfo("ILS", "Israeli New Shekel"),
            CurrencyInfo("INR", "Indian Rupee"),
            CurrencyInfo("IQD", "Iraqi Dinar"),
            CurrencyInfo("IRR", "Iranian Rial"),
            CurrencyInfo("ISK", "Icelandic Króna"),
            CurrencyInfo("JMD", "Jamaican Dollar"),
            CurrencyInfo("JOD", "Jordanian Dinar"),
            CurrencyInfo("JPY", "Japanese Yen"),
            CurrencyInfo("KES", "Kenyan Shilling"),
            CurrencyInfo("KGS", "Kyrgyzstani Som"),
            CurrencyInfo("KHR", "Cambodian Riel"),
            CurrencyInfo("KMF", "Comorian Franc"),
            CurrencyInfo("KPW", "North Korean Won"),
            CurrencyInfo("KRW", "South Korean Won"),
            CurrencyInfo("KWD", "Kuwaiti Dinar"),
            CurrencyInfo("KZT", "Kazakhstani Tenge"),
            CurrencyInfo("LAK", "Lao Kip"),
            CurrencyInfo("LBP", "Lebanese Pound"),
            CurrencyInfo("LKR", "Sri Lankan Rupee"),
            CurrencyInfo("LRD", "Liberian Dollar"),
            CurrencyInfo("LSL", "Lesotho Loti"),
            CurrencyInfo("LYD", "Libyan Dinar"),
            CurrencyInfo("MAD", "Moroccan Dirham"),
            CurrencyInfo("MDL", "Moldovan Leu"),
            CurrencyInfo("MGA", "Malagasy Ariary"),
            CurrencyInfo("MKD", "North Macedonian Denar"),
            CurrencyInfo("MMK", "Myanmar Kyat"),
            CurrencyInfo("MNT", "Mongolian Tugrik"),
            CurrencyInfo("MRU", "Mauritanian Ouguiya"),
            CurrencyInfo("MUR", "Mauritian Rupee"),
            CurrencyInfo("MVR", "Maldivian Rufiyaa"),
            CurrencyInfo("MWK", "Malawian Kwacha"),
            CurrencyInfo("MXN", "Mexican Peso"),
            CurrencyInfo("MYR", "Malaysian Ringgit"),
            CurrencyInfo("MZN", "Mozambican Metical"),
            CurrencyInfo("NAD", "Namibian Dollar"),
            CurrencyInfo("NGN", "Nigerian Naira"),
            CurrencyInfo("NIO", "Nicaraguan Córdoba"),
            CurrencyInfo("NOK", "Norwegian Krone"),
            CurrencyInfo("NPR", "Nepalese Rupee"),
            CurrencyInfo("NZD", "New Zealand Dollar"),
            CurrencyInfo("OMR", "Omani Rial"),
            CurrencyInfo("PAB", "Panamanian Balboa"),
            CurrencyInfo("PEN", "Peruvian Sol"),
            CurrencyInfo("PGK", "Papua New Guinean Kina"),
            CurrencyInfo("PHP", "Philippine Peso"),
            CurrencyInfo("PKR", "Pakistani Rupee"),
            CurrencyInfo("PLN", "Polish Złoty"),
            CurrencyInfo("PYG", "Paraguayan Guarani"),
            CurrencyInfo("QAR", "Qatari Riyal"),
            CurrencyInfo("RON", "Romanian Leu"),
            CurrencyInfo("RSD", "Serbian Dinar"),
            CurrencyInfo("RUB", "Russian Ruble"),
            CurrencyInfo("RWF", "Rwandan Franc"),
            CurrencyInfo("SAR", "Saudi Riyal"),
            CurrencyInfo("SBD", "Solomon Islands Dollar"),
            CurrencyInfo("SCR", "Seychellois Rupee"),
            CurrencyInfo("SDG", "Sudanese Pound"),
            CurrencyInfo("SEK", "Swedish Krona"),
            CurrencyInfo("SGD", "Singapore Dollar"),
            CurrencyInfo("SLE", "Sierra Leonean Leone"),
            CurrencyInfo("SOS", "Somali Shilling"),
            CurrencyInfo("SRD", "Surinamese Dollar"),
            CurrencyInfo("SSP", "South Sudanese Pound"),
            CurrencyInfo("STN", "São Tomé and Príncipe Dobra"),
            CurrencyInfo("SYP", "Syrian Pound"),
            CurrencyInfo("SZL", "Swazi Lilangeni"),
            CurrencyInfo("THB", "Thai Baht"),
            CurrencyInfo("TJS", "Tajikistani Somoni"),
            CurrencyInfo("TMT", "Turkmenistani Manat"),
            CurrencyInfo("TND", "Tunisian Dinar"),
            CurrencyInfo("TOP", "Tongan Pa'anga"),
            CurrencyInfo("TRY", "Turkish Lira"),
            CurrencyInfo("TTD", "Trinidad and Tobago Dollar"),
            CurrencyInfo("TWD", "Taiwan Dollar"),
            CurrencyInfo("TZS", "Tanzanian Shilling"),
            CurrencyInfo("UAH", "Ukrainian Hryvnia"),
            CurrencyInfo("UGX", "Ugandan Shilling"),
            CurrencyInfo("USD", "United States Dollar"),
            CurrencyInfo("UYU", "Uruguayan Peso"),
            CurrencyInfo("UZS", "Uzbekistani Som"),
            CurrencyInfo("VES", "Venezuelan Bolívar"),
            CurrencyInfo("VND", "Vietnamese Dong"),
            CurrencyInfo("VUV", "Vanuatu Vatu"),
            CurrencyInfo("WST", "Samoan Tala"),
            CurrencyInfo("XAF", "Central African CFA Franc"),
            CurrencyInfo("XCD", "East Caribbean Dollar"),
            CurrencyInfo("XOF", "West African CFA Franc"),
            CurrencyInfo("YER", "Yemeni Rial"),
            CurrencyInfo("ZAR", "South African Rand"),
            CurrencyInfo("ZMW", "Zambian Kwacha"),
            CurrencyInfo("ZWL", "Zimbabwean Dollar")
        )
    }

    /**
     * Creates a NumberFormat instance for a given currency code.
     *
     * @param currencyCode The ISO 4217 code for the currency (e.g., "USD").
     * @return A NumberFormat instance configured for the specified currency.
     */
    fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        return try {
            val currency = Currency.getInstance(currencyCode)
            // Create a locale that's appropriate for the currency formatting.
            // This is a simple approach; more complex logic might be needed for perfect locale matching.
            val locale = Locale.getAvailableLocales().find {
                try {
                    Currency.getInstance(it) == currency
                } catch (_: Exception) {
                    false
                }
            } ?: Locale.getDefault() // Fallback to default locale

            (NumberFormat.getCurrencyInstance(locale) as NumberFormat).apply {
                this.currency = currency
            }
        } catch (_: IllegalArgumentException) {
            // Fallback to default if the currency code is invalid
            NumberFormat.getCurrencyInstance(Locale.getDefault())
        }
    }
}
