package com.fahdev.expensetracker.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A utility object to provide predefined lists of icons and colors for customization.
 */
object IconAndColorUtils {

    // Data class to hold both the string name and the ImageVector for an icon
    data class IconInfo(val name: String, val icon: ImageVector)

    // The master list of all available icons for categories, suppliers, etc.
    val iconList = listOf(
        IconInfo("Fastfood", Icons.Default.Fastfood),
        IconInfo("ShoppingCart", Icons.Default.ShoppingCart),
        IconInfo("Home", Icons.Default.Home),
        IconInfo("Commute", Icons.Default.Commute),
        IconInfo("ReceiptLong", Icons.AutoMirrored.Filled.ReceiptLong),
        IconInfo("LocalHospital", Icons.Default.LocalHospital),
        IconInfo("Spa", Icons.Default.Spa),
        IconInfo("Theaters", Icons.Default.Theaters),
        IconInfo("Flight", Icons.Default.Flight),
        IconInfo("School", Icons.Default.School),
        IconInfo("Savings", Icons.Default.Savings),
        IconInfo("Category", Icons.Default.Category),
        IconInfo("Store", Icons.Default.Store),
        IconInfo("LocalGasStation", Icons.Default.LocalGasStation),
        IconInfo("LocalGroceryStore", Icons.Default.LocalGroceryStore),
        IconInfo("Restaurant", Icons.Default.Restaurant),
        IconInfo("Build", Icons.Default.Build),
        IconInfo("CreditCard", Icons.Default.CreditCard),
        IconInfo("Pets", Icons.Default.Pets)
    )

    // A map to quickly look up an ImageVector by its string name
    val iconMap: Map<String, ImageVector> = iconList.associate { it.name to it.icon }

    // Data class to hold both the Color object and its hex string representation
    data class ColorInfo(val color: Color, val hex: String)

    // The master list of all available colors for customization
    val colorList = listOf(
        ColorInfo(Color(0xFFF44336), "#F44336"), // Red
        ColorInfo(Color(0xFFE91E63), "#E91E63"), // Pink
        ColorInfo(Color(0xFF9C27B0), "#9C27B0"), // Purple
        ColorInfo(Color(0xFF673AB7), "#673AB7"), // Deep Purple
        ColorInfo(Color(0xFF3F51B5), "#3F51B5"), // Indigo
        ColorInfo(Color(0xFF2196F3), "#2196F3"), // Blue
        ColorInfo(Color(0xFF00BCD4), "#00BCD4"), // Cyan
        ColorInfo(Color(0xFF009688), "#009688"), // Teal
        ColorInfo(Color(0xFF4CAF50), "#4CAF50"), // Green
        ColorInfo(Color(0xFF8BC34A), "#8BC34A"), // Light Green
        ColorInfo(Color(0xFFFFC107), "#FFC107"), // Amber
        ColorInfo(Color(0xFFFF9800), "#FF9800"), // Orange
        ColorInfo(Color(0xFF795548), "#795548"), // Brown
        ColorInfo(Color(0xFF9E9E9E), "#9E9E9E"), // Grey
        ColorInfo(Color(0xFF607D8B), "#607D8B")  // Blue Grey
    )

    // A map to quickly look up a Color object by its hex string
    val colorMap: Map<String, Color> = colorList.associate { it.hex to it.color }
}
