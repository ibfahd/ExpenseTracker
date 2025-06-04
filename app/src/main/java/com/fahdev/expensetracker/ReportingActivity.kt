package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import java.text.NumberFormat
import java.util.Locale

class ReportingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                ReportingScreen(expenseViewModel = expenseViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(expenseViewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    val totalExpensesAllTime by expenseViewModel.totalExpensesAllTime.collectAsState()
    val averageDailyExpense by expenseViewModel.averageDailyExpense.collectAsState()
    val averageMonthlyExpense by expenseViewModel.averageMonthlyExpense.collectAsState()
    val totalTransactionCount by expenseViewModel.totalTransactionCount.collectAsState()
    val spendingByCategory by expenseViewModel.spendingByCategory.collectAsState()
    val spendingBySupplier by expenseViewModel.spendingBySupplier.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_reporting)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReportSectionCard(title = stringResource(R.string.report_section_summary)) {
                    StatItem(label = stringResource(R.string.report_total_expenses_all_time), value = currencyFormat.format(totalExpensesAllTime))
                    StatItem(label = stringResource(R.string.report_average_daily_expense), value = currencyFormat.format(averageDailyExpense))
                    StatItem(label = stringResource(R.string.report_average_monthly_expense), value = currencyFormat.format(averageMonthlyExpense))
                    StatItem(label = stringResource(R.string.report_total_transactions), value = totalTransactionCount.toString())
                }
            }

            if (spendingByCategory.isNotEmpty()) {
                item {
                    ReportSectionCard(title = stringResource(R.string.report_section_spending_by_category)) {
                        spendingByCategory.forEach { categoryData ->
                            StatItem(label = categoryData.categoryName, value = currencyFormat.format(categoryData.totalAmount))
                        }
                    }
                }
            } else {
                item {
                    ReportSectionCard(title = stringResource(R.string.report_section_spending_by_category)) {
                        Text(stringResource(R.string.report_no_data_categories), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            if (spendingBySupplier.isNotEmpty()) {
                item {
                    ReportSectionCard(title = stringResource(R.string.report_section_spending_by_supplier)) {
                        spendingBySupplier.forEach { supplierData ->
                            StatItem(label = supplierData.supplierName, value = currencyFormat.format(supplierData.totalAmount))
                        }
                    }
                }
            } else {
                item {
                    ReportSectionCard(title = stringResource(R.string.report_section_spending_by_supplier)) {
                        Text(stringResource(R.string.report_no_data_suppliers), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
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
            // The 'content' lambda is invoked here, within the scope of this Column.
            // So, 'this' inside the content lambda will refer to ColumnScope.
            this.content()
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false) // Prevent label from taking too much space if value is short
        )
        Spacer(Modifier.width(16.dp)) // Add some space between label and value
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportingScreenPreview() {
    ExpenseTrackerTheme {
        val context = LocalContext.current
        ReportingScreen(expenseViewModel = ExpenseViewModel(context.applicationContext as Application))
    }
}
