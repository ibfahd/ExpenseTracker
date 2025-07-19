package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
    val currentSupplierId by shoppingListViewModel.currentSupplierId.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())
    var expandedSupplierDropdown by remember { mutableStateOf(false) }
    var showAddShoppingItemDialog by remember { mutableStateOf(false) }
    var newProductIdForAddItemDialog by remember { mutableStateOf<Int?>(null) }
    var newProductTextForAddItemDialog by remember { mutableStateOf("") }
    var newPlannedQuantityForAddItemDialog by remember { mutableStateOf("") }
    var newUnitForAddItemDialog by remember { mutableStateOf("") }
    var expandedProductDropdownForAddItemDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductNameForAddProductDialog by remember { mutableStateOf("") }
    var showConfirmValidateDialog by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
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
                    newPlannedQuantityForAddItemDialog = ""
                    newUnitForAddItemDialog = ""
                    expandedProductDropdownForAddItemDialog = false
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_new_shopping_item))
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Button(
                    onClick = { showConfirmValidateDialog = true },
                    enabled = validationStats.hasValidItems && !isValidating,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.processing_purchases),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        BadgedBox(
                            badge = {
                                if (validationStats.validItemsCount > 0) {
                                    Badge { Text(validationStats.validItemsCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (validationStats.hasValidItems) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (validationStats.hasValidItems) stringResource(R.string.complete_shopping) else stringResource(R.string.no_items_ready),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            // Top section with supplier selection
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
            // Main content area for the list
            if (shoppingListItems.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ShoppingCartCheckout,
                        title = stringResource(id = R.string.no_items_in_list_title),
                        description = stringResource(id = R.string.no_items_in_list_description)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
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
            // The summary text is now here, at the bottom of the main content area,
            // just above the BottomAppBar.
            if (validationStats.totalItems > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            R.string.validation_summary,
                            validationStats.validItemsCount,
                            validationStats.totalItems
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (validationStats.totalCost > 0) {
                        Text(
                            text = stringResource(R.string.total_cost, validationStats.totalCost),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    if (showConfirmValidateDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmValidateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.complete_shopping_title))
                }
            },
            text = {
                Column {
                    Text(stringResource(R.string.confirm_purchases_message))
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.validation_summary_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.items_to_record,
                                    validationStats.validItemsCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (validationStats.totalCost > 0) {
                                Text(
                                    text = stringResource(R.string.total_amount, validationStats.totalCost),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                                if (recordedCount > 0) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.expenses_recorded_successfully, recordedCount)
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.no_valid_purchases_to_record)
                                    )
                                }
                            } finally {
                                isValidating = false
                                showConfirmValidateDialog = false
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.record_expenses),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmValidateDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showAddShoppingItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddShoppingItemDialog = false },
            title = { Text(stringResource(R.string.add_new_item_title)) },
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
                                    text = { Text(stringResource(R.string.add_new_product, newProductTextForAddItemDialog)) },
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
                        value = newPlannedQuantityForAddItemDialog,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                                newPlannedQuantityForAddItemDialog = newValue
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
                        val plannedQuantity = newPlannedQuantityForAddItemDialog.toDoubleOrNull()
                        if (productId != null && plannedQuantity != null && plannedQuantity > 0) {
                            shoppingListViewModel.addShoppingItem(productId, newUnitForAddItemDialog.ifBlank { null }, plannedQuantity)
                            showAddShoppingItemDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.item_added_to_shopping_list)) }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.select_product_and_enter_quantity),
                                    withDismissAction = true
                                )
                            }
                        }
                    },
                    enabled = newProductIdForAddItemDialog != null && newPlannedQuantityForAddItemDialog.toDoubleOrNull() != null && (newPlannedQuantityForAddItemDialog.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(stringResource(R.string.add_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddShoppingItemDialog = false }) { Text(stringResource(R.string.cancel)) }
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
    var purchasedQuantityText by remember(item.id) {
        mutableStateOf(item.purchasedQuantity.toString().takeIf { it != "0.0" } ?: "")
    }
    var unitPriceText by remember(item.id) {
        mutableStateOf(item.unitPrice?.toString() ?: "")
    }
    LaunchedEffect(item.purchasedQuantity) {
        val modelPurchasedQty = item.purchasedQuantity
        val textAsDouble = purchasedQuantityText.toDoubleOrNull()
        if (textAsDouble != modelPurchasedQty) {
            purchasedQuantityText = if (modelPurchasedQty > 0.0) modelPurchasedQty.toString() else ""
        }
    }
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    var newProductNameDialog by remember { mutableStateOf(initialProductName) }
    var newProductSelectedCategory by remember { mutableStateOf<Category?>(null) }
    var newProductCategorySearchQuery by remember { mutableStateOf("") }
    var newProductCategoryDropdownExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.add_new_product_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newProductNameDialog,
                    onValueChange = { newProductNameDialog = it },
                    label = { Text(stringResource(R.string.product_name_label)) },
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
                        label = { Text(stringResource(R.string.category)) },
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
                                text = { Text(stringResource(R.string.add_new_category, newProductCategorySearchQuery)) },
                                onClick = {
                                    coroutineScope.launch {
                                        val existingCategory = expenseViewModel.getCategoryByName(newProductCategorySearchQuery)
                                        val categoryToAdd = existingCategory ?: Category(name = newProductCategorySearchQuery)
                                        val categoryId = existingCategory?.id?.toLong() ?: expenseViewModel.addCategory(categoryToAdd)

                                        if (categoryId != -1L) {
                                            newProductSelectedCategory = categoryToAdd.copy(id = categoryId.toInt())
                                            newProductCategorySearchQuery = newProductSelectedCategory!!.name
                                            snackbarHostState.showSnackbar(context.getString(R.string.category_added))
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.failed_to_add_category))
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
                                    snackbarHostState.showSnackbar(context.getString(R.string.product_added))
                                } else {
                                    snackbarHostState.showSnackbar(context.getString(R.string.failed_to_add_product))
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
                }
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
        //val context = LocalContext.current
        //val application = context.applicationContext as Application
        //val shoppingListViewModel: ShoppingListViewModel = viewModel(factory = ShoppingListViewModelFactory(application))
        //val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
        /*ShoppingListScreen(
            shoppingListViewModel = shoppingListViewModel,
            expenseViewModel = expenseViewModel
        )*/
    }
}