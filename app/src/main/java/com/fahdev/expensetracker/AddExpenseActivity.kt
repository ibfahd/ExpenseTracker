package com.fahdev.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.fahdev.expensetracker.ui.components.SelectionAccordion
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseFlow(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for the form inputs
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // State for accordion expansion
    var isSupplierAccordionExpanded by remember { mutableStateOf(true) }
    var isCategoryAccordionExpanded by remember { mutableStateOf(false) }

    // Data from ViewModel
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    val selectedSupplier by expenseViewModel.selectedSupplierForAdd.collectAsState()
    val categoriesForSupplier by expenseViewModel.categoriesForSelectedSupplier.collectAsState()
    val selectedCategory by expenseViewModel.selectedCategoryForAdd.collectAsState()
    val products by expenseViewModel.productsForAddScreen.collectAsState()

    // Reset product selection when category changes
    LaunchedEffect(selectedCategory) {
        selectedProduct = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Supplier Accordion ---
        SelectionAccordion(
            title = stringResource(R.string.supplier),
            selectedItem = selectedSupplier,
            isExpanded = isSupplierAccordionExpanded,
            onToggle = { isSupplierAccordionExpanded = !isSupplierAccordionExpanded },
            items = allSuppliers,
            onItemSelected = { supplier ->
                expenseViewModel.onSupplierSelected(supplier)
                isSupplierAccordionExpanded = false
                if (selectedSupplier == null) isCategoryAccordionExpanded = true
            },
            defaultIcon = Icons.Outlined.Storefront
        )

        Spacer(Modifier.height(8.dp))

        // --- Category Accordion ---
        AnimatedVisibility(visible = selectedSupplier != null) {
            Column {
                SelectionAccordion(
                    title = stringResource(R.string.category),
                    selectedItem = selectedCategory,
                    isExpanded = isCategoryAccordionExpanded,
                    onToggle = { isCategoryAccordionExpanded = !isCategoryAccordionExpanded },
                    items = categoriesForSupplier,
                    onItemSelected = { category ->
                        expenseViewModel.onCategorySelected(category)
                        isCategoryAccordionExpanded = false
                    },
                    defaultIcon = Icons.Outlined.Category
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // --- Product Selection and Final Inputs ---
        AnimatedVisibility(visible = selectedCategory != null) {
            Column {
                Text(stringResource(R.string.product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp) // Constrain height to prevent excessive scrolling
                ) {
                    item {
                        AddNewItemCard(
                            text = stringResource(R.string.add_new_product_short),
                            onClick = { showAddProductDialog = true }
                        )
                    }
                    items(products) { product ->
                        GridItem(
                            item = product,
                            isSelected = product == selectedProduct,
                            onClick = { selectedProduct = product },
                            defaultIcon = Icons.AutoMirrored.Outlined.Label
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Amount and Date Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f).clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date)) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull()
                        if (amountDouble == null || amountDouble <= 0 || selectedProduct == null) {
                            Toast.makeText(context, context.getString(R.string.please_fill_amount_product_supplier), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val newExpense = Expense(
                                amount = amountDouble,
                                productId = selectedProduct!!.id,
                                supplierId = selectedSupplier!!.id,
                                date = selectedDate
                            )
                            expenseViewModel.addExpense(newExpense)
                            Toast.makeText(context, context.getString(R.string.expense_saved_successfully), Toast.LENGTH_SHORT).show()
                            onSaveSuccess()
                        }
                    },
                    enabled = selectedProduct != null && amount.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.save_expense), fontSize = 16.sp)
                }
            }
        }
    }

    // --- Dialogs ---
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = it
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
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

@OptIn(ExperimentalMaterial3Api::class)
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