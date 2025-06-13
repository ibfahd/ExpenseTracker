package com.fahdev.expensetracker

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.UserPreferencesRepository
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Changed from ComponentActivity to AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                MainAppScreen(expenseViewModel = expenseViewModel)
            }
        }
    }
}


@Composable
fun MainAppScreen(expenseViewModel: ExpenseViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val userPrefsRepo = remember { UserPreferencesRepository.getInstance(context.applicationContext) }
    val currencyCode by userPrefsRepo.currencyCode.collectAsState()
    val currencyFormatter = remember(currencyCode) {
        CurrencyHelper.getCurrencyFormatter(currencyCode)
    }

    val navigationActions = mapOf(
        stringResource(R.string.manage_categories) to { context.startActivity(Intent(context, CategoryManagementActivity::class.java)) },
        stringResource(R.string.manage_suppliers) to { context.startActivity(Intent(context, SupplierManagementActivity::class.java)) },
        stringResource(R.string.shopping_list_title) to { context.startActivity(Intent(context, ShoppingListActivity::class.java)) },
        stringResource(R.string.title_activity_reporting) to { context.startActivity(Intent(context, ReportingActivity::class.java)) },
        stringResource(R.string.settings) to { context.startActivity(Intent(context, SettingsActivity::class.java)) },
        stringResource(R.string.about) to { context.startActivity(Intent(context, AboutActivity::class.java)) }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                onNavigate = { destinationKey ->
                    navigationActions[destinationKey]?.invoke()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        ExpenseTrackerApp(
            expenseViewModel = expenseViewModel,
            currencyFormatter = currencyFormatter,
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )
    }
}

// ... All other composable functions in this file remain the same
// The only change was MainActivity extending AppCompatActivity.
// The rest of the file is included for completeness.

data class DrawerMenuItem(val key: String, val icon: ImageVector)
@Composable
fun AppDrawerContent(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val menuItems = listOf(
        DrawerMenuItem(context.getString(R.string.manage_categories), Icons.Default.Category),
        DrawerMenuItem(context.getString(R.string.manage_suppliers), Icons.Default.LocationOn),
        DrawerMenuItem(context.getString(R.string.shopping_list_title), Icons.Default.ShoppingCart),
        DrawerMenuItem(context.getString(R.string.title_activity_reporting), Icons.Default.Assessment),
        DrawerMenuItem(context.getString(R.string.settings), Icons.Default.Settings),
        DrawerMenuItem(context.getString(R.string.about), Icons.Default.Info)
    )

    ModalDrawerSheet {
        Column {
            Text(
                stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            menuItems.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.key) },
                    label = { Text(item.key) },
                    selected = false,
                    onClick = { onNavigate(item.key) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(
    expenseViewModel: ExpenseViewModel,
    currencyFormatter: NumberFormat,
    onMenuClick: () -> Unit
) {
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
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
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
            ExpenseSummaryCard(
                totalAmount = totalFilteredExpenses,
                currencyFormatter = currencyFormatter
            )
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
                    items(filteredExpenses, key = { it.expense.id }) { expenseWithDetails ->
                        ExpenseItem(
                            expenseWithDetails = expenseWithDetails,
                            currencyFormatter = currencyFormatter,
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
fun ExpenseSummaryCard(totalAmount: Double, currencyFormatter: NumberFormat) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
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
                text = context.getString(R.string.total_expenses),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = currencyFormatter.format(totalAmount),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 48.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExpenseItem(
    expenseWithDetails: ExpenseWithDetails,
    currencyFormatter: NumberFormat,
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
                    text = currencyFormatter.format(expenseWithDetails.expense.amount),
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
                    text = stringResource(R.string.from_supplier, expenseWithDetails.supplier.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(expenseWithDetails.expense.timestamp)),
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
                    } else if (selectedStartDate != null) {
                        val start = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedStartDate))
                        Text(
                            text = "From: $start",
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
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(context.getString(R.string.filter_by_date), style = MaterialTheme.typography.titleSmall)
                DateFilterDropdown(
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onDateOptionSelected = { option ->
                        expenseViewModel.setDateRangeFilter(option)
                    }
                )
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text(context.getString(R.string.clear_all_filters))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(context.getString(R.string.apply_filters_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startDateMillis = dateRangePickerState.selectedStartDateMillis
                        var endDateMillis = dateRangePickerState.selectedEndDateMillis

                        if (startDateMillis != null && endDateMillis == null) {
                            val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.set(Calendar.MILLISECOND, 999)
                            endDateMillis = cal.timeInMillis
                        }
                        val normalizedStartDate = startDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        val normalizedEndDate = endDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        }

                        expenseViewModel.setCustomDateRangeFilter(normalizedStartDate, normalizedEndDate)
                        showDatePicker = false
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
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.padding(16.dp))
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

    val currentSelectionText = remember(selectedStartDate, selectedEndDate) {
        when {
            selectedStartDate == null && selectedEndDate == null -> context.getString(R.string.all_time)
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val startStr = selectedStartDate?.let { sdf.format(Date(it)) } ?: "N/A"
                val endStr = selectedEndDate?.let { sdf.format(Date(it)) } ?: "N/A"
                if (selectedStartDate != null && selectedEndDate != null && selectedStartDate == selectedEndDate) {
                    startStr
                } else if (selectedStartDate != null && selectedEndDate != null) {
                    "$startStr - $endStr"
                } else {
                    context.getString(R.string.select_date_range)
                }
            }
        }
    }


    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = currentSelectionText,
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

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    ExpenseTrackerTheme {
        val mockViewModel = remember {
            ExpenseViewModel(
                application = object : Application() {
                    override fun getApplicationContext(): Context = this
                }
            )
        }
        MainAppScreen(expenseViewModel = mockViewModel)
    }
}

