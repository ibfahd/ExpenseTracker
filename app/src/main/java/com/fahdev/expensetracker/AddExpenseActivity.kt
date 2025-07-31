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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.components.LetterAvatar
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddExpenseActivity : AppCompatActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        expenseViewModel.resetAddExpenseFlow()
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
        }
    ) { innerPadding ->
        AddExpenseFlow(
            modifier = Modifier.padding(innerPadding),
            expenseViewModel = expenseViewModel,
            onSaveSuccess = onBackClick
        )
    }
}

@Composable
fun AddExpenseFlow(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    val selectedSupplier by expenseViewModel.selectedSupplierForAdd.collectAsState()

    // This is the key change: we now use the filtered list of categories.
    val categoriesForSupplier by expenseViewModel.categoriesForSelectedSupplier.collectAsState()
    val selectedCategory by expenseViewModel.selectedCategoryForAdd.collectAsState()

    val products by expenseViewModel.productsForAddScreen.collectAsState()

    LaunchedEffect(selectedCategory) {
        selectedProduct = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SelectionGrid(
            title = stringResource(R.string.supplier),
            items = allSuppliers,
            selectedItem = selectedSupplier,
            onItemSelected = { expenseViewModel.onSupplierSelected(it) },
            defaultIcon = Icons.Outlined.Storefront
        )

        AnimatedVisibility(visible = selectedSupplier != null) {
            Column {
                Spacer(Modifier.height(24.dp))
                SelectionGrid(
                    title = stringResource(R.string.category),
                    items = categoriesForSupplier, // Use the filtered list here
                    selectedItem = selectedCategory,
                    onItemSelected = { expenseViewModel.onCategorySelected(it) },
                    defaultIcon = Icons.Outlined.Category
                )
            }
        }

        AnimatedVisibility(visible = selectedCategory != null) {
            Column {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    item {
                        AddNewItemCard(
                            text = stringResource(R.string.add_new_product_short),
                            onClick = { showAddProductDialog = true }
                        )
                    }
                    items(products) { product ->
                        val isSelected = product == selectedProduct
                        GridItem(
                            item = product,
                            isSelected = isSelected,
                            onClick = { selectedProduct = product },
                            defaultIcon = Icons.AutoMirrored.Outlined.Label
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedProduct != null) {
            Column {
                Spacer(Modifier.height(24.dp))
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
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull()
                        if (amountDouble == null || amountDouble <= 0) {
                            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
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
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.save_expense), fontSize = 16.sp)
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onProductAdded = { newProduct ->
                selectedProduct = newProduct
                showAddProductDialog = false
            },
            selectedCategory = selectedCategory,
            expenseViewModel = expenseViewModel
        )
    }
}

@Composable
fun <T> SelectionGrid(
    title: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    defaultIcon: ImageVector
) where T : Any {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                GridItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemSelected(item) },
                    defaultIcon = defaultIcon
                )
            }
        }
    }
}


@Composable
fun <T> GridItem(
    item: T,
    isSelected: Boolean,
    onClick: () -> Unit,
    defaultIcon: ImageVector
) where T : Any {
    val name: String
    val iconName: String?
    val colorHex: String?

    when (item) {
        is Supplier -> {
            name = item.name
            iconName = null // Suppliers will always use the LetterAvatar
            colorHex = item.colorHex
        }
        is Category -> { name = item.name; iconName = item.iconName; colorHex = item.colorHex }
        is Product -> { name = item.name; iconName = item.iconName; colorHex = item.colorHex }
        else -> throw IllegalArgumentException("Unsupported type for GridItem")
    }

    val itemColor = colorHex?.let { IconAndColorUtils.colorMap[it] } ?: MaterialTheme.colorScheme.surfaceVariant
    val onColor = if (itemColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else itemColor),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!iconName.isNullOrBlank()) {
                val icon = IconAndColorUtils.iconMap[iconName] ?: defaultIcon
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    modifier = Modifier.size(36.dp),
                    tint = if (isSelected) Color.White else onColor
                )
            } else {
                LetterAvatar(
                    name = name,
                    size = 36.dp,
                    backgroundColor = if (isSelected) Color.White.copy(alpha = 0.2f) else onColor.copy(alpha = 0.2f),
                    contentColor = if (isSelected) Color.White else onColor
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                textAlign = TextAlign.Center,
                color = if (isSelected) Color.White else onColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AddNewItemCard(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = text,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (Product) -> Unit,
    selectedCategory: Category?,
    expenseViewModel: ExpenseViewModel
) {
    var newProductName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (selectedCategory == null) {
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
                            val existingProduct = expenseViewModel.getProductByNameInCategory(newProductName, selectedCategory.id)
                            if (existingProduct != null) {
                                Toast.makeText(context, context.getString(R.string.product_name_exists_in_category), Toast.LENGTH_SHORT).show()
                            } else {
                                val newProduct = Product(name = newProductName, categoryId = selectedCategory.id)
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
