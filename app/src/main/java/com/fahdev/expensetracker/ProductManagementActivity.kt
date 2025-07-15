package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button_desc))
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_product_title))
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
                        snackbarHostState.showSnackbar(context.getString(R.string.product_name_exists_in_category))
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
                            snackbarHostState.showSnackbar(context.getString(R.string.cannot_delete_product_has_expenses))
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
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_product_title))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_button))
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
        title = { Text(if (productToEdit == null) stringResource(R.string.add_product_title) else stringResource(R.string.edit_product_title)) },
        text = {
            TextField(
                value = productName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.product_name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                if (productName.isNotBlank()) {
                    onConfirm()
                }
            }) {
                Text(if (productToEdit == null) stringResource(R.string.add_button) else stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.delete_product_confirmation, productToDelete.name)) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}