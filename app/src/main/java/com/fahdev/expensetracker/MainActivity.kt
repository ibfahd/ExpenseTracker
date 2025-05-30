package com.fahdev.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.*
import android.app.Application
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.MenuAnchorType
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Supplier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                ExpenseTrackerApp(expenseViewModel = expenseViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(expenseViewModel: ExpenseViewModel) {
    val context = LocalContext.current

    val filteredExpenses by expenseViewModel.filteredExpenses.collectAsState(initial = emptyList())
    val totalFilteredExpenses by expenseViewModel.totalFilteredExpenses.collectAsState(initial = 0.0)

    val selectedStartDate by expenseViewModel.selectedStartDate.collectAsState(initial = null)
    val selectedEndDate by expenseViewModel.selectedEndDate.collectAsState(initial = null)
    val selectedCategoryId by expenseViewModel.selectedCategoryId.collectAsState(initial = null)
    val selectedSupplierId by expenseViewModel.selectedSupplierId.collectAsState(initial = null)

    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())

    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, CategoryManagementActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = context.getString(R.string.manage_categories),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SupplierManagementActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = context.getString(R.string.manage_suppliers),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = context.getString(R.string.filter_expenses),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddExpenseActivity::class.java)
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, context.getString(R.string.add_new_expense))
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ExpenseSummaryCard(totalAmount = totalFilteredExpenses)

            Spacer(Modifier.height(8.dp))

            FilterStatusRow(
                selectedStartDate = selectedStartDate,
                selectedEndDate = selectedEndDate,
                selectedCategory = allCategories.find { it.id == selectedCategoryId },
                selectedSupplier = allSuppliers.find { it.id == selectedSupplierId },
                onResetFilters = { expenseViewModel.resetFilters() }
            )

            Spacer(Modifier.height(8.dp))

            if (filteredExpenses.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_expenses),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredExpenses) { expenseWithDetails ->
                        ExpenseItem(
                            expenseWithDetails = expenseWithDetails,
                            onExpenseClick = { clickedExpenseId ->
                                val intent = Intent(context, EditExpenseActivity::class.java).apply {
                                    putExtra("EXPENSE_ID", clickedExpenseId)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
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

@Composable
fun ExpenseSummaryCard(totalAmount: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = LocalContext.current.getString(R.string.total_expenses),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = "$${"%.2f".format(totalAmount)}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 48.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExpenseItem(
    expenseWithDetails: ExpenseWithDetails,
    onExpenseClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onExpenseClick(expenseWithDetails.expense.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expenseWithDetails.productWithCategory.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$${"%.2f".format(expenseWithDetails.expense.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expenseWithDetails.productWithCategory.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = LocalContext.current.getString(R.string.from_supplier, expenseWithDetails.supplier.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("MMM dd,yyyy - HH:mm", Locale.getDefault()).format(Date(expenseWithDetails.expense.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FilterStatusRow(
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    selectedCategory: Category?,
    selectedSupplier: Supplier?,
    onResetFilters: () -> Unit
) {
    val context = LocalContext.current
    val filtersActive = selectedStartDate != null || selectedEndDate != null || selectedCategory != null || selectedSupplier != null

    if (filtersActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.active_filters),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    if (selectedStartDate != null && selectedEndDate != null) {
                        val start = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedStartDate))
                        val end = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedEndDate))
                        Text(
                            text = context.getString(R.string.date_range, start, end),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (selectedCategory != null) {
                        Text(
                            text = context.getString(R.string.category_filter, selectedCategory.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (selectedSupplier != null) {
                        Text(
                            text = context.getString(R.string.supplier_filter, selectedSupplier.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(onClick = onResetFilters) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = context.getString(R.string.clear_filters),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    expenseViewModel: ExpenseViewModel,
    allCategories: List<Category>,
    allSuppliers: List<Supplier>,
    onDismiss: () -> Unit
) {
    val selectedStartDate by expenseViewModel.selectedStartDate.collectAsState(initial = null)
    val selectedEndDate by expenseViewModel.selectedEndDate.collectAsState(initial = null)
    val selectedCategoryId by expenseViewModel.selectedCategoryId.collectAsState(initial = null)
    val selectedSupplierId by expenseViewModel.selectedSupplierId.collectAsState(initial = null)

    var showDatePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedStartDate,
        initialSelectedEndDateMillis = selectedEndDate
    )
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.filter_expenses)) },
        text = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(context.getString(R.string.filter_by_date), style = MaterialTheme.typography.titleSmall)
                DateFilterDropdown(
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onDateOptionSelected = { option ->
                        if (option == "All") {
                            expenseViewModel.resetFilters()
                        } else {
                            expenseViewModel.setDateRangeFilter(option)
                        }
                        onDismiss()
                    }
                )
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(context.getString(R.string.select_custom_date_range))
                }
                Spacer(Modifier.height(8.dp))

                Text(context.getString(R.string.filter_by_category), style = MaterialTheme.typography.titleSmall)
                CategoryFilterDropdown(
                    allCategories = allCategories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { categoryId ->
                        expenseViewModel.setCategoryFilter(categoryId)
                    }
                )
                Spacer(Modifier.height(8.dp))

                Text(context.getString(R.string.filter_by_supplier), style = MaterialTheme.typography.titleSmall)
                SupplierFilterDropdown(
                    allSuppliers = allSuppliers,
                    selectedSupplierId = selectedSupplierId,
                    onSupplierSelected = { supplierId ->
                        expenseViewModel.setSupplierFilter(supplierId)
                    }
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        expenseViewModel.resetFilters()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(context.getString(R.string.clear_all_filters))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(context.getString(R.string.close))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateRangePickerState.selectedStartDateMillis?.let { startDate ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = startDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val normalizedStartDate = calendar.timeInMillis

                            val endDate = dateRangePickerState.selectedEndDateMillis ?: startDate
                            calendar.timeInMillis = endDate
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            calendar.set(Calendar.MILLISECOND, 999)
                            val normalizedEndDate = calendar.timeInMillis

                            expenseViewModel.setCustomDateRangeFilter(normalizedStartDate, normalizedEndDate)
                        } ?: run {
                            expenseViewModel.setCustomDateRangeFilter(null, null)
                        }
                        showDatePicker = false
                        onDismiss()
                    }
                ) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterDropdown(
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    onDateOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateOptions = listOf(
        "ThisMonth" to context.getString(R.string.this_month),
        "Last7Days" to context.getString(R.string.last_7_days),
        "LastMonth" to context.getString(R.string.last_month),
        "ThisYear" to context.getString(R.string.this_year),
        "All" to context.getString(R.string.all_time)
    )
    val currentOption = dateOptions.find { (key, _) ->
        when (key) {
            "ThisMonth" -> selectedStartDate == getStartOfMonth(System.currentTimeMillis()) && selectedEndDate == System.currentTimeMillis()
            "Last7Days" -> selectedStartDate == (System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) && selectedEndDate == System.currentTimeMillis()
            "LastMonth" -> selectedStartDate == getStartOfLastMonth() && selectedEndDate == getEndOfLastMonth()
            "ThisYear" -> selectedStartDate == getStartOfYear() && selectedEndDate == System.currentTimeMillis()
            "All" -> selectedStartDate == null && selectedEndDate == null
            else -> false
        }
    }?.second ?: context.getString(R.string.select_date_range)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = currentOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.date_range_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dateOptions.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onDateOptionSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    allCategories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedCategoryName = allCategories.find { it.id == selectedCategoryId }?.name ?: context.getString(R.string.all_categories)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedCategoryName,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.all_categories)) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            allCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFilterDropdown(
    allSuppliers: List<Supplier>,
    selectedSupplierId: Int?,
    onSupplierSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedSupplierName = allSuppliers.find { it.id == selectedSupplierId }?.name ?: context.getString(R.string.all_suppliers)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedSupplierName,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.supplier)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.all_suppliers)) },
                onClick = {
                    onSupplierSelected(null)
                    expanded = false
                }
            )
            allSuppliers.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplier.name) },
                    onClick = {
                        onSupplierSelected(supplier.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun getStartOfMonth(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun getStartOfLastMonth(): Long {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun getEndOfLastMonth(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.DAY_OF_MONTH, -1)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

private fun getStartOfYear(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

@Preview(showBackground = true)
@Composable
fun ExpenseTrackerAppPreview() {
    ExpenseTrackerTheme {
        ExpenseTrackerApp(expenseViewModel = ExpenseViewModel(Application()))
    }
}