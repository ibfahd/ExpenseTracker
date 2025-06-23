package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class SupplierManagementActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                SupplierManagementScreen(expenseViewModel = expenseViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierManagementScreen(expenseViewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())

    var showAddEditDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierNameInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }

    var showDeleteWithExpensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_suppliers)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    supplierToEdit = null
                    supplierNameInput = ""
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_new_supplier_desc))
            }
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
            if (allSuppliers.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_suppliers_placeholder),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(allSuppliers) { supplier ->
                        SupplierItem(
                            supplier = supplier,
                            onEditClick = {
                                supplierToEdit = it
                                supplierNameInput = it.name
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                supplierToDelete = it
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                val titleRes = if (supplierToEdit == null) R.string.add_supplier_title else R.string.edit_supplier_title
                Text(stringResource(titleRes))
            },
            text = {
                TextField(
                    value = supplierNameInput,
                    onValueChange = { supplierNameInput = it },
                    label = { Text(stringResource(R.string.supplier_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (supplierNameInput.isNotBlank()) {
                        showAddEditDialog = false
                        scope.launch {
                            val existingSupplier = expenseViewModel.getSupplierByName(supplierNameInput)
                            if (existingSupplier != null && existingSupplier.id != supplierToEdit?.id) {
                                snackbarHostState.showSnackbar(context.getString(R.string.supplier_exists_error))
                            } else {
                                if (supplierToEdit == null) {
                                    expenseViewModel.addSupplier(Supplier(name = supplierNameInput))
                                    //snackbarHostState.showSnackbar(context.getString(R.string.supplier_added_success))
                                } else {
                                    val updatedSupplier = supplierToEdit!!.copy(name = supplierNameInput)
                                    expenseViewModel.updateSupplier(updatedSupplier)
                                    //snackbarHostState.showSnackbar(context.getString(R.string.supplier_updated_success))
                                }
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.supplier_name_empty_error)) }
                    }
                }) {
                    val textRes = if (supplierToEdit == null) R.string.add_button else R.string.save_button
                    Text(stringResource(textRes))
                }
            },
            dismissButton = {
                Button(onClick = { showAddEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.delete_supplier_confirmation_message, supplierToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        supplierToDelete?.let { supplier ->
                            scope.launch {
                                if (expenseViewModel.supplierHasExpenses(supplier.id)) {
                                    // Supplier has expenses, so show the second dialog instead of deleting
                                    showDeleteDialog = false
                                    showDeleteWithExpensesDialog = true
                                } else {
                                    // No expenses, delete directly
                                    expenseViewModel.deleteSupplier(supplier)
                                    showDeleteDialog = false
                                    supplierToDelete = null
                                }
                                //snackbarHostState.showSnackbar(context.getString(R.string.supplier_deleted_success))
                            }
                        }
                        //showDeleteDialog = false
                        //supplierToDelete = null
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
    if (showDeleteWithExpensesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWithExpensesDialog = false },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            // Use the new, more descriptive warning message
            text = { Text(stringResource(id = R.string.delete_supplier_with_expenses_confirmation, supplierToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        supplierToDelete?.let { expenseViewModel.deleteSupplierAndExpenses(it) }
                        showDeleteWithExpensesDialog = false
                        supplierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteWithExpensesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SupplierItem(
    supplier: Supplier,
    onEditClick: (Supplier) -> Unit,
    onDeleteClick: (Supplier) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = supplier.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onEditClick(supplier) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_supplier_desc, supplier.name),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { onDeleteClick(supplier) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_supplier_desc, supplier.name),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SupplierManagementScreenPreview() {
    ExpenseTrackerTheme {
        // Correctly create ViewModel for preview using the factory
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val factory = ExpenseViewModelFactory(application)
        val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
        SupplierManagementScreen(expenseViewModel = expenseViewModel)
    }
}
