package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

/**
 * Activity for managing the shopping list.
 * Allows users to select a supplier, add new items to the list,
 * update quantities and prices, delete items, and validate all purchases.
 */
class ShoppingListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ShoppingListActivity", "onCreate started")
        setContent {
            Log.d("ShoppingListActivity", "setContent started")
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application

                    val shoppingListViewModel: ShoppingListViewModel = viewModel(
                        factory = ShoppingListViewModelFactory(app)
                    )
                    val expenseViewModel: ExpenseViewModel = viewModel(
                        factory = ExpenseViewModelFactory(app)
                    )

                    Log.d("ShoppingListActivity", "ViewModels initialized. Composing screen.")
                    ShoppingListScreen(
                        shoppingListViewModel = shoppingListViewModel,
                        expenseViewModel = expenseViewModel
                    )
                    Log.d("ShoppingListActivity", "ShoppingListScreen composed.")
                }
            }
            Log.d("ShoppingListActivity", "onCreate finished.")
        }
    }
}

/**
 * Composable function for the Shopping List screen.
 * Displays the shopping list, allows supplier selection, adding new items,
 * and managing existing items.
 *
 * @param shoppingListViewModel The ViewModel providing data and logic for the shopping list.
 * @param expenseViewModel The ViewModel providing data and logic for expenses, including product and category management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingListViewModel: ShoppingListViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect states from the ViewModels
    val currentSupplierId by shoppingListViewModel.currentSupplierId.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())

    // State for supplier dropdown
    var expandedSupplierDropdown by remember { mutableStateOf(false) }

    // State for main "Add Shopping Item" dialog
    var showAddShoppingItemDialog by remember { mutableStateOf(false) }
    var newProductIdForAddItemDialog by remember { mutableStateOf<Int?>(null) }
    var newProductTextForAddItemDialog by remember { mutableStateOf("") }
    var newQuantityForAddItemDialog by remember { mutableStateOf("") }
    var newUnitForAddItemDialog by remember { mutableStateOf("") }
    var expandedProductDropdownForAddItemDialog by remember { mutableStateOf(false) }

    // State for nested "Add New Product" dialog
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductNameForAddProductDialog by remember { mutableStateOf("") } // Pre-fill for AddProductDialog

    // State for "Validate All Purchases" confirmation dialog
    var showConfirmValidateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddShoppingItemDialog = true
                // Reset fields when opening the Add Shopping Item dialog
                newProductIdForAddItemDialog = null
                newProductTextForAddItemDialog = ""
                newQuantityForAddItemDialog = ""
                newUnitForAddItemDialog = ""
                expandedProductDropdownForAddItemDialog = false
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add new shopping item")
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showConfirmValidateDialog = true // Show confirmation dialog
                        },
                        // Enable the button if there are any items in the list that could potentially be recorded
                        enabled = shoppingListItems.any { it.quantity > 0.0 && it.unitPrice != null }
                    ) {
                        Text("Validate All Purchases")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Supplier Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
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

            Spacer(Modifier.height(16.dp))

            // Shopping List Items Display Section
            Text(stringResource(R.string.current_shopping_list_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            if (shoppingListItems.isEmpty()) {
                Text(stringResource(R.string.no_items_in_list))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(shoppingListItems, key = { it.id }) { item ->
                        ShoppingListItemCard(
                            item = item,
                            onUpdate = { updatedItem ->
                                shoppingListViewModel.updateShoppingItem(updatedItem)
                            },
                            onDelete = { itemToDelete ->
                                shoppingListViewModel.deleteShoppingItem(itemToDelete)
                            },
                            allProducts = allProducts
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // --- Add Shopping Item Dialog ---
    if (showAddShoppingItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddShoppingItemDialog = false },
            title = { Text("Add New Shopping Item") },
            text = {
                Column {
                    // Product Selection (Autocomplete-like dropdown)
                    ExposedDropdownMenuBox(
                        expanded = expandedProductDropdownForAddItemDialog,
                        onExpandedChange = { expandedProductDropdownForAddItemDialog = !expandedProductDropdownForAddItemDialog },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = newProductTextForAddItemDialog,
                            onValueChange = { newValue ->
                                newProductTextForAddItemDialog = newValue
                                newProductIdForAddItemDialog = null // Clear product ID if text changes (user is typing)
                                expandedProductDropdownForAddItemDialog = true // Keep dropdown open while typing
                            },
                            label = { Text(stringResource(R.string.product_name_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductDropdownForAddItemDialog) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedProductDropdownForAddItemDialog,
                            onDismissRequest = { expandedProductDropdownForAddItemDialog = false }
                        ) {
                            val filteredProducts = allProducts.filter {
                                it.name.contains(newProductTextForAddItemDialog, ignoreCase = true)
                            }
                            if (filteredProducts.isEmpty() && newProductTextForAddItemDialog.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Add new product: \"$newProductTextForAddItemDialog\"") },
                                    onClick = {
                                        newProductNameForAddProductDialog = newProductTextForAddItemDialog // Pre-fill for AddProductDialog
                                        showAddProductDialog = true // Show the nested add product dialog
                                        expandedProductDropdownForAddItemDialog = false
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            filteredProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = {
                                        newProductTextForAddItemDialog = product.name
                                        newProductIdForAddItemDialog = product.id
                                        expandedProductDropdownForAddItemDialog = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newQuantityForAddItemDialog,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                                newQuantityForAddItemDialog = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.quantity_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUnitForAddItemDialog,
                        onValueChange = { newUnitForAddItemDialog = it },
                        label = { Text(stringResource(R.string.unit_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val productId = newProductIdForAddItemDialog
                        val quantity = newQuantityForAddItemDialog.toDoubleOrNull()
                        if (productId != null && quantity != null && quantity > 0) {
                            shoppingListViewModel.addShoppingItem(productId, newUnitForAddItemDialog.ifBlank { null }, quantity)
                            showAddShoppingItemDialog = false // Close dialog
                            coroutineScope.launch { snackbarHostState.showSnackbar("Item added to shopping list!") }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Please select a product and enter a valid positive quantity.",
                                    withDismissAction = true
                                )
                            }
                        }
                    },
                    enabled = newProductIdForAddItemDialog != null && newQuantityForAddItemDialog.toDoubleOrNull() != null && (newQuantityForAddItemDialog.toDoubleOrNull() ?: 0.0) > 0
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddShoppingItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Add New Product Dialog (Extracted as a separate Composable) ---
    if (showAddProductDialog) {
        AddProductDialog(
            onDismissRequest = { showAddProductDialog = false },
            initialProductName = newProductNameForAddProductDialog,
            expenseViewModel = expenseViewModel,
            snackbarHostState = snackbarHostState,
            onProductAddedOrSelected = { productId, productName ->
                // Update the state variables of the *parent* Add Shopping Item Dialog
                newProductIdForAddItemDialog = productId
                newProductTextForAddItemDialog = productName
            }
        )
    }

    // --- Confirmation Dialog for Validate All Purchases ---
    if (showConfirmValidateDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmValidateDialog = false },
            title = { Text("Confirm Purchases") },
            text = { Text("Are you sure you want to record all entered purchases as expenses?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val recordedCount = shoppingListViewModel.recordAllPurchases()
                            if (recordedCount > 0) {
                                snackbarHostState.showSnackbar("$recordedCount expenses recorded successfully!")
                            } else {
                                snackbarHostState.showSnackbar("No valid purchases to record.")
                            }
                            showConfirmValidateDialog = false
                        }
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmValidateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Composable function for displaying a single shopping list item as a card.
 * Allows updating purchased quantity and unit price, and deleting the item.
 *
 * @param item The [ShoppingListItem] to display.
 * @param onUpdate Lambda to be invoked when the item is updated.
 * @param onDelete Lambda to be invoked when the item is deleted.
 * @param allProducts List of all products to resolve product names.
 * @param modifier Modifier for this composable.
 */
@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onUpdate: (ShoppingListItem) -> Unit,
    onDelete: (ShoppingListItem) -> Unit,
    allProducts: List<Product>,
    modifier: Modifier = Modifier
) {
    // Find the product name based on productId
    val productName = allProducts.find { it.id == item.productId }?.name ?: stringResource(R.string.unknown_product)

    // State for purchased quantity and unit price text fields
    var purchasedQuantityText by remember(item.quantity) {
        mutableStateOf(if (item.quantity > 0.0) item.quantity.toString() else "")
    }
    var unitPriceText by remember(item.unitPrice) {
        mutableStateOf(item.unitPrice?.toString() ?: "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Change card color based on whether the item has been "purchased" (quantity > 0 and unitPrice is set)
            containerColor = if (item.quantity > 0.0 && item.unitPrice != null) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.planned_quantity_display)}: ${item.quantity} ${item.unit.orEmpty()}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = purchasedQuantityText,
                    onValueChange = { newValue ->
                        // Allow only numeric input (including decimals)
                        if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            purchasedQuantityText = newValue
                            val newQuantity = newValue.toDoubleOrNull() ?: 0.0
                            // Update the item in the ViewModel (without creating expense yet)
                            onUpdate(item.copy(quantity = newQuantity))
                        }
                    },
                    label = { Text(stringResource(R.string.purchased_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { newValue ->
                        // Allow only numeric input (including decimals)
                        if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            unitPriceText = newValue
                            val newPrice = newValue.toDoubleOrNull()
                            // Update the item in the ViewModel (without creating expense yet)
                            onUpdate(item.copy(unitPrice = newPrice))
                        }
                    },
                    label = { Text(stringResource(R.string.unit_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onDelete(item) }, // Delete the item
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_item))
                }
            }
        }
    }
}

/**
 * Composable function for adding a new product or selecting an existing one.
 * This dialog is typically nested within another dialog (e.g., Add Shopping Item Dialog).
 *
 * @param onDismissRequest Lambda to be invoked when the dialog is dismissed.
 * @param onProductAddedOrSelected Lambda to be invoked when a product is added or selected,
 * providing the product ID and name.
 * @param initialProductName The initial product name to pre-fill the text field.
 * @param expenseViewModel The ViewModel for expense-related operations (product/category management).
 * @param snackbarHostState The SnackbarHostState to show messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismissRequest: () -> Unit,
    onProductAddedOrSelected: (Int, String) -> Unit,
    initialProductName: String,
    expenseViewModel: ExpenseViewModel,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())

    var newProductNameDialog by remember { mutableStateOf(initialProductName) }
    var newProductSelectedCategory by remember { mutableStateOf<Category?>(null) }
    var newProductCategorySearchQuery by remember { mutableStateOf("") }
    var newProductCategoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add New Product") },
        text = {
            Column {
                OutlinedTextField(
                    value = newProductNameDialog,
                    onValueChange = { newProductNameDialog = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = newProductCategoryDropdownExpanded,
                    onExpandedChange = { newProductCategoryDropdownExpanded = !newProductCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newProductSelectedCategory?.name ?: newProductCategorySearchQuery,
                        onValueChange = { newValue ->
                            newProductCategorySearchQuery = newValue
                            newProductSelectedCategory = null
                            newProductCategoryDropdownExpanded = true
                        },
                        readOnly = false,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = newProductCategoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = newProductCategoryDropdownExpanded,
                        onDismissRequest = { newProductCategoryDropdownExpanded = false }
                    ) {
                        val filteredCategories = allCategories.filter {
                            it.name.contains(newProductCategorySearchQuery, ignoreCase = true)
                        }
                        if (filteredCategories.isEmpty() && newProductCategorySearchQuery.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Add new category: \"$newProductCategorySearchQuery\"") },
                                onClick = {
                                    coroutineScope.launch {
                                        val existingCategory = expenseViewModel.getCategoryByName(newProductCategorySearchQuery)
                                        val categoryToAdd = existingCategory ?: Category(name = newProductCategorySearchQuery)
                                        val categoryId = existingCategory?.id?.toLong() ?: expenseViewModel.addCategory(categoryToAdd)

                                        if (categoryId != -1L) {
                                            newProductSelectedCategory = categoryToAdd.copy(id = categoryId.toInt())
                                            newProductCategorySearchQuery = newProductSelectedCategory!!.name
                                            snackbarHostState.showSnackbar("Category added!")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to add category.")
                                        }
                                        newProductCategoryDropdownExpanded = false
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
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
            TextButton(
                onClick = {
                    if (newProductNameDialog.isNotBlank() && newProductSelectedCategory != null) {
                        coroutineScope.launch {
                            val existingProduct = expenseViewModel.getProductByName(newProductNameDialog)
                            if (existingProduct == null) {
                                val newId = expenseViewModel.addProduct(
                                    Product(
                                        name = newProductNameDialog,
                                        categoryId = newProductSelectedCategory!!.id
                                    )
                                )
                                if (newId != -1L) {
                                    onProductAddedOrSelected(newId.toInt(), newProductNameDialog)
                                    snackbarHostState.showSnackbar("Product added!")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to add product.")
                                }
                            } else {
                                onProductAddedOrSelected(existingProduct.id, existingProduct.name)
                                snackbarHostState.showSnackbar("Product already exists, selected.")
                            }
                            onDismissRequest() // Dismiss this dialog
                        }
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Product name and category cannot be empty.") }
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

/**
 * Preview for the ShoppingListScreen.
 */
@Preview(showBackground = true)
@Composable
fun PreviewShoppingListScreen() {
    ExpenseTrackerTheme {
        // For preview, provide dummy ViewModels
        ShoppingListScreen(
            shoppingListViewModel = ShoppingListViewModel(Application()),
            expenseViewModel = ExpenseViewModel(Application())
        )
    }
}