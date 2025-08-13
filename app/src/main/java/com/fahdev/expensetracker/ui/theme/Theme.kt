package com.fahdev.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fahdev.expensetracker.data.UserPreferencesRepository

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAppColor,
    onPrimary = OnPrimaryAppColor,
    secondary = SecondaryAppColor,
    tertiary = TertiaryAppColor,
    error = ErrorAppColor, // Your custom error color
    // ...
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    // ... other colors you want to customize
    primaryContainer = SecondaryAppColor, // Using secondary for primary container
    onPrimaryContainer = OnPrimaryAppColor // Using onPrimary for text on primary container
)

@Composable
fun ExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository.getInstance(context) }
    val theme by userPreferencesRepository.theme.collectAsState()
    val cardStyle by userPreferencesRepository.cardStyle.collectAsState()

    val darkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val shapes = if (cardStyle == "rounded") RoundedShapes else SquareShapes

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}