package com.fahdev.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddExpenseActivity : AppCompatActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                AddExpenseScreen(
                    expenseViewModel = expenseViewModel,
                    onBackClick = { finish() }
                )
            }
        }
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
                title = { Text(stringResource(R.string.add_new_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
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
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var supplierSearchQuery by remember { mutableStateOf("") }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    val selectedCategoryId by expenseViewModel.selectedCategoryIdForAdd.collectAsState()
    val productsInCategory by expenseViewModel.productsInCategory.collectAsState()

    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())

    var showAddProductDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Reset product selection if category changes
    LaunchedEffect(selectedCategoryId) {
        selectedProduct = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Amount
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                    amount = newValue
                }
            },
            label = { Text(stringResource(R.string.amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        // Category Selection
        Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(allCategories) { category ->
                val isSelected = category.id == selectedCategoryId
                Button(
                    onClick = { expenseViewModel.selectCategoryForAdd(category.id) },
                    colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(category.name)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Product Selection
        AnimatedVisibility(visible = selectedCategoryId != null) {
            Column {
                Text(stringResource(R.string.product), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // Use a fixed height for the grid area to prevent scroll conflicts
                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(productsInCategory) { product ->
                            val isSelected = product.id == selectedProduct?.id
                            OutlinedButton(
                                onClick = { selectedProduct = product },
                                colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) else ButtonDefaults.outlinedButtonColors(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(product.name, textAlign = TextAlign.Center)
                            }
                        }
                        item {
                            Button(
                                onClick = { showAddProductDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.add_new_product_short))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Supplier Selection
        ExposedDropdownMenuBox(
            expanded = supplierDropdownExpanded,
            onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedSupplier?.name ?: supplierSearchQuery,
                onValueChange = { newValue ->
                    supplierSearchQuery = newValue
                    selectedSupplier = null
                    supplierDropdownExpanded = true
                },
                label = { Text(stringResource(R.string.select_supplier_title)) },
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
                filteredSuppliers.forEach { supplier ->
                    DropdownMenuItem(
                        text = { Text(supplier.name) },
                        onClick = {
                            selectedSupplier = supplier
                            supplierSearchQuery = supplier.name
                            supplierDropdownExpanded = false
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
                    Toast.makeText(context, context.getString(R.string.please_fill_amount_product_supplier), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    val newExpense = Expense(
                        amount = amountDouble,
                        productId = selectedProduct!!.id,
                        supplierId = selectedSupplier!!.id
                    )
                    expenseViewModel.addExpense(newExpense)
                    Toast.makeText(context, context.getString(R.string.expense_saved), Toast.LENGTH_SHORT).show()
                    onSaveSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.save_expense))
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onProductAdded = { newProduct ->
                selectedProduct = newProduct // Automatically select the new product
                showAddProductDialog = false
            },
            selectedCategoryId = selectedCategoryId,
            expenseViewModel = expenseViewModel
        )
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (Product) -> Unit,
    selectedCategoryId: Int?,
    expenseViewModel: ExpenseViewModel
) {
    var newProductName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (selectedCategoryId == null) {
        // This should not happen if the button is only visible when a category is selected
        Toast.makeText(context, "Please select a category first.", Toast.LENGTH_SHORT).show()
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_product_title)) },
        text = {
            OutlinedTextField(
                value = newProductName,
                onValueChange = { newProductName = it },
                label = { Text(stringResource(R.string.product_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newProductName.isNotBlank()) {
                        coroutineScope.launch {
                            val existingProduct = expenseViewModel.getProductByNameInCategory(newProductName, selectedCategoryId)
                            if (existingProduct != null) {
                                Toast.makeText(context, context.getString(R.string.product_name_exists_in_category), Toast.LENGTH_SHORT).show()
                            } else {
                                val newProduct = Product(name = newProductName, categoryId = selectedCategoryId)
                                val newId = expenseViewModel.addProduct(newProduct)
                                if (newId != -1L) {
                                    onProductAdded(newProduct.copy(id = newId.toInt()))
                                } else {
                                    Toast.makeText(context, context.getString(R.string.failed_to_add_product), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
