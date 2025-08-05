package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CurrencyHelper
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.data.UserPreferencesRepository
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

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
    val currentSupplierId by shoppingListViewModel.currentSupplierId.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())
    val userPrefsRepo = remember { UserPreferencesRepository.getInstance(context.applicationContext) }
    val currencyCode by userPrefsRepo.currencyCode.collectAsState()
    val currencyFormatter = remember(currencyCode) {
        CurrencyHelper.getCurrencyFormatter(currencyCode)
    }


    // UI State
    var expandedSupplierDropdown by remember { mutableStateOf(false) }
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
            if (currentSupplierId != null) {
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
            // Supplier selection dropdown
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.supplier_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedSupplierDropdown,
                    onExpandedChange = { expandedSupplierDropdown = !expandedSupplierDropdown },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = allSuppliers.find { it.id == currentSupplierId }?.name ?: stringResource(R.string.select_supplier),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplierDropdown) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSupplierDropdown,
                        onDismissRequest = { expandedSupplierDropdown = false }
                    ) {
                        allSuppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    shoppingListViewModel.selectSupplier(supplier.id)
                                    expandedSupplierDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Main content area for the list
            if (shoppingListItems.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = stringResource(id = R.string.no_items_in_list_title),
                    description = stringResource(id = R.string.no_items_in_list_description)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
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

    // --- Dialogs ---

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
    val totalCost = if (isItemValidated) item.purchasedQuantity * (item.unitPrice ?: 0.0) else 0.0

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

@Preview(showBackground = true)
@Composable
fun PreviewShoppingListScreen() {
    ExpenseTrackerTheme {
        /* This is a placeholder for the preview.
        // In a real app, you would inject mock ViewModels here.
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Shopping List") },
                    navigationIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) },
                    actions = {
                        IconButton(onClick = {}) {
                            BadgedBox(badge = { Badge { Text("3") } }) {
                                Icon(Icons.Default.ShoppingCartCheckout, null)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, null)
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("Shopping List Content Area")
            }
        }*/
    }
}
