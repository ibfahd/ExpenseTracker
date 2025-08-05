package com.fahdev.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
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
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.lifecycle.lifecycleScope
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.UserPreferencesRepository
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.components.FilterDialog
import com.fahdev.expensetracker.ui.components.FilterStatusRow
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Add test data only in debug builds to avoid populating production releases
        if (com.fahdev.expensetracker.BuildConfig.DEBUG) {
            lifecycleScope.launch {
                // Check if data already exists to prevent adding it on every launch
                val expenseCount = expenseViewModel.totalTransactionCount.first()
                if (expenseCount == 0) {
                    addTestData(expenseViewModel)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                MainAppScreen(expenseViewModel = expenseViewModel)
            }
        }
    }
}

/**
 * Populates the database with a set of fictitious data for testing and demonstration.
 * This function creates several categories, suppliers, and products, then generates
 * random expenses over the last 3 years.
 */
private suspend fun addTestData(viewModel: ExpenseViewModel) {
    withContext(Dispatchers.IO) {
        // 1. Define Categories
        val categories = listOf(
            Category(name = "Groceries", iconName = "local_grocery_store", colorHex = "#4CAF50"),
            Category(name = "Utilities", iconName = "lightbulb", colorHex = "#FFC107"),
            Category(name = "Transport", iconName = "directions_car", colorHex = "#2196F3"),
            Category(name = "Entertainment", iconName = "theaters", colorHex = "#E91E63"),
            Category(name = "Health", iconName = "favorite", colorHex = "#F44336"),
            Category(name = "Shopping", iconName = "shopping_bag", colorHex = "#9C27B0")
        )
        val categoryIds = categories.map { viewModel.addCategory(it) }

        // 2. Define Suppliers
        val suppliers = listOf(
            Supplier(name = "FreshMart"),
            Supplier(name = "City Power & Light"),
            Supplier(name = "Metro Transit"),
            Supplier(name = "Cineplex"),
            Supplier(name = "Community Pharmacy"),
            Supplier(name = "Amazon"),
            Supplier(name = "SuperValue Grocers"),
            Supplier(name = "City Gas Station")
        )
        val supplierIds = suppliers.map { viewModel.addSupplier(it) }

        // 3. Define Products and link them to Categories
        val products = listOf(
            // Groceries
            Product(name = "Milk", categoryId = categoryIds[0].toInt()),
            Product(name = "Bread", categoryId = categoryIds[0].toInt()),
            Product(name = "Eggs", categoryId = categoryIds[0].toInt()),
            Product(name = "Apples", categoryId = categoryIds[0].toInt()),
            // Utilities
            Product(name = "Electricity Bill", categoryId = categoryIds[1].toInt()),
            Product(name = "Water Bill", categoryId = categoryIds[1].toInt()),
            // Transport
            Product(name = "Bus Fare", categoryId = categoryIds[2].toInt()),
            Product(name = "Gasoline", categoryId = categoryIds[2].toInt()),
            // Entertainment
            Product(name = "Movie Ticket", categoryId = categoryIds[3].toInt()),
            Product(name = "Streaming Subscription", categoryId = categoryIds[3].toInt()),
            // Health
            Product(name = "Painkillers", categoryId = categoryIds[4].toInt()),
            // Shopping
            Product(name = "T-Shirt", categoryId = categoryIds[5].toInt()),
            Product(name = "Book", categoryId = categoryIds[5].toInt())
        )
        val productIds = products.map { viewModel.addProduct(it) }

        // 4. Link Suppliers to Categories (Many-to-Many)
        // FreshMart & SuperValue -> Groceries
        viewModel.saveSupplierLinksForCategory(categoryIds[0].toInt(), listOf(supplierIds[0].toInt(), supplierIds[6].toInt()))
        // City Power -> Utilities
        viewModel.saveSupplierLinksForCategory(categoryIds[1].toInt(), listOf(supplierIds[1].toInt()))
        // Metro Transit & City Gas -> Transport
        viewModel.saveSupplierLinksForCategory(categoryIds[2].toInt(), listOf(supplierIds[2].toInt(), supplierIds[7].toInt()))
        // Cineplex & Amazon -> Entertainment
        viewModel.saveSupplierLinksForCategory(categoryIds[3].toInt(), listOf(supplierIds[3].toInt(), supplierIds[5].toInt()))
        // Community Pharmacy -> Health
        viewModel.saveSupplierLinksForCategory(categoryIds[4].toInt(), listOf(supplierIds[4].toInt()))
        // Amazon -> Shopping
        viewModel.saveSupplierLinksForCategory(categoryIds[5].toInt(), listOf(supplierIds[5].toInt()))


        // 5. Generate random expenses over the last 3 years
        val today = System.currentTimeMillis()
        val random = Random(System.currentTimeMillis())
        val expenseList = mutableListOf<Expense>()

        for (i in 1..500) { // Create 500 random expenses
            val randomProductIndex = random.nextInt(productIds.size)
            val product = products[randomProductIndex]
            val productId = productIds[randomProductIndex].toInt()

            // Find suppliers linked to the product's category
            val linkedSupplierIds = when (product.categoryId) {
                categoryIds[0].toInt() -> listOf(supplierIds[0].toInt(), supplierIds[6].toInt()) // Groceries
                categoryIds[1].toInt() -> listOf(supplierIds[1].toInt()) // Utilities
                categoryIds[2].toInt() -> listOf(supplierIds[2].toInt(), supplierIds[7].toInt()) // Transport
                categoryIds[3].toInt() -> listOf(supplierIds[3].toInt(), supplierIds[5].toInt()) // Entertainment
                categoryIds[4].toInt() -> listOf(supplierIds[4].toInt()) // Health
                categoryIds[5].toInt() -> listOf(supplierIds[5].toInt()) // Shopping
                else -> emptyList()
            }
            if (linkedSupplierIds.isEmpty()) continue

            val supplierId = linkedSupplierIds.random(random)
            val amount = when (product.categoryId) {
                categoryIds[1].toInt() -> random.nextDouble(50.0, 200.0) // Utilities are expensive
                else -> random.nextDouble(5.0, 75.0)
            }
            val daysAgo = random.nextInt(1095) // 3 years
            val timestamp = today - TimeUnit.DAYS.toMillis(daysAgo.toLong())

            expenseList.add(
                Expense(
                    productId = productId,
                    supplierId = supplierId,
                    amount = amount,
                    timestamp = timestamp
                )
            )
        }

        expenseList.forEach { viewModel.addExpense(it) }
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
        stringResource(R.string.manage_suppliers) to { context.startActivity(Intent(context, SupplierManagementActivity::class.java)) },
        stringResource(R.string.manage_categories) to { context.startActivity(Intent(context, CategoryManagementActivity::class.java)) },
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
                val isFilterActive = selectedStartDate != null || selectedEndDate != null || selectedCategoryId != null || selectedSupplierId != null
                val title = if (isFilterActive) {
                    stringResource(R.string.no_expenses_match_filter_title)
                } else {
                    stringResource(R.string.no_expenses_title)
                }
                val description = if (isFilterActive) {
                    stringResource(R.string.no_expenses_match_filter_description)
                } else {
                    stringResource(R.string.no_expenses_description)
                }
                val icon = if (isFilterActive) {
                    Icons.Outlined.SearchOff
                } else {
                    Icons.AutoMirrored.Outlined.ReceiptLong
                }
                EmptyState(icon = icon, title = title, description = description)
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

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    ExpenseTrackerTheme {
        //val context = LocalContext.current
        //val application = context.applicationContext as Application
        //val factory = ExpenseViewModelFactory(application)
        //val mockViewModel: ExpenseViewModel = viewModel(factory = factory)
        //MainAppScreen(expenseViewModel = mockViewModel)
    }
}
