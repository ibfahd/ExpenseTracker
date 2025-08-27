package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.*
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.components.SelectionAccordion
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ShoppingListActivity : AppCompatActivity() {
    private val shoppingListViewModel: ShoppingListViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShoppingListScreen(
                        shoppingListViewModel = shoppingListViewModel,
                        expenseViewModel = expenseViewModel
                    )
                }
            }
        }
    }
}

data class ValidationStats(
    val validItemsCount: Int,
    val totalItems: Int,
    val totalCost: Double,
    val hasValidItems: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingListViewModel: ShoppingListViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State from ViewModels
    val currentSupplier by shoppingListViewModel.currentSupplier.collectAsState()
    val shoppingDate by shoppingListViewModel.shoppingDate.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())
    val userPrefsRepo = remember { UserPreferencesRepository.getInstance(context.applicationContext) }
    val currencyCode by userPrefsRepo.currencyCode.collectAsState()
    val currencyFormatter = remember(currencyCode) {
        CurrencyHelper.getCurrencyFormatter(currencyCode)
    }

    // UI State
    var isSupplierAccordionExpanded by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddShoppingItemDialog by remember { mutableStateOf(false) }
    var showEditShoppingItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShoppingListItem?>(null) }
    var showConfirmValidateDialog by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }

    // Derived state for validation summary
    val validationStats by remember(shoppingListItems) {
        derivedStateOf {
            val validItems = shoppingListItems.filter { it.purchasedQuantity > 0.0 && it.unitPrice != null }
            val totalCost = validItems.sumOf { it.purchasedQuantity * (it.unitPrice ?: 0.0) }
            ValidationStats(
                validItemsCount = validItems.size,
                totalItems = shoppingListItems.size,
                totalCost = totalCost,
                hasValidItems = validItems.isNotEmpty()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button_desc))
                    }
                },
                actions = {
                    if (validationStats.hasValidItems) {
                        IconButton(onClick = { showConfirmValidateDialog = true }, enabled = !isValidating) {
                            BadgedBox(
                                badge = { Badge { Text(validationStats.validItemsCount.toString()) } }
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCartCheckout,
                                    contentDescription = stringResource(R.string.validate_purchases_action)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentSupplier != null && shoppingDate != null) {
                FloatingActionButton(onClick = { showAddShoppingItemDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_new_shopping_item))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            // --- Supplier Accordion ---
            SelectionAccordion(
                title = stringResource(R.string.supplier),
                selectedItem = currentSupplier,
                isExpanded = isSupplierAccordionExpanded,
                onToggle = { isSupplierAccordionExpanded = !isSupplierAccordionExpanded },
                items = allSuppliers,
                onItemSelected = { supplier ->
                    shoppingListViewModel.selectSupplier(supplier)
                    isSupplierAccordionExpanded = false
                },
                defaultIcon = Icons.Outlined.Storefront
            )

            Spacer(Modifier.height(16.dp))

            // --- Date Selection ---
            AnimatedVisibility(visible = currentSupplier != null) {
                Box(modifier = Modifier.clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = shoppingDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: stringResource(R.string.select_date),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.date)) },
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false, // Make it visually distinct as a non-editable field
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- Main content area for the list ---
            AnimatedVisibility(visible = currentSupplier != null && shoppingDate != null) {
                if (shoppingListItems.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.ShoppingCart,
                        title = stringResource(id = R.string.no_items_in_list_title),
                        description = stringResource(id = R.string.no_items_in_list_description)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shoppingListItems, key = { it.id }) { item ->
                            ShoppingListItemCard(
                                item = item,
                                productName = allProducts.find { it.id == item.productId }?.name ?: stringResource(R.string.unknown_product),
                                currencyFormatter = currencyFormatter,
                                onClick = {
                                    itemToEdit = item
                                    showEditShoppingItemDialog = true
                                },
                                onDelete = { itemToDelete ->
                                    shoppingListViewModel.deleteShoppingItem(itemToDelete)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = shoppingDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        shoppingListViewModel.selectShoppingDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddShoppingItemDialog) {
        AddShoppingItemDialog(
            allProducts = allProducts,
            expenseViewModel = expenseViewModel,
            shoppingListViewModel = shoppingListViewModel,
            onDismiss = { showAddShoppingItemDialog = false },
            onConfirm = { productId, unit, quantity ->
                shoppingListViewModel.addShoppingItem(productId, unit, quantity)
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.item_added_to_shopping_list)) }
            },
            snackbarHostState = snackbarHostState
        )
    }

    if (showEditShoppingItemDialog && itemToEdit != null) {
        EditShoppingItemDialog(
            item = itemToEdit!!,
            currencyFormatter = currencyFormatter,
            onDismiss = { showEditShoppingItemDialog = false },
            onConfirm = { updatedItem ->
                shoppingListViewModel.updateShoppingItem(updatedItem)
                showEditShoppingItemDialog = false
            }
        )
    }

    if (showConfirmValidateDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmValidateDialog = false },
            title = { Text(stringResource(R.string.complete_shopping_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.confirm_purchases_message))
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.validation_summary_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.items_to_record, validationStats.validItemsCount))
                            Text(stringResource(R.string.total_amount, currencyFormatter.format(validationStats.totalCost)), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isValidating = true
                            try {
                                val recordedCount = shoppingListViewModel.recordAllPurchases()
                                snackbarHostState.showSnackbar(
                                    if (recordedCount > 0) context.getString(R.string.expenses_recorded_successfully, recordedCount)
                                    else context.getString(R.string.no_valid_purchases_to_record)
                                )
                            } finally {
                                isValidating = false
                                showConfirmValidateDialog = false
                            }
                        }
                    },
                    enabled = !isValidating
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.record_expenses))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmValidateDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    productName: String,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit,
    onDelete: (ShoppingListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isItemValidated = item.purchasedQuantity > 0.0 && item.unitPrice != null
    val totalCost = if (isItemValidated) item.purchasedQuantity * item.unitPrice else 0.0

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isItemValidated) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isItemValidated) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.item_validated_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.planned_quantity_display)}: ${item.plannedQuantity} ${item.unit.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                if (isItemValidated) {
                    Text(
                        text = "${item.purchasedQuantity} x ${currencyFormatter.format(item.unitPrice)} = ${currencyFormatter.format(totalCost)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onDelete(item) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_item, productName),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EditShoppingItemDialog(
    item: ShoppingListItem,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (ShoppingListItem) -> Unit
) {
    var purchasedQuantityText by remember { mutableStateOf(item.purchasedQuantity.toString().takeIf { it != "0.0" } ?: "") }
    var unitPriceText by remember { mutableStateOf(item.unitPrice?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = purchasedQuantityText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            purchasedQuantityText = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.purchased_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            unitPriceText = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.unit_price_label)) },
                    prefix = { Text(currencyFormatter.currency?.symbol ?: "$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedItem = item.copy(
                    purchasedQuantity = purchasedQuantityText.toDoubleOrNull() ?: 0.0,
                    unitPrice = unitPriceText.toDoubleOrNull()
                )
                onConfirm(updatedItem)
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemDialog(
    allProducts: List<Product>,
    expenseViewModel: ExpenseViewModel,
    shoppingListViewModel: ShoppingListViewModel,
    onDismiss: () -> Unit,
    onConfirm: (productId: Int, unit: String?, quantity: Double) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var productText by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var plannedQuantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }

    val categoriesForSupplier by shoppingListViewModel.categoriesForSupplier.collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val productsForCategory by remember(selectedCategoryId) {
        derivedStateOf {
            if (selectedCategoryId == null) {
                allProducts
            } else {
                allProducts.filter { it.categoryId == selectedCategoryId }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    TextField(
                        value = categoriesForSupplier.find { it.id == selectedCategoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categoriesForSupplier.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    productText = ""
                                    selectedProductId = null
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = productDropdownExpanded,
                    onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                ) {
                    TextField(
                        value = productText,
                        onValueChange = {
                            productText = it
                            selectedProductId = null
                            productDropdownExpanded = true
                        },
                        label = { Text(stringResource(R.string.product_name_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                        enabled = selectedCategoryId != null
                    )
                    ExposedDropdownMenu(
                        expanded = productDropdownExpanded,
                        onDismissRequest = { productDropdownExpanded = false }
                    ) {
                        val filteredProducts = productsForCategory.filter { it.name.contains(productText, ignoreCase = true) }
                        if (filteredProducts.isEmpty() && productText.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_new_product, productText)) },
                                onClick = {
                                    newProductName = productText
                                    showAddProductDialog = true
                                    productDropdownExpanded = false
                                }
                            )
                        }
                        filteredProducts.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    productText = product.name
                                    selectedProductId = product.id
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = plannedQuantity,
                    onValueChange = { newValue -> if (newValue.matches(Regex("""^\d*\.?\d*$"""))) plannedQuantity = newValue },
                    label = { Text(stringResource(R.string.quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.unit_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val isConfirmEnabled = selectedProductId != null && (plannedQuantity.toDoubleOrNull() ?: 0.0) > 0.0
            Button(
                onClick = {
                    onConfirm(selectedProductId!!, unit.ifBlank { null }, plannedQuantity.toDouble())
                    onDismiss()
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showAddProductDialog) {
        AddProductDialog(
            onDismissRequest = { showAddProductDialog = false },
            initialProductName = newProductName,
            expenseViewModel = expenseViewModel,
            snackbarHostState = snackbarHostState,
            onProductAddedOrSelected = { productId, productName ->
                selectedProductId = productId
                productText = productName
            },
            // Pass the selected category to the AddProductDialog
            preselectedCategoryId = selectedCategoryId
        )
    }
}

// This is the same dialog from ShoppingListActivity, kept for creating new products in the flow
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismissRequest: () -> Unit,
    onProductAddedOrSelected: (Int, String) -> Unit,
    initialProductName: String,
    expenseViewModel: ExpenseViewModel,
    snackbarHostState: SnackbarHostState,
    preselectedCategoryId: Int?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    var newProductNameDialog by remember { mutableStateOf(initialProductName) }
    var newProductSelectedCategory by remember { mutableStateOf<Category?>(null) }
    var newProductCategorySearchQuery by remember { mutableStateOf("") }
    var newProductCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-select the category if one was passed in
    LaunchedEffect(preselectedCategoryId, allCategories) {
        if (preselectedCategoryId != null) {
            newProductSelectedCategory = allCategories.find { it.id == preselectedCategoryId }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.add_new_product_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newProductNameDialog,
                    onValueChange = { newProductNameDialog = it },
                    label = { Text(stringResource(R.string.product_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = newProductCategoryDropdownExpanded,
                    onExpandedChange = { newProductCategoryDropdownExpanded = !newProductCategoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = newProductSelectedCategory?.name ?: newProductCategorySearchQuery,
                        onValueChange = {
                            newProductCategorySearchQuery = it
                            newProductSelectedCategory = null
                            newProductCategoryDropdownExpanded = true
                        },
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = newProductCategoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                        // Disable editing if a category is pre-selected
                        enabled = preselectedCategoryId == null
                    )
                    ExposedDropdownMenu(
                        expanded = newProductCategoryDropdownExpanded,
                        onDismissRequest = { newProductCategoryDropdownExpanded = false }
                    ) {
                        val filteredCategories = allCategories.filter { it.name.contains(newProductCategorySearchQuery, ignoreCase = true) }
                        if (filteredCategories.isEmpty() && newProductCategorySearchQuery.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_new_category, newProductCategorySearchQuery)) },
                                onClick = {
                                    coroutineScope.launch {
                                        val existingCategory = expenseViewModel.getCategoryByName(newProductCategorySearchQuery)
                                        val categoryToAdd = existingCategory ?: Category(name = newProductCategorySearchQuery)
                                        val categoryId = existingCategory?.id?.toLong() ?: expenseViewModel.addCategory(categoryToAdd)
                                        if (categoryId != -1L) {
                                            newProductSelectedCategory = categoryToAdd.copy(id = categoryId.toInt())
                                            newProductCategorySearchQuery = newProductSelectedCategory!!.name
                                        }
                                        newProductCategoryDropdownExpanded = false
                                    }
                                }
                            )
                        }
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    newProductSelectedCategory = category
                                    newProductCategorySearchQuery = category.name
                                    newProductCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newProductNameDialog.isNotBlank() && newProductSelectedCategory != null) {
                        coroutineScope.launch {
                            val existingProduct = expenseViewModel.getProductByName(newProductNameDialog)
                            if (existingProduct == null) {
                                val newId = expenseViewModel.addProduct(Product(name = newProductNameDialog, categoryId = newProductSelectedCategory!!.id))
                                if (newId != -1L) {
                                    onProductAddedOrSelected(newId.toInt(), newProductNameDialog)
                                    snackbarHostState.showSnackbar(context.getString(R.string.product_added_successfully))
                                }
                            } else {
                                onProductAddedOrSelected(existingProduct.id, existingProduct.name)
                                snackbarHostState.showSnackbar(context.getString(R.string.product_exists))
                            }
                            onDismissRequest()
                        }
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.product_name_and_category_empty)) }
                    }
                },
                enabled = newProductNameDialog.isNotBlank() && newProductSelectedCategory != null
            ) { Text(stringResource(R.string.add_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}