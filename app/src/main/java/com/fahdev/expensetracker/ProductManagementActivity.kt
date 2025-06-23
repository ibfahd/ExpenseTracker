package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class ProductManagementActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getIntExtra("CATEGORY_ID", -1)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Products"

        if (categoryId == -1) {
            finish() // Close if the category ID is invalid
            return
        }

        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                ProductManagementScreen(
                    expenseViewModel = expenseViewModel,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    expenseViewModel: ExpenseViewModel,
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val products by expenseViewModel.getProductsForCategory(categoryId).collectAsState(initial = emptyList())

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productNameInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                productToEdit = null
                productNameInput = ""
                showAddEditDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductItem(
                    product = product,
                    onEditClick = {
                        productToEdit = product
                        productNameInput = product.name
                        showAddEditDialog = true
                    },
                    onDeleteClick = {
                        productToDelete = product
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showAddEditDialog) {
        AddEditProductDialog(
            productToEdit = productToEdit,
            productName = productNameInput,
            onNameChange = { productNameInput = it },
            onDismiss = { showAddEditDialog = false },
            onConfirm = {
                scope.launch {
                    val existingProduct = expenseViewModel.getProductByNameInCategory(productNameInput, categoryId)
                    if (existingProduct != null && existingProduct.id != productToEdit?.id) {
                        snackbarHostState.showSnackbar("Product with this name already exists in this category.")
                    } else {
                        if (productToEdit == null) {
                            expenseViewModel.addProduct(Product(name = productNameInput, categoryId = categoryId))
                        } else {
                            expenseViewModel.updateProduct(productToEdit!!.copy(name = productNameInput))
                        }
                        showAddEditDialog = false
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteProductDialog(
            productToDelete = productToDelete,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                productToDelete?.let { product ->
                    scope.launch {
                        if (expenseViewModel.productHasExpenses(product.id)) {
                            snackbarHostState.showSnackbar("Cannot delete product. It has associated expenses.")
                        } else {
                            expenseViewModel.deleteProduct(product)
                        }
                        showDeleteDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = product.name, modifier = Modifier.weight(1f))
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Product")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Product")
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    productName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (productToEdit == null) "Add Product" else "Edit Product") },
        text = {
            TextField(
                value = productName,
                onValueChange = onNameChange,
                label = { Text("Product Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                if (productName.isNotBlank()) {
                    onConfirm()
                }
            }) {
                Text(if (productToEdit == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteProductDialog(
    productToDelete: Product?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete the product '${productToDelete.name}'?") },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}