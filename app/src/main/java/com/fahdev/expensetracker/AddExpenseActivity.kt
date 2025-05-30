package com.fahdev.expensetracker

import android.app.Application // Make sure this is imported
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.compose.runtime.collectAsState
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import kotlinx.coroutines.launch

// AddExpenseActivity and ExpenseViewModelFactory are unchanged.

class AddExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))

                AddExpenseScreen(
                    expenseViewModel = expenseViewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

class ExpenseViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseViewModel: ExpenseViewModel,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Expense") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AddExpenseForm(
            expenseViewModel = expenseViewModel,
            modifier = Modifier.padding(innerPadding),
            onSaveSuccess = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    expenseViewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    onSaveSuccess: () -> Unit
) {
    var amount by remember { mutableStateOf("") }

    // --- Product Selection State ---
    val allProducts by expenseViewModel.allProducts.collectAsState(initial = emptyList())
    var selectedProduct by remember { mutableStateOf<Product?>(null) } // Holds selected Product object
    var productSearchQuery by remember { mutableStateOf("") } // For filtering product suggestions
    var productDropdownExpanded by remember { mutableStateOf(false) }

    // --- Supplier Selection State ---
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) } // Holds selected Supplier object
    var supplierSearchQuery by remember { mutableStateOf("") }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    // --- Category Selection for New Product Dialog ---
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList()) // Get all categories from ViewModel
    var showAddProductDialog by remember { mutableStateOf(false) } // Controls "Add New Product" dialog
    var newProductName by remember { mutableStateOf("") }
    var newProductSelectedCategory by remember { mutableStateOf<Category?>(null) } // Holds selected Category object for new product
    var newProductCategorySearchQuery by remember { mutableStateOf("") } // For searching/filtering categories in new product dialog
    var newProductCategoryDropdownExpanded by remember { mutableStateOf(false) } // Controls category dropdown in new product dialog


    var showAddSupplierDialog by remember { mutableStateOf(false) } // Controls "Add New Supplier" dialog
    var newSupplierName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for calling suspend ViewModel functions


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("""^\d*\.?\d{0,2}$"""))) { // Fixed regex warning
                    amount = newValue
                }
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // --- Product Selector (Autocomplete-like dropdown) ---
        ExposedDropdownMenuBox(
            expanded = productDropdownExpanded,
            onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { newValue ->
                    productSearchQuery = newValue
                    selectedProduct = null // Clear selection if user types
                    productDropdownExpanded = true // Keep expanded while typing
                },
                label = { Text("Select Product") },
                readOnly = false, // Allow typing
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true) // Updated menuAnchor for editable text field
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = productDropdownExpanded,
                onDismissRequest = { productDropdownExpanded = false }
            ) {
                val filteredProducts = allProducts.filter {
                    it.name.contains(productSearchQuery, ignoreCase = true)
                }
                if (filteredProducts.isEmpty() && productSearchQuery.isNotBlank()) {
                    // Option to add new product if not found
                    DropdownMenuItem(
                        text = { Text("Add new product: \"$productSearchQuery\"") },
                        onClick = {
                            newProductName = productSearchQuery // Pre-fill new product name
                            // Reset category selection for new product dialog
                            newProductSelectedCategory = null
                            newProductCategorySearchQuery = ""

                            showAddProductDialog = true // Show dialog
                            productDropdownExpanded = false // Close main dropdown
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                filteredProducts.forEach { product ->
                    // Display product name and category (lookup category name from allCategories)
                    val productCategory = allCategories.find { it.id == product.categoryId }?.name ?: "Unknown Category"
                    DropdownMenuItem(
                        text = { Text("${product.name} (${productCategory})") },
                        onClick = {
                            selectedProduct = product
                            productSearchQuery = product.name // Set text field to selected product's name
                            productDropdownExpanded = false // Close dropdown
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // --- Supplier Selector (Autocomplete-like dropdown) ---
        ExposedDropdownMenuBox(
            expanded = supplierDropdownExpanded,
            onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = supplierSearchQuery,
                onValueChange = { newValue ->
                    supplierSearchQuery = newValue
                    selectedSupplier = null // Clear selection if user types
                    supplierDropdownExpanded = true
                },
                label = { Text("Select Supplier") },
                readOnly = false,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = supplierDropdownExpanded,
                onDismissRequest = { supplierDropdownExpanded = false }
            ) {
                val filteredSuppliers = allSuppliers.filter {
                    it.name.contains(supplierSearchQuery, ignoreCase = true)
                }
                if (filteredSuppliers.isEmpty() && supplierSearchQuery.isNotBlank()) {
                    // Option to add new supplier if not found
                    DropdownMenuItem(
                        text = { Text("Add new supplier: \"$supplierSearchQuery\"") },
                        onClick = {
                            newSupplierName = supplierSearchQuery // Pre-fill new supplier name
                            showAddSupplierDialog = true // Show dialog
                            supplierDropdownExpanded = false // Close main dropdown
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                filteredSuppliers.forEach { supplier ->
                    DropdownMenuItem(
                        text = { Text(supplier.name) },
                        onClick = {
                            selectedSupplier = supplier
                            supplierSearchQuery = supplier.name // Set text field to selected supplier's name
                            supplierDropdownExpanded = false // Close dropdown
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                val amountDouble = amount.toDoubleOrNull()
                if (amountDouble == null || selectedProduct == null || selectedSupplier == null) {
                    android.widget.Toast.makeText(context, "Please fill Amount, select Product and Supplier", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch { // Use coroutineScope to call suspend functions
                    val newExpense = Expense(
                        amount = amountDouble,
                        productId = selectedProduct!!.id, // Use !! to assert not null after validation
                        supplierId = selectedSupplier!!.id
                    )
                    expenseViewModel.addExpense(newExpense)
                    android.widget.Toast.makeText(context, "Expense saved!", android.widget.Toast.LENGTH_SHORT).show()
                    onSaveSuccess() // Navigate back to the main screen
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Save Expense")
        }
    }

    // --- Add New Product Dialog ---
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                // Reset fields on dismiss to clean state for next time
                newProductName = ""
                newProductSelectedCategory = null
                newProductCategorySearchQuery = ""
            },
            title = { Text("Add New Product") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Category Selector for new product within the dialog
                    ExposedDropdownMenuBox(
                        expanded = newProductCategoryDropdownExpanded,
                        onExpandedChange = { newProductCategoryDropdownExpanded = !newProductCategoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newProductSelectedCategory?.name ?: newProductCategorySearchQuery, // Display selected category name or search query
                            onValueChange = { newValue ->
                                newProductCategorySearchQuery = newValue
                                newProductSelectedCategory = null // Clear selection if user types
                                newProductCategoryDropdownExpanded = true // Keep expanded while typing
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
                                // Option to add new category if not found
                                DropdownMenuItem(
                                    text = { Text("Add new category: \"$newProductCategorySearchQuery\"") },
                                    onClick = {
                                        coroutineScope.launch {
                                            val existingCategory = expenseViewModel.getCategoryByName(newProductCategorySearchQuery)
                                            val categoryToAdd = existingCategory ?: Category(name = newProductCategorySearchQuery)
                                            // Try to insert; if it exists, insertCategory will return -1.
                                            val categoryId = existingCategory?.id?.toLong() ?: expenseViewModel.addCategory(categoryToAdd)

                                            if (categoryId != -1L) { // Check if insertion was successful or category already existed
                                                newProductSelectedCategory = categoryToAdd.copy(id = categoryId.toInt()) // Select the category (new or existing)
                                                newProductCategorySearchQuery = newProductSelectedCategory!!.name // Update text field
                                                android.widget.Toast.makeText(context, "Category added!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                // This else block is less likely with OnConflictStrategy.IGNORE, but good for robust error handling
                                                android.widget.Toast.makeText(context, "Failed to add category.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            newProductCategoryDropdownExpanded = false // Close category dropdown
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
                                        newProductCategorySearchQuery = category.name // Update search query field
                                        newProductCategoryDropdownExpanded = false // Close category dropdown
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
                        if (newProductName.isNotBlank() && newProductSelectedCategory != null) {
                            coroutineScope.launch {
                                val existingProduct = expenseViewModel.getProductByName(newProductName)
                                if (existingProduct == null) {
                                    val newId = expenseViewModel.addProduct(
                                        Product(
                                            name = newProductName,
                                            categoryId = newProductSelectedCategory!!.id
                                        )
                                    )
                                    if (newId != -1L) { // Check if insertion was successful
                                        // Select the newly added product in the main form's state
                                        selectedProduct = Product(
                                            id = newId.toInt(),
                                            name = newProductName,
                                            categoryId = newProductSelectedCategory!!.id
                                        )
                                        productSearchQuery = newProductName // Set the main product search query field
                                        android.widget.Toast.makeText(context, "Product added!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to add product.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Product already exists, select it instead of adding a new one
                                    selectedProduct = existingProduct
                                    productSearchQuery = existingProduct.name
                                    android.widget.Toast.makeText(context, "Product already exists, selected.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                showAddProductDialog = false // Close the add product dialog
                                // Reset dialog fields after successful add or selection
                                newProductName = ""
                                newProductSelectedCategory = null
                                newProductCategorySearchQuery = ""
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Product name and category cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddProductDialog = false
                    // Reset dialog fields on dismiss
                    newProductName = ""
                    newProductSelectedCategory = null
                    newProductCategorySearchQuery = ""
                }) { Text("Cancel") }
            }
        )
    }

    // --- Add New Supplier Dialog (Unchanged, copied for completeness) ---
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("Add New Supplier") },
            text = {
                OutlinedTextField(
                    value = newSupplierName,
                    onValueChange = { newSupplierName = it },
                    label = { Text("Supplier Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSupplierName.isNotBlank()) {
                            coroutineScope.launch {
                                val existingSupplier = expenseViewModel.getSupplierByName(newSupplierName)
                                if (existingSupplier == null) {
                                    val newId = expenseViewModel.addSupplier(Supplier(name = newSupplierName))
                                    if (newId != -1L) {
                                        selectedSupplier =
                                            Supplier(id = newId.toInt(), name = newSupplierName)
                                        supplierSearchQuery = newSupplierName
                                        android.widget.Toast.makeText(context, "Supplier added!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to add supplier.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedSupplier = existingSupplier
                                    supplierSearchQuery = existingSupplier.name
                                    android.widget.Toast.makeText(context, "Supplier already exists, selected.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                showAddSupplierDialog = false
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Supplier name cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text("Cancel") }
            }
        )
    }
}


// Preview composables for AddExpenseScreen (unchanged)
@Preview(showBackground = true)
@Composable
fun AddExpenseScreenPreview() {
    ExpenseTrackerTheme {
        AddExpenseScreen(expenseViewModel = ExpenseViewModel(Application()), onBackClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseFormPreview() {
    ExpenseTrackerTheme {
        AddExpenseForm(expenseViewModel = ExpenseViewModel(Application()), onSaveSuccess = {})
    }
}