package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductManagementActivity : AppCompatActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getIntExtra("CATEGORY_ID", -1)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Products"

        if (categoryId == -1) {
            finish()
            return
        }

        setContent {
            ExpenseTrackerTheme {
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
    var selectedIconName by remember { mutableStateOf<String?>(null) }
    var selectedColorHex by remember { mutableStateOf<String?>(null) }

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
                selectedIconName = null
                selectedColorHex = null
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
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductItem(
                    product = product,
                    onEditClick = {
                        productToEdit = product
                        productNameInput = product.name
                        selectedIconName = product.iconName
                        selectedColorHex = product.colorHex
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
            selectedIconName = selectedIconName,
            onIconChange = { selectedIconName = it },
            selectedColorHex = selectedColorHex,
            onColorChange = { selectedColorHex = it },
            onDismiss = { showAddEditDialog = false },
            onConfirm = {
                scope.launch {
                    val existingProduct = expenseViewModel.getProductByNameInCategory(productNameInput, categoryId)
                    if (existingProduct != null && existingProduct.id != productToEdit?.id) {
                        snackbarHostState.showSnackbar(context.getString(R.string.product_name_exists_in_category))
                    } else {
                        if (productToEdit == null) {
                            expenseViewModel.addProduct(
                                Product(
                                    name = productNameInput,
                                    categoryId = categoryId,
                                    iconName = selectedIconName,
                                    colorHex = selectedColorHex
                                )
                            )
                        } else {
                            expenseViewModel.updateProduct(
                                productToEdit!!.copy(
                                    name = productNameInput,
                                    iconName = selectedIconName,
                                    colorHex = selectedColorHex
                                )
                            )
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
    val productColor = product.colorHex?.let { IconAndColorUtils.colorMap[it] } ?: MaterialTheme.colorScheme.surfaceVariant
    val onProductColor = if (productColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = productColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon: ImageVector = product.iconName?.let { IconAndColorUtils.iconMap[it] } ?: Icons.AutoMirrored.Outlined.Label
            Icon(
                imageVector = icon,
                contentDescription = product.name,
                tint = onProductColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = product.name,
                modifier = Modifier.weight(1f),
                color = onProductColor,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_product_title), tint = onProductColor)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_button), tint = onProductColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    productName: String,
    onNameChange: (String) -> Unit,
    selectedIconName: String?,
    onIconChange: (String) -> Unit,
    selectedColorHex: String?,
    onColorChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (productToEdit == null) stringResource(R.string.add_product_title) else stringResource(R.string.edit_product_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = productName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.product_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(IconAndColorUtils.iconList) { iconInfo ->
                        val isSelected = iconInfo.name == selectedIconName
                        IconButton(
                            onClick = { onIconChange(iconInfo.name) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(imageVector = iconInfo.icon, contentDescription = iconInfo.name)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(IconAndColorUtils.colorList) { colorInfo ->
                        val isSelected = colorInfo.hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorInfo.color)
                                .clickable { onColorChange(colorInfo.hex) }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
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
