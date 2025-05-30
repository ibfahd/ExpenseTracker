package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))

                // Get the expense ID passed from MainActivity
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

    // State for the expense details, initialized to null
    var currentExpenseWithDetails by remember { mutableStateOf<ExpenseWithDetails?>(null) }

    // Initial states for editable fields, will be updated once data loads
    var amount by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var selectedSupplierId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) } // Milliseconds

    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Collect all products, suppliers, and the specific expense
    val allProducts by expenseViewModel.allProducts.collectAsState(initial = emptyList())
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())

    // Fetch the expense details once when the screen is launched
    LaunchedEffect(expenseId) {
        if (expenseId != -1) {
            expenseViewModel.getExpenseWithDetailsById(expenseId)
                .filterNotNull() // Ensure we only proceed if expense is found
                .collect { expenseDetails ->
                    currentExpenseWithDetails = expenseDetails
                    // Populate UI states with fetched data
                    amount = "%.2f".format(expenseDetails.expense.amount) // Format for TextField
                    selectedProductId = expenseDetails.expense.productId
                    selectedSupplierId = expenseDetails.expense.supplierId
                    selectedDate = expenseDetails.expense.timestamp
                    // No need for setSelection here, it's handled when dialog is opened
                }
        }
    }

    // State for showing product/supplier pickers
    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }

    // Get selected product and supplier names for display
    val selectedProductName = allProducts.find { it.id == selectedProductId }?.name ?: "Select Product"
    val selectedSupplierName = allSuppliers.find { it.id == selectedSupplierId }?.name ?: "Select Supplier"

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                CircularProgressIndicator() // Show a loading spinner
                Text("Loading expense details...")
            } else if (currentExpenseWithDetails == null && expenseId == -1) {
                Text("Error: Expense not found or ID missing.")
            } else { // Data is loaded, display editable fields
                TextField(
                    value = amount,
                    onValueChange = { newValue ->
                        amount = newValue.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // Product Picker
                TextField(
                    value = selectedProductName,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Product") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProductPicker = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = "Select Product")
                    }
                )
                if (showProductPicker) {
                    ProductOrSupplierPickerDialog(
                        title = "Select Product",
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
                    label = { Text("Supplier") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSupplierPicker = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = "Select Supplier")
                    }
                )
                if (showSupplierPicker) {
                    ProductOrSupplierPickerDialog(
                        title = "Select Supplier",
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
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                    }
                )
                if (showDatePickerDialog) {
                    // Correct: Create DatePickerState here so it's initialized with the latest selectedDate
                    val dialogDatePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate) // <<< CORRECTED LINE
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            Button(onClick = {
                                dialogDatePickerState.selectedDateMillis?.let {
                                    selectedDate = it // Update your source of truth (selectedDate)
                                }
                                showDatePickerDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDatePickerDialog = false }) {
                                Text("Cancel")
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
                                snackbarHostState.showSnackbar("Expense updated successfully!")
                                (context as? ComponentActivity)?.finish() // Go back after update
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill all fields correctly.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
                Spacer(Modifier.height(8.dp))

                // Delete Expense Button
                Button(
                    onClick = { showDeleteDialog = true }, // Show confirmation dialog
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Expense")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                Button(
                    onClick = {
                        currentExpenseWithDetails?.expense?.let { expenseToDelete ->
                            scope.launch {
                                expenseViewModel.deleteExpense(expenseToDelete)
                                snackbarHostState.showSnackbar("Expense deleted successfully!")
                                (context as? ComponentActivity)?.finish() // Go back after delete
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Reusing ProductOrSupplierPickerDialog from AddExpenseActivity
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
                        text = (item as? Product)?.name ?: (item as? Supplier)?.name ?: "Unknown",
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
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun EditExpenseScreenPreview() {
    ExpenseTrackerTheme {
        // Provide a dummy ID for preview purposes; actual data won't load
        EditExpenseScreen(expenseViewModel = ExpenseViewModel(Application()), expenseId = 1)
    }
}