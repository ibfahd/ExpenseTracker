package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.UserPreferencesRepository
import com.fahdev.expensetracker.ui.components.FilterDialog
import com.fahdev.expensetracker.ui.components.FilterStatusRow
import com.fahdev.expensetracker.ui.reporting.ChartsReportContent
import com.fahdev.expensetracker.ui.reporting.ProductDetailsReportContent
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

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
    CHARTS(R.string.report_tab_charts, Icons.Filled.BarChart),
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
                ReportTab.CHARTS -> ChartsReportContent(expenseViewModel, currencyFormatter)
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
