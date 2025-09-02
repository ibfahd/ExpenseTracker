package com.fahdev.expensetracker.ui.reporting

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.ChartColors
import com.fahdev.expensetracker.ExpenseViewModel
import com.fahdev.expensetracker.R
import com.fahdev.expensetracker.data.CategorySpending
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.SupplierSpending
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

// Constants for better maintainability
private const val CHART_ANIMATION_DURATION = 2000
private const val CHART_ANIMATION_DELAY = 1000L
private const val BAR_ANIMATION_DURATION = 1000
private const val BAR_ANIMATION_DELAY = 100L
private const val MAX_DISPLAY_ITEMS = 6
private val CHART_BAR_HEIGHT = 12.dp
private val CHART_BAR_CORNER_RADIUS = 6.dp
private val CHART_HEIGHT = 250.dp
private val PIE_CHART_HEIGHT = 300.dp
private const val MINIMUM_DAY_COUNT_FOR_MONTHLY_VIEW = 28L
private const val MAXIMUM_DAY_COUNT_FOR_MONTHLY_VIEW = 31L

@Composable
fun ChartsReportContent(
    expenseViewModel: ExpenseViewModel,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val spendingByCategory by expenseViewModel.spendingByCategoryFiltered.collectAsState()
    val spendingBySupplier by expenseViewModel.spendingBySupplierFiltered.collectAsState()
    val selectedStartDate by expenseViewModel.selectedStartDate.collectAsState()
    val selectedEndDate by expenseViewModel.selectedEndDate.collectAsState()

    val isMonthlyView = remember(selectedStartDate, selectedEndDate) {
        calculateIsMonthlyView(selectedStartDate, selectedEndDate)
    }

    val trendData by if (isMonthlyView) {
        expenseViewModel.spendingByDay.collectAsState(initial = emptyList())
    } else {
        expenseViewModel.spendingByMonth.collectAsState(initial = emptyList())
    }

    // Calculate chart title based on the view
    val chartTitle = remember(isMonthlyView, selectedStartDate) {
        if (isMonthlyView) {
            val cal = Calendar.getInstance().apply {
                selectedStartDate?.let { timeInMillis = it }
            }
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
            "Daily Spending Trend for $monthName"
        } else {
            "Monthly Spending Trend"
        }
    }

    val hasData = spendingByCategory.isNotEmpty() ||
            spendingBySupplier.isNotEmpty() ||
            trendData.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (trendData.isNotEmpty()) {
            ReportSectionCard(title = chartTitle) {
                SpendingTrendChart(
                    data = trendData,
                    currencyFormatter = currencyFormatter,
                    isMonthlyView = isMonthlyView
                )
            }
        }

        if (spendingByCategory.isNotEmpty()) {
            ReportSectionCard(title = "Category Spending Distribution") {
                CategorySpendingPieChart(
                    data = spendingByCategory.take(MAX_DISPLAY_ITEMS),
                    currencyFormatter = currencyFormatter
                )
            }
        }

        if (spendingByCategory.isNotEmpty()) {
            ReportSectionCard(title = stringResource(R.string.report_category_spending)) {
                CategorySpendingBarChart(
                    data = spendingByCategory.take(MAX_DISPLAY_ITEMS),
                    currencyFormatter = currencyFormatter
                )
            }
        }

        if (spendingBySupplier.isNotEmpty()) {
            ReportSectionCard(title = stringResource(R.string.report_supplier_spending)) {
                SupplierSpendingChart(
                    data = spendingBySupplier.take(MAX_DISPLAY_ITEMS),
                    currencyFormatter = currencyFormatter
                )
            }
        }

        if (!hasData) {
            ReportSectionCard(title = stringResource(R.string.report_section_summary)) {
                Text(
                    text = stringResource(R.string.report_no_data_in_period),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

private fun calculateIsMonthlyView(startDate: Long?, endDate: Long?): Boolean {
    if (startDate == null) return false

    val end = endDate ?: System.currentTimeMillis()
    val diffInMillis = end - startDate
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return diffInDays in MINIMUM_DAY_COUNT_FOR_MONTHLY_VIEW..MAXIMUM_DAY_COUNT_FOR_MONTHLY_VIEW
}

@Composable
fun SpendingTrendChart(
    data: List<ExpenseDao.TrendDataPoint>,
    currencyFormatter: NumberFormat,
    isMonthlyView: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    // Check if data is empty to prevent crashes
    if (data.isEmpty()) {
        Box(
            modifier = Modifier
                .height(CHART_HEIGHT)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Safely extract values from data points
    val values = remember(data) {
        data.map { it.amount }
    }

    // Create formatted labels for the x-axis
    val labels = remember(data, isMonthlyView) {
        val calendar = Calendar.getInstance()
        data.map { point ->
            calendar.timeInMillis = point.timestamp
            if (isMonthlyView) {
                // Daily view: show day of the month
                calendar.get(Calendar.DAY_OF_MONTH).toString()
            } else {
                // Monthly view: show month name
                calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
            }
        }
    }

    val lineData = remember(values, primaryColor) {
        Line(
            label = "Spending Trend",
            values = values,
            color = SolidColor(primaryColor),
            firstGradientFillColor = primaryColor.copy(alpha = 0.5f),
            secondGradientFillColor = Color.Transparent,
            strokeAnimationSpec = tween(CHART_ANIMATION_DURATION),
            gradientAnimationDelay = CHART_ANIMATION_DELAY,
            drawStyle = DrawStyle.Stroke(width = 3.dp)
        )
    }

    val scrollState = rememberScrollState()

    // Auto-scroll to end for better UX
    LaunchedEffect(data.size) {
        if (data.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.report_spending_patterns_over_time),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .height(CHART_HEIGHT)
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Use a fixed width based on data points with a minimum width
            val chartWidth = remember(data.size) {
                // Ensure we have a reasonable minimum width
                val minWidth = maxOf(data.size * 80, 1) // Avoid zero width
                minWidth.dp
            }

            val yAxisLabelCount = min(5, data.size.coerceAtLeast(2))

            LineChart(
                modifier = Modifier
                    .width(chartWidth)
                    .fillMaxHeight(),
                data = listOf(lineData),
                gridProperties = GridProperties(
                    enabled = true,
                    xAxisProperties = GridProperties.AxisProperties(
                        color = SolidColor(outlineColor.copy(alpha = 0.3f))
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        color = SolidColor(outlineColor.copy(alpha = 0.3f))
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurfaceColor),
                    count = IndicatorCount.CountBased(count = yAxisLabelCount),
                    contentBuilder = { value -> currencyFormatter.format(value) }
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = labels, // Use our formatted labels
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurfaceColor)
                ),
                popupProperties = PopupProperties(
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurfaceColor),
                    containerColor = surfaceVariantColor,
                    contentBuilder = { index, _, value ->
                        // Create a more informative popup content
                        "${labels.getOrNull(index)}: ${currencyFormatter.format(value)}"
                    }
                )
            )
        }
    }
}

@Composable
fun CategorySpendingPieChart(
    data: List<CategorySpending>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Log.d("PieChartData", "Data: $data")
    if (data.isEmpty()) {
        Box(
            modifier = Modifier
                .height(PIE_CHART_HEIGHT)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val pieData = remember(data) {
        data.mapIndexed { index, spending ->
            Pie(
                label = "${spending.categoryName}\n${currencyFormatter.format(spending.totalAmount)}",
                data = spending.totalAmount,
                color = ChartColors.categoryColors[index % ChartColors.categoryColors.size]
            )
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.report_spending_distribution_by_category),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        PieChart(
            modifier = Modifier
                .height(PIE_CHART_HEIGHT)
                .align(Alignment.CenterHorizontally),
            data = pieData
        )
    }
}

@Composable
fun CategorySpendingBarChart(
    data: List<CategorySpending>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(data) { data.maxOfOrNull { it.totalAmount } ?: 0.0 }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.report_category_spending_breakdown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.forEachIndexed { index, spending ->
                SpendingBarItem(
                    name = spending.categoryName,
                    amount = spending.totalAmount,
                    maxValue = maxValue,
                    color = if (ChartColors.categoryColors.isNotEmpty()) {
                        ChartColors.categoryColors[index % ChartColors.categoryColors.size]
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    currencyFormatter = currencyFormatter,
                    animationLabel = "category_bar_${spending.categoryName}"
                )
            }
        }
    }
}

@Composable
fun SupplierSpendingChart(
    data: List<SupplierSpending>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(data) { data.maxOfOrNull { it.totalAmount } ?: 0.0 }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.report_supplier_spending_comparison),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.forEachIndexed { index, spending ->
                SpendingBarItem(
                    name = spending.supplierName,
                    amount = spending.totalAmount,
                    maxValue = maxValue,
                    color = if (ChartColors.supplierColors.isNotEmpty()) {
                        ChartColors.supplierColors[index % ChartColors.supplierColors.size]
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    currencyFormatter = currencyFormatter,
                    animationLabel = "supplier_bar_${spending.supplierName}"
                )
            }
        }
    }
}

@Composable
fun SpendingBarItem(
    name: String,
    amount: Double,
    maxValue: Double,
    color: Color,
    currencyFormatter: NumberFormat,
    animationLabel: String
) {
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(animationLabel) {
        animatedFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = BAR_ANIMATION_DURATION,
                delayMillis = BAR_ANIMATION_DELAY.toInt()
            )
        )
    }

    val fraction = if (maxValue > 0) (amount / maxValue).toFloat() else 0f
    val animatedWidthFraction = fraction * animatedFraction.value

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = currencyFormatter.format(amount),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_BAR_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(CHART_BAR_CORNER_RADIUS))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidthFraction)
                    .height(CHART_BAR_HEIGHT)
                    .background(color, RoundedCornerShape(CHART_BAR_CORNER_RADIUS))
            )
        }
    }
}
