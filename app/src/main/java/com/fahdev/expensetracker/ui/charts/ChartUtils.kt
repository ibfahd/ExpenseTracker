package com.fahdev.expensetracker.ui.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.fahdev.expensetracker.data.CategorySpending
import com.fahdev.expensetracker.data.ExpenseDao
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.Pie
import java.text.SimpleDateFormat
import java.util.*

object ChartUtils {

    val categoryColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFF84CC16), // Lime
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6)  // Purple
    )

    val supplierColors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF84CC16)  // Lime
    )

    fun transformCategoryDataToPie(data: List<CategorySpending>): List<Pie> {
        return data.take(10).mapIndexed { index, spending ->
            Pie(
                label = spending.categoryName,
                data = spending.totalAmount, // Corrected: use 'data' instead of 'value'
                color = categoryColors[index % categoryColors.size]
            )
        }
    }

    fun transformTrendDataToLine(
        data: List<ExpenseDao.TrendDataPoint>,
        color: Color,
        label: String = "Spending"
    ): Line {
        return Line(
            label = label,
            values = data.map { it.amount }, // Corrected: map to list of Double
            color = SolidColor(color), // Corrected: wrap Color in SolidColor (Brush)
            // Other parameters have defaults, so no need to set them unless required
        )
    }

    fun formatDateForPeriod(timestamp: Long, period: TrendPeriod): String {
        val date = Date(timestamp)
        return when (period) {
            TrendPeriod.DAILY -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            TrendPeriod.WEEKLY -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            TrendPeriod.MONTHLY -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    fun getOptimalTrendPeriod(startDate: Long?, endDate: Long?): TrendPeriod {
        if (startDate == null || endDate == null) return TrendPeriod.MONTHLY

        val daysDiff = (endDate - startDate) / (24 * 60 * 60 * 1000)
        return when {
            daysDiff <= 31 -> TrendPeriod.DAILY
            daysDiff <= 90 -> TrendPeriod.WEEKLY
            else -> TrendPeriod.MONTHLY
        }
    }

    fun calculatePercentage(value: Double, total: Double): Double {
        return if (total > 0) (value / total) * 100 else 0.0
    }
}

enum class TrendPeriod {
    DAILY, WEEKLY, MONTHLY
}