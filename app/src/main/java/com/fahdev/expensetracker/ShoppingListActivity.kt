package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class ShoppingListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme { // Replace with your actual app theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Call your ShoppingListScreen composable here
                    ShoppingListScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingListViewModel: ShoppingListViewModel = viewModel()
) {
    val currentSupplierId by shoppingListViewModel.currentSupplierId.collectAsState()
    val allSuppliers by shoppingListViewModel.allSuppliers.collectAsState(initial = emptyList())
    val shoppingListItems by shoppingListViewModel.shoppingListItems.collectAsState(initial = emptyList())
    val allProducts by shoppingListViewModel.allProducts.collectAsState(initial = emptyList())

    // State for New Item
    var newProductId by remember { mutableStateOf<Int?>(null) }
    var newProductText by remember { mutableStateOf("") }
    var newQuantity by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("") }

    // State for Supplier Dropdown
    var expandedSupplierDropdown by remember { mutableStateOf(false) }

    // State for Product Dropdown
    var expandedProductDropdown by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope() // Now it will be used
    val snackbarHostState = remember { SnackbarHostState() } // State for Snackbar

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.shopping_list_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // FIX: Add SnackbarHost
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

            // Start New Trip Button
            Button(
                onClick = { shoppingListViewModel.startNewTripForSupplier() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_new_trip_button))
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider() // Corrected from Divider

            Spacer(Modifier.height(16.dp))

            // Add New Shopping Item Section
            Text(stringResource(R.string.add_new_item_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedProductDropdown,
                onExpandedChange = { expandedProductDropdown = !expandedProductDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = newProductText,
                    onValueChange = { newProductText = it; newProductId = null }, // Reset ID if text changes
                    label = { Text(stringResource(R.string.product_name_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductDropdown) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedProductDropdown,
                    onDismissRequest = { expandedProductDropdown = false }
                ) {
                    allProducts.filter { it.name.contains(newProductText, ignoreCase = true) || newProductText.isBlank() }
                        .forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    newProductText = product.name
                                    newProductId = product.id
                                    expandedProductDropdown = false
                                }
                            )
                        }
                }
            }


            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newQuantity,
                    onValueChange = { newQuantity = it },
                    label = { Text(stringResource(R.string.quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = newUnit,
                    onValueChange = { newUnit = it },
                    label = { Text(stringResource(R.string.unit_label)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val productId = newProductId
                    val quantity = newQuantity.toDoubleOrNull()
                    if (productId != null && quantity != null) {
                        shoppingListViewModel.addShoppingItem(productId, newUnit.ifBlank { null }, quantity)
                        newProductText = ""
                        newProductId = null
                        newQuantity = ""
                        newUnit = ""
                    } else {
                        // FIX: Use Snackbar for user feedback
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Please select a product and enter a valid quantity.",
                                withDismissAction = true // Allows user to dismiss
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newProductId != null && newQuantity.toDoubleOrNull() != null
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item_button))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_item_button))
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider() // Corrected from Divider

            Spacer(Modifier.height(16.dp))

            // Shopping List Items Display
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
                            allProducts = allProducts // Pass products to resolve names
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onUpdate: (ShoppingListItem) -> Unit, // Callback for when item is updated
    onDelete: (ShoppingListItem) -> Unit, // Callback for when item is deleted
    allProducts: List<Product>, // List of all products to get product name
    modifier: Modifier = Modifier
) {
    val productName = allProducts.find { it.id == item.productId }?.name ?: stringResource(R.string.unknown_product)

    var purchasedQuantityText by remember(item.quantity) {
        mutableStateOf(if (item.quantity > 0.0) item.quantity.toString() else "")
    }
    var unitPriceText by remember(item.unitPrice) {
        mutableStateOf(item.unitPrice?.toString() ?: "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
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
                // *** CORRECTED LINE HERE ***
                // Use item.quantity for the planned/current quantity display
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
                        purchasedQuantityText = newValue
                        val newQuantity = newValue.toDoubleOrNull() ?: 0.0 // Default to 0.0 if invalid
                        onUpdate(item.copy(quantity = newQuantity))
                    },
                    label = { Text(stringResource(R.string.purchased_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { newValue ->
                        unitPriceText = newValue
                        val newPrice = newValue.toDoubleOrNull() // Null if invalid
                        onUpdate(item.copy(unitPrice = newPrice))
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

// Preview Composable (for design preview in Android Studio)
@Preview(showBackground = true)
@Composable
fun PreviewShoppingListScreen() {
    // You'd typically provide a mock ViewModel for previews
    // For simplicity, this just shows the basic layout
    MaterialTheme {
        ShoppingListScreen()
    }
}