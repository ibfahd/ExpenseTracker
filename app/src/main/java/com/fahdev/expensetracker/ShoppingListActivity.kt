package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
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
import androidx.compose.runtime.LaunchedEffect
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
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class ShoppingListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
                    ShoppingListScreen(
                        shoppingListViewModel = shoppingListViewModel,
                        expenseViewModel = expenseViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingListViewModel: ShoppingListViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSupplierId by shoppingListViewModel.currentSupplierId.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())
    //val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())

    var expandedSupplierDropdown by remember { mutableStateOf(false) }
    var showAddShoppingItemDialog by remember { mutableStateOf(false) }
    var newProductIdForAddItemDialog by remember { mutableStateOf<Int?>(null) }
    var newProductTextForAddItemDialog by remember { mutableStateOf("") }
    var newPlannedQuantityForAddItemDialog by remember { mutableStateOf("") } // Changed from newQuantity...
    var newUnitForAddItemDialog by remember { mutableStateOf("") }
    var expandedProductDropdownForAddItemDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductNameForAddProductDialog by remember { mutableStateOf("") }
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
            if (currentSupplierId != null) {
                FloatingActionButton(onClick = {
                    showAddShoppingItemDialog = true
                    newProductIdForAddItemDialog = null
                    newProductTextForAddItemDialog = ""
                    newPlannedQuantityForAddItemDialog = "" // Reset planned quantity
                    newUnitForAddItemDialog = ""
                    expandedProductDropdownForAddItemDialog = false
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add new shopping item")
                }
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
                        onClick = { showConfirmValidateDialog = true },
                        enabled = shoppingListItems.any { it.purchasedQuantity > 0.0 && it.unitPrice != null }
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

            Text(stringResource(R.string.current_shopping_list_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            if (shoppingListItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ShoppingCartCheckout,
                        title = stringResource(id = R.string.no_items_in_list_title),
                        description = stringResource(id = R.string.no_items_in_list_description)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
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

    if (showAddShoppingItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddShoppingItemDialog = false },
            title = { Text("Add New Shopping Item") },
            text = {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = expandedProductDropdownForAddItemDialog,
                        onExpandedChange = { expandedProductDropdownForAddItemDialog = !expandedProductDropdownForAddItemDialog },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = newProductTextForAddItemDialog,
                            onValueChange = { newValue ->
                                newProductTextForAddItemDialog = newValue
                                newProductIdForAddItemDialog = null
                                expandedProductDropdownForAddItemDialog = true
                            },
                            label = { Text(stringResource(R.string.product_name_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductDropdownForAddItemDialog) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
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
                                        newProductNameForAddProductDialog = newProductTextForAddItemDialog
                                        showAddProductDialog = true
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
                        value = newPlannedQuantityForAddItemDialog, // Use newPlannedQuantity...
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                                newPlannedQuantityForAddItemDialog = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.quantity_label)) }, // This label means "Planned Quantity"
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
                        val plannedQuantity = newPlannedQuantityForAddItemDialog.toDoubleOrNull() // Parse planned quantity
                        if (productId != null && plannedQuantity != null && plannedQuantity > 0) {
                            shoppingListViewModel.addShoppingItem(productId, newUnitForAddItemDialog.ifBlank { null }, plannedQuantity)
                            showAddShoppingItemDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Item added to shopping list!") }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Please select a product and enter a valid positive planned quantity.",
                                    withDismissAction = true
                                )
                            }
                        }
                    },
                    enabled = newProductIdForAddItemDialog != null && newPlannedQuantityForAddItemDialog.toDoubleOrNull() != null && (newPlannedQuantityForAddItemDialog.toDoubleOrNull() ?: 0.0) > 0
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddShoppingItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismissRequest = { showAddProductDialog = false },
            initialProductName = newProductNameForAddProductDialog,
            expenseViewModel = expenseViewModel,
            snackbarHostState = snackbarHostState,
            onProductAddedOrSelected = { productId, productName ->
                newProductIdForAddItemDialog = productId
                newProductTextForAddItemDialog = productName
            }
        )
    }

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

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onUpdate: (ShoppingListItem) -> Unit,
    onDelete: (ShoppingListItem) -> Unit,
    allProducts: List<Product>,
    modifier: Modifier = Modifier
) {
    val productName = allProducts.find { it.id == item.productId }?.name ?: stringResource(R.string.unknown_product)

    // State for purchased quantity text field. Keyed by item.id.
    var purchasedQuantityText by remember(item.id) {
        mutableStateOf(item.purchasedQuantity.toString().takeIf { it != "0.0" } ?: "")
    }
    // State for unit price text field. Keyed by item.id.
    var unitPriceText by remember(item.id) {
        mutableStateOf(item.unitPrice?.toString() ?: "")
    }

    // Effect to synchronize purchasedQuantityText with item.purchasedQuantity from the model
    LaunchedEffect(item.purchasedQuantity) {
        val modelPurchasedQty = item.purchasedQuantity
        val textAsDouble = purchasedQuantityText.toDoubleOrNull()
        // Update text field if it doesn't match the model, avoiding overwrite of user's partial input like "."
        if (textAsDouble != modelPurchasedQty) {
            purchasedQuantityText = if (modelPurchasedQty > 0.0) modelPurchasedQty.toString() else ""
        }
    }

    // Effect to synchronize unitPriceText with item.unitPrice from the model
    LaunchedEffect(item.unitPrice) {
        val modelPrice = item.unitPrice
        val textAsDouble = unitPriceText.toDoubleOrNull()
        if (modelPrice == null) {
            if (unitPriceText.isNotEmpty() && unitPriceText != ".") {
                unitPriceText = ""
            }
        } else {
            if (textAsDouble != modelPrice) {
                unitPriceText = modelPrice.toString()
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchasedQuantity > 0.0 && item.unitPrice != null) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            // Display the PLANNED quantity
            Text(
                text = "${stringResource(R.string.planned_quantity_display)}: ${item.plannedQuantity} ${item.unit.orEmpty()}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = purchasedQuantityText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            purchasedQuantityText = newValue
                            val newPurchasedQuantity = newValue.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(purchasedQuantity = newPurchasedQuantity))
                        } else if (newValue.isEmpty()) {
                            purchasedQuantityText = ""
                            onUpdate(item.copy(purchasedQuantity = 0.0))
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
                        if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                            unitPriceText = newValue
                            val newPrice = newValue.toDoubleOrNull()
                            onUpdate(item.copy(unitPrice = newPrice))
                        }
                    },
                    label = { Text(stringResource(R.string.unit_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_item))
                }
            }
        }
    }
}

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
                            onDismissRequest()
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

@Preview(showBackground = true)
@Composable
fun PreviewShoppingListScreen() {
    ExpenseTrackerTheme {
        // Correctly create ViewModel for preview using the factory
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val shoppingListViewModel: ShoppingListViewModel = viewModel(factory = ShoppingListViewModelFactory(application))
        val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))

        ShoppingListScreen(
            shoppingListViewModel = shoppingListViewModel,
            expenseViewModel = expenseViewModel
        )
    }
}
