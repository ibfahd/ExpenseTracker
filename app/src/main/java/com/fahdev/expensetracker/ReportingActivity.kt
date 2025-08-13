package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.*
import com.fahdev.expensetracker.ui.components.FilterDialog
import com.fahdev.expensetracker.ui.components.FilterStatusRow
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat

@AndroidEntryPoint
class ReportingActivity : AppCompatActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                ReportingScreenWithTabs(expenseViewModel = expenseViewModel)
            }
        }
    }
}

enum class ReportTab(val titleResId: Int, val icon: ImageVector) {
    SUMMARY(R.string.report_tab_summary, Icons.Filled.Info),
    PRODUCT_DETAILS(R.string.report_tab_product_details, Icons.AutoMirrored.Filled.ListAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreenWithTabs(expenseViewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = ReportTab.entries.toTypedArray()
    val userPrefsRepo = remember { UserPreferencesRepository.getInstance(context.applicationContext) }
    val currencyCode by userPrefsRepo.currencyCode.collectAsState()
    val currencyFormatter = remember(currencyCode) { CurrencyHelper.getCurrencyFormatter(currencyCode) }
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_reporting)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = context.getString(R.string.filter_expenses), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FilterStatusRow(
                selectedStartDate = expenseViewModel.selectedStartDate.collectAsState().value,
                selectedEndDate = expenseViewModel.selectedEndDate.collectAsState().value,
                selectedCategory = allCategories.find { it.id == expenseViewModel.selectedCategoryId.collectAsState().value },
                selectedSupplier = allSuppliers.find { it.id == expenseViewModel.selectedSupplierId.collectAsState().value },
                onResetFilters = { expenseViewModel.resetFilters() }
            )
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, tab ->
                    LeadingIconTab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(stringResource(tab.titleResId)) },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.titleResId)) }
                    )
                }
            }
            when (tabs[selectedTabIndex]) {
                ReportTab.SUMMARY -> SummaryReportContent(expenseViewModel, currencyFormatter)
                ReportTab.PRODUCT_DETAILS -> ProductDetailsReportContent(expenseViewModel, currencyFormatter)
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                expenseViewModel = expenseViewModel,
                allCategories = allCategories,
                allSuppliers = allSuppliers,
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
fun SummaryReportContent(expenseViewModel: ExpenseViewModel, currencyFormatter: NumberFormat) {
    val spendingByCategory by expenseViewModel.spendingByCategoryFiltered.collectAsState()
    val spendingBySupplier by expenseViewModel.spendingBySupplierFiltered.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (spendingByCategory.isNotEmpty()) {
            item {
                ReportSectionCard(title = stringResource(R.string.report_section_spending_by_category_period)) {
                    SimpleBarChart(
                        data = spendingByCategory.take(5).map { it.categoryName to it.totalAmount },
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
        if (spendingBySupplier.isNotEmpty()) {
            item {
                ReportSectionCard(title = stringResource(R.string.report_section_spending_by_supplier_period)) {
                    SimpleBarChart(
                        data = spendingBySupplier.take(5).map { it.supplierName to it.totalAmount },
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
        if (spendingByCategory.isEmpty() && spendingBySupplier.isEmpty()) {
            item {
                ReportSectionCard(title = stringResource(R.string.report_section_summary)) {
                    Text(stringResource(R.string.report_no_data_in_period), modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val maxValue = data.map { it.second }.maxOrNull() ?: 0.0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.forEach { (label, value) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currencyFormatter.format(value),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                val barWidthFraction = if (maxValue > 0) (value / maxValue).toFloat() else 0f
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(fraction = barWidthFraction)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

@Composable
fun ReportSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun ProductDetailsReportContent(expenseViewModel: ExpenseViewModel, currencyFormatter: NumberFormat) {
    val productReportDetails by expenseViewModel.productReportDetails.collectAsState()
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (productReportDetails.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        stringResource(R.string.report_no_product_data),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            val groupedProducts = productReportDetails.groupBy { it.categoryName }
            groupedProducts.forEach { (categoryName, productsInCategory) ->
                item {
                    CategoryAccordionHeader(
                        categoryName = categoryName,
                        isExpanded = expandedCategories[categoryName] == true,
                        onToggle = {
                            expandedCategories[categoryName] = expandedCategories[categoryName] != true
                        }
                    )
                }
                item {
                    AnimatedVisibility(visible = expandedCategories[categoryName] == true) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            productsInCategory.forEach { productDetail ->
                                ProductReportItem(productDetail = productDetail, currencyFormatter = currencyFormatter)
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProductReportItem(productDetail: ProductReportDetail, currencyFormatter: NumberFormat) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Text(productDetail.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        ProductStatRow(
            label = stringResource(R.string.report_product_total_spent),
            value = currencyFormatter.format(productDetail.totalAmountSpent ?: 0.0)
        )
        ProductStatRow(
            label = stringResource(R.string.report_product_lowest_price),
            value = currencyFormatter.format(productDetail.lowestTransactionAmount ?: 0.0)
        )
        ProductStatRow(
            label = stringResource(R.string.report_product_cheapest_supplier),
            value = productDetail.cheapestSupplierName ?: stringResource(R.string.report_not_available)
        )
    }
}

@Composable
fun ProductStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CategoryAccordionHeader(
    categoryName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse $categoryName" else "Expand $categoryName",
            modifier = Modifier.size(30.dp).rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}