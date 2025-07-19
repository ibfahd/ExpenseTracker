package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditExpenseActivity : AppCompatActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseId = intent.getIntExtra("EXPENSE_ID", -1)
                EditExpenseScreen(
                    expenseViewModel = expenseViewModel,
                    expenseId = expenseId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(expenseViewModel: ExpenseViewModel, expenseId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentExpenseWithDetails by remember { mutableStateOf<ExpenseWithDetails?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var selectedSupplierId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) } // Milliseconds
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val allProducts by expenseViewModel.allProducts.collectAsState(initial = emptyList())
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())
    LaunchedEffect(expenseId) {
        if (expenseId != -1) {
            expenseViewModel.getExpenseWithDetailsById(expenseId)
                .filterNotNull() // Ensure we only proceed if expense is found
                .collect { expenseDetails ->
                    currentExpenseWithDetails = expenseDetails
                    amount = "%.2f".format(expenseDetails.expense.amount)
                    selectedProductId = expenseDetails.expense.productId
                    selectedSupplierId = expenseDetails.expense.supplierId
                    selectedDate = expenseDetails.expense.timestamp
                }
        }
    }
    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    val selectedProductName = allProducts.find { it.id == selectedProductId }?.name ?: stringResource(R.string.select_product)
    val selectedSupplierName = allSuppliers.find { it.id == selectedSupplierId }?.name ?: stringResource(R.string.select_supplier)
    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_expense_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button_desc))
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display a loading indicator or message until data is loaded
            if (currentExpenseWithDetails == null && expenseId != -1) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading_expense_details))
            } else if (currentExpenseWithDetails == null && expenseId == -1) {
                Text(stringResource(R.string.error_expense_not_found))
            } else {
                TextField(
                    value = amount,
                    onValueChange = { newValue ->
                        amount = newValue.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                // Product Picker
                TextField(
                    value = selectedProductName,
                    onValueChange = { /* Read-only */ },
                    label = { Text(stringResource(R.string.product)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProductPicker = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.select_product_icon))
                    }
                )
                if (showProductPicker) {
                    ProductOrSupplierPickerDialog(
                        title = stringResource(R.string.select_product),
                        items = allProducts,
                        onDismissRequest = { showProductPicker = false },
                        onItemClick = { product ->
                            selectedProductId = product.id
                            showProductPicker = false
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Supplier Picker
                TextField(
                    value = selectedSupplierName,
                    onValueChange = { /* Read-only */ },
                    label = { Text(stringResource(R.string.supplier)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSupplierPicker = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.select_supplier_icon))
                    }
                )
                if (showSupplierPicker) {
                    ProductOrSupplierPickerDialog(
                        title = stringResource(R.string.select_supplier),
                        items = allSuppliers,
                        onDismissRequest = { showSupplierPicker = false },
                        onItemClick = { supplier ->
                            selectedSupplierId = supplier.id
                            showSupplierPicker = false
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Date Picker
                TextField(
                    value = SimpleDateFormat("MMM dd,yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    onValueChange = { /* Read-only */ },
                    label = { Text(stringResource(R.string.date)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.select_date))
                    }
                )
                if (showDatePickerDialog) {
                    val dialogDatePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate) // <<< CORRECTED LINE
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            Button(onClick = {
                                dialogDatePickerState.selectedDateMillis?.let {
                                    selectedDate = it
                                }
                                showDatePickerDialog = false
                            }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDatePickerDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = dialogDatePickerState) // Use the dialog-specific state
                    }
                }
                Spacer(Modifier.height(32.dp))
                // Save Changes Button
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()
                        if (parsedAmount != null && selectedProductId != null && selectedSupplierId != null && currentExpenseWithDetails != null) {
                            val updatedExpense = currentExpenseWithDetails!!.expense.copy(
                                amount = parsedAmount,
                                productId = selectedProductId!!,
                                supplierId = selectedSupplierId!!,
                                timestamp = selectedDate
                            )
                            scope.launch {
                                expenseViewModel.updateExpense(updatedExpense)
                                snackbarHostState.showSnackbar(context.getString(R.string.expense_updated_successfully))
                                (context as? ComponentActivity)?.finish() // Go back after update
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.fill_all_fields))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_changes))
                }
                Spacer(Modifier.height(8.dp))
                // Delete Expense Button
                Button(
                    onClick = { showDeleteDialog = true }, // Show confirmation dialog
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_expense))
                }
            }
        }
    }
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.are_you_sure_delete_expense)) },
            confirmButton = {
                Button(
                    onClick = {
                        currentExpenseWithDetails?.expense?.let { expenseToDelete ->
                            scope.launch {
                                expenseViewModel.deleteExpense(expenseToDelete)
                                snackbarHostState.showSnackbar(context.getString(R.string.expense_deleted_successfully))
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> ProductOrSupplierPickerDialog(
    title: String,
    items: List<T>,
    onDismissRequest: () -> Unit,
    onItemClick: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items) { item ->
                    Text(
                        text = (item as? Product)?.name ?: (item as? Supplier)?.name ?: stringResource(R.string.unknown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun EditExpenseScreenPreview() {
    ExpenseTrackerTheme {
        //val context = LocalContext.current
        //val application = context.applicationContext as Application
        //val factory = ExpenseViewModelFactory(application)
        //val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
        //EditExpenseScreen(expenseViewModel = expenseViewModel, expenseId = -1)
    }
}