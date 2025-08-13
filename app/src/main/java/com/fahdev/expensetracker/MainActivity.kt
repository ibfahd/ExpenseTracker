package com.fahdev.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.fahdev.expensetracker.data.*
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.components.FilterDialog
import com.fahdev.expensetracker.ui.components.FilterStatusRow
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !expenseViewModel.isReady.value
        }

        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                if (expenseViewModel.totalTransactionCount.first() == 0) {
                    addTestData(expenseViewModel)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val isReady by expenseViewModel.isReady.collectAsState()
            if (isReady) {
                ExpenseTrackerTheme {
                    MainAppScreen(expenseViewModel = expenseViewModel)
                }
            }
        }
    }
}

private suspend fun addTestData(viewModel: ExpenseViewModel) {
    withContext(Dispatchers.IO) {
        val categories = listOf(
            Category(name = "Groceries", iconName = "local_grocery_store", colorHex = "#4CAF50"),
            Category(name = "Utilities", iconName = "lightbulb", colorHex = "#FFC107"),
            Category(name = "Transport", iconName = "directions_car", colorHex = "#2196F3"),
            Category(name = "Entertainment", iconName = "theaters", colorHex = "#E91E63"),
            Category(name = "Health", iconName = "favorite", colorHex = "#F44336"),
            Category(name = "Shopping", iconName = "shopping_bag", colorHex = "#9C27B0")
        )
        val categoryIds = categories.map { viewModel.addCategory(it) }
        val suppliers = listOf(
            Supplier(name = "FreshMart"), Supplier(name = "City Power & Light"), Supplier(name = "Metro Transit"),
            Supplier(name = "Cineplex"), Supplier(name = "Community Pharmacy"), Supplier(name = "Amazon"),
            Supplier(name = "SuperValue Grocers"), Supplier(name = "City Gas Station")
        )
        val supplierIds = suppliers.map { viewModel.addSupplier(it) }
        val products = listOf(
            Product(name = "Milk", categoryId = categoryIds[0].toInt()), Product(name = "Bread", categoryId = categoryIds[0].toInt()),
            Product(name = "Electricity Bill", categoryId = categoryIds[1].toInt()), Product(name = "Bus Fare", categoryId = categoryIds[2].toInt()),
            Product(name = "Gasoline", categoryId = categoryIds[2].toInt()), Product(name = "Movie Ticket", categoryId = categoryIds[3].toInt()),
            Product(name = "Painkillers", categoryId = categoryIds[4].toInt()), Product(name = "T-Shirt", categoryId = categoryIds[5].toInt())
        )
        val productIds = products.map { viewModel.addProduct(it) }
        viewModel.saveSupplierLinksForCategory(categoryIds[0].toInt(), listOf(supplierIds[0].toInt(), supplierIds[6].toInt()))
        viewModel.saveSupplierLinksForCategory(categoryIds[2].toInt(), listOf(supplierIds[2].toInt(), supplierIds[7].toInt()))

        val today = System.currentTimeMillis()
        val random = Random(System.currentTimeMillis())
        for (i in 1..200) {
            val randomProductIndex = random.nextInt(productIds.size)
            val product = products[randomProductIndex]
            val productId = productIds[randomProductIndex].toInt()
            val linkedSupplierIds = when (product.categoryId) {
                categoryIds[0].toInt() -> listOf(supplierIds[0].toInt(), supplierIds[6].toInt())
                categoryIds[2].toInt() -> listOf(supplierIds[2].toInt(), supplierIds[7].toInt())
                else -> listOf(supplierIds.random().toInt())
            }
            val supplierId = linkedSupplierIds.random(random)
            val amount = random.nextDouble(5.0, 120.0)
            val daysAgo = random.nextInt(365)
            val date = today - TimeUnit.DAYS.toMillis(daysAgo.toLong())
            viewModel.addExpense(Expense(productId = productId, supplierId = supplierId, amount = amount, date = date))
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
    val currencyFormatter = remember(currencyCode) { CurrencyHelper.getCurrencyFormatter(currencyCode) }

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
            AppDrawerContent(onNavigate = { destinationKey ->
                navigationActions[destinationKey]?.invoke()
                scope.launch { drawerState.close() }
            })
        }
    ) {
        ExpenseTrackerApp(
            expenseViewModel = expenseViewModel,
            currencyFormatter = currencyFormatter,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
fun AppDrawerContent(onNavigate: (String) -> Unit) {
    val menuItems = listOf(
        Pair(stringResource(R.string.manage_suppliers), Icons.Default.LocationOn),
        Pair(stringResource(R.string.manage_categories), Icons.Default.Category),
        Pair(stringResource(R.string.shopping_list_title), Icons.Default.ShoppingCart),
        Pair(stringResource(R.string.title_activity_reporting), Icons.Default.Assessment),
        Pair(stringResource(R.string.settings), Icons.Default.Settings),
        Pair(stringResource(R.string.about), Icons.Default.Info)
    )
    ModalDrawerSheet {
        Text(
            stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        menuItems.forEach { (title, icon) ->
            NavigationDrawerItem(
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) },
                selected = false,
                onClick = { onNavigate(title) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
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

    val isFilterActive = selectedStartDate != null || selectedEndDate != null || selectedCategoryId != null || selectedSupplierId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.app_name)) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu)) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = context.getString(R.string.filter_expenses), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { context.startActivity(Intent(context, AddExpenseActivity::class.java)) },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, context.getString(R.string.add_new_expense))
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ExpenseSummaryCard(totalAmount = totalFilteredExpenses, currencyFormatter = currencyFormatter)
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
                val title = if (isFilterActive) stringResource(R.string.no_expenses_match_filter_title) else stringResource(R.string.no_expenses_title)
                val description = if (isFilterActive) stringResource(R.string.no_expenses_match_filter_description) else stringResource(R.string.no_expenses_description)
                val icon = if (isFilterActive) Icons.Outlined.SearchOff else Icons.AutoMirrored.Outlined.ReceiptLong
                EmptyState(icon = icon, title = title, description = description)
            } else {
                if (isFilterActive) {
                    CategorizedExpenseList(expenses = filteredExpenses, currencyFormatter = currencyFormatter)
                } else {
                    ChronologicalExpenseList(expenses = filteredExpenses, currencyFormatter = currencyFormatter)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChronologicalExpenseList(expenses: List<ExpenseWithDetails>, currencyFormatter: NumberFormat) {
    val context = LocalContext.current
    val groupedByDate = expenses.groupBy {
        // Normalize to the start of the day
        Calendar.getInstance().apply {
            timeInMillis = it.expense.date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        groupedByDate.forEach { (dateMillis, expensesOnDate) ->
            stickyHeader {
                DateHeader(dateMillis = dateMillis)
            }
            items(expensesOnDate, key = { it.expense.id }) { expense ->
                ExpenseItem(
                    expenseWithDetails = expense,
                    currencyFormatter = currencyFormatter,
                    onExpenseClick = {
                        val intent = Intent(context, EditExpenseActivity::class.java).apply {
                            putExtra("EXPENSE_ID", it)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun CategorizedExpenseList(expenses: List<ExpenseWithDetails>, currencyFormatter: NumberFormat) {
    val context = LocalContext.current
    val groupedByCategory = expenses.groupBy { it.productWithCategory.category }
    val expandedCategories = remember { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        groupedByCategory.forEach { (category, expensesInCategory) ->
            item {
                val total = expensesInCategory.sumOf { it.expense.amount }
                CategoryAccordionHeader(
                    category = category,
                    totalAmount = total,
                    isExpanded = expandedCategories[category.id] ?: false,
                    onToggle = { expandedCategories[category.id] = !(expandedCategories[category.id] ?: false) },
                    currencyFormatter = currencyFormatter
                )
            }
            item {
                AnimatedVisibility(visible = expandedCategories[category.id] ?: false) {
                    Column {
                        expensesInCategory.forEach { expense ->
                            ExpenseItem(
                                expenseWithDetails = expense,
                                currencyFormatter = currencyFormatter,
                                onExpenseClick = {
                                    val intent = Intent(context, EditExpenseActivity::class.java).apply {
                                        putExtra("EXPENSE_ID", it)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(dateMillis: Long) {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    calendar.timeInMillis = dateMillis

    val dateText = when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    Text(
        text = dateText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun CategoryAccordionHeader(
    category: Category,
    totalAmount: Double,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = IconAndColorUtils.iconMap[category.iconName] ?: Icons.AutoMirrored.Filled.Label
        val color = IconAndColorUtils.colorMap[category.colorHex] ?: MaterialTheme.colorScheme.primary
        Icon(imageVector = icon, contentDescription = category.name, tint = color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = currencyFormatter.format(totalAmount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(rotationAngle)
        )
    }
}

@Composable
fun ExpenseSummaryCard(totalAmount: Double, currencyFormatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_expenses),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onExpenseClick(expenseWithDetails.expense.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val category = expenseWithDetails.productWithCategory.category
            val icon = IconAndColorUtils.iconMap[category.iconName] ?: Icons.AutoMirrored.Filled.Label
            val color = IconAndColorUtils.colorMap[category.colorHex] ?: MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = category.name, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expenseWithDetails.productWithCategory.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = expenseWithDetails.supplier.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = currencyFormatter.format(expenseWithDetails.expense.amount),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}