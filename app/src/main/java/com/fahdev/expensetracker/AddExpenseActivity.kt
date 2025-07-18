package com.fahdev.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
    val allProducts by expenseViewModel.allProducts.collectAsState(initial = emptyList())
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productSearchQuery by remember { mutableStateOf("") }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var supplierSearchQuery by remember { mutableStateOf("") }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductSelectedCategory by remember { mutableStateOf<Category?>(null) }
    var newProductCategorySearchQuery by remember { mutableStateOf("") }
    var newProductCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(
            expanded = productDropdownExpanded,
            onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { newValue ->
                    productSearchQuery = newValue
                    selectedProduct = null
                    productDropdownExpanded = true
                },
                label = { Text(stringResource(R.string.select_product)) },
                readOnly = false,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_new_product, productSearchQuery)) },
                        onClick = {
                            newProductName = productSearchQuery
                            newProductSelectedCategory = null
                            newProductCategorySearchQuery = ""
                            showAddProductDialog = true
                            productDropdownExpanded = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                filteredProducts.forEach { product ->
                    val productCategory = allCategories.find { it.id == product.categoryId }?.name ?: stringResource(R.string.unknown_category)
                    DropdownMenuItem(
                        text = { Text("${product.name} (${productCategory})") },
                        onClick = {
                            selectedProduct = product
                            productSearchQuery = product.name
                            productDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(
            expanded = supplierDropdownExpanded,
            onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = supplierSearchQuery,
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
                if (filteredSuppliers.isEmpty() && supplierSearchQuery.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_new_supplier, supplierSearchQuery)) },
                        onClick = {
                            newSupplierName = supplierSearchQuery
                            showAddSupplierDialog = true
                            supplierDropdownExpanded = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
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
        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                newProductName = ""
                newProductSelectedCategory = null
                newProductCategorySearchQuery = ""
            },
            title = { Text(stringResource(R.string.add_new_product_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
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
                                                Toast.makeText(context, context.getString(R.string.category_added), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.failed_to_add_category), Toast.LENGTH_SHORT).show()
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
                                    if (newId != -1L) {
                                        selectedProduct = Product(
                                            id = newId.toInt(),
                                            name = newProductName,
                                            categoryId = newProductSelectedCategory!!.id
                                        )
                                        productSearchQuery = newProductName
                                        Toast.makeText(context, context.getString(R.string.product_added), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_add_product), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedProduct = existingProduct
                                    productSearchQuery = existingProduct.name
                                    Toast.makeText(context, context.getString(R.string.product_exists), Toast.LENGTH_SHORT).show()
                                }
                                showAddProductDialog = false
                                newProductName = ""
                                newProductSelectedCategory = null
                                newProductCategorySearchQuery = ""
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.product_name_and_category_empty), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(stringResource(R.string.add_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddProductDialog = false
                    newProductName = ""
                    newProductSelectedCategory = null
                    newProductCategorySearchQuery = ""
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text(stringResource(R.string.add_new_supplier_title)) },
            text = {
                OutlinedTextField(
                    value = newSupplierName,
                    onValueChange = { newSupplierName = it },
                    label = { Text(stringResource(R.string.supplier_name_label)) },
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
                                        Toast.makeText(context, context.getString(R.string.supplier_added), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_add_supplier), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedSupplier = existingSupplier
                                    supplierSearchQuery = existingSupplier.name
                                    Toast.makeText(context, context.getString(R.string.supplier_exists), Toast.LENGTH_SHORT).show()
                                }
                                showAddSupplierDialog = false
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.supplier_name_empty), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(stringResource(R.string.add_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseScreenPreview() {
    ExpenseTrackerTheme {
        //val context = LocalContext.current
        //val application = context.applicationContext as Application
        //val factory = ExpenseViewModelFactory(application)
        //val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
        //AddExpenseScreen(expenseViewModel = expenseViewModel, onBackClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseFormPreview() {
    ExpenseTrackerTheme {
        //val context = LocalContext.current
        //val application = context.applicationContext as Application
        //val factory = ExpenseViewModelFactory(application)
        //val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
        //AddExpenseForm(expenseViewModel = expenseViewModel, onSaveSuccess = {})
    }
}