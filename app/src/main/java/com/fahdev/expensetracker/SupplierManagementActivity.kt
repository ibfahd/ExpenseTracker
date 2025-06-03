package com.fahdev.expensetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class SupplierManagementActivity : ComponentActivity() {
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

    // State for showing the Add/Edit Supplier dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    // State to hold the supplier being edited (null for adding new)
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    // State for the text field in the dialog
    var supplierNameInput by remember { mutableStateOf("") }

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Suppliers") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    supplierToEdit = null // Indicate adding a new supplier
                    supplierNameInput = "" // Clear input field
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, "Add new supplier")
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
                    text = "No suppliers yet. Tap '+' to add one!",
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

    // Add/Edit Supplier Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (supplierToEdit == null) "Add Supplier" else "Edit Supplier") },
            text = {
                TextField(
                    value = supplierNameInput,
                    onValueChange = { supplierNameInput = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (supplierNameInput.isNotBlank()) {
                        scope.launch {
                            val existingSupplier = expenseViewModel.getSupplierByName(supplierNameInput)
                            if (existingSupplier != null && existingSupplier.id != supplierToEdit?.id) {
                                snackbarHostState.showSnackbar("Supplier with this name already exists!")
                            } else {
                                if (supplierToEdit == null) {
                                    // Add new supplier
                                    expenseViewModel.addSupplier(Supplier(name = supplierNameInput))
                                    snackbarHostState.showSnackbar("Supplier added!")
                                } else {
                                    // Update existing supplier
                                    val updatedSupplier = supplierToEdit!!.copy(name = supplierNameInput)
                                    expenseViewModel.updateSupplier(updatedSupplier)
                                    snackbarHostState.showSnackbar("Supplier updated!")
                                }
                                showAddEditDialog = false
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Supplier name cannot be empty.") }
                    }
                }) {
                    Text(if (supplierToEdit == null) "Add" else "Save")
                }
            },
            dismissButton = {
                Button(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete supplier '${supplierToDelete?.name}'? This will also affect expenses using this supplier.") },
            confirmButton = {
                Button(
                    onClick = {
                        supplierToDelete?.let { supplier ->
                            scope.launch {
                                expenseViewModel.deleteSupplier(supplier)
                                snackbarHostState.showSnackbar("Supplier deleted!")
                            }
                        }
                        showDeleteDialog = false
                        supplierToDelete = null
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
                        contentDescription = "Edit ${supplier.name}",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { onDeleteClick(supplier) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete ${supplier.name}",
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
        SupplierManagementScreen(expenseViewModel = ExpenseViewModel(Application()))
    }
}