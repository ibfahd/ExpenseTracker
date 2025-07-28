package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.components.LetterAvatar
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SupplierManagementActivity : AppCompatActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
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
    var selectedColorHex by remember { mutableStateOf<String?>(null) }

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
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                    selectedColorHex = null
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
                EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(id = R.string.no_suppliers_title),
                    description = stringResource(id = R.string.no_suppliers_description)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSuppliers, key = { it.id }) { supplier ->
                        SupplierItem(
                            supplier = supplier,
                            onEditClick = {
                                supplierToEdit = it
                                supplierNameInput = it.name
                                selectedColorHex = it.colorHex
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
        AddEditSupplierDialog(
            supplierToEdit = supplierToEdit,
            supplierName = supplierNameInput,
            onNameChange = { supplierNameInput = it },
            selectedColorHex = selectedColorHex,
            onColorChange = { selectedColorHex = it },
            onDismiss = { showAddEditDialog = false },
            onConfirm = {
                scope.launch {
                    val existingSupplier = expenseViewModel.getSupplierByName(supplierNameInput)
                    if (existingSupplier != null && existingSupplier.id != supplierToEdit?.id) {
                        snackbarHostState.showSnackbar(context.getString(R.string.supplier_exists_error))
                    } else {
                        if (supplierToEdit == null) {
                            expenseViewModel.addSupplier(
                                Supplier(name = supplierNameInput, colorHex = selectedColorHex)
                            )
                        } else {
                            val updatedSupplier = supplierToEdit!!.copy(
                                name = supplierNameInput,
                                colorHex = selectedColorHex
                            )
                            expenseViewModel.updateSupplier(updatedSupplier)
                        }
                        showAddEditDialog = false
                    }
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
                                    showDeleteDialog = false
                                    showDeleteWithExpensesDialog = true
                                } else {
                                    expenseViewModel.deleteSupplier(supplier)
                                    showDeleteDialog = false
                                    supplierToDelete = null
                                }
                            }
                        }
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
    val supplierColor = supplier.colorHex?.let { IconAndColorUtils.colorMap[it] } ?: MaterialTheme.colorScheme.surfaceVariant
    val onSupplierColor = if (supplierColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = supplierColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LetterAvatar(
                name = supplier.name,
                backgroundColor = onSupplierColor.copy(alpha = 0.2f),
                contentColor = onSupplierColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = supplier.name,
                style = MaterialTheme.typography.titleMedium,
                color = onSupplierColor,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onEditClick(supplier) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_supplier_desc, supplier.name),
                        tint = onSupplierColor
                    )
                }
                IconButton(onClick = { onDeleteClick(supplier) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_supplier_desc, supplier.name),
                        tint = onSupplierColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplierToEdit: Supplier?,
    supplierName: String,
    onNameChange: (String) -> Unit,
    selectedColorHex: String?,
    onColorChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplierToEdit == null) stringResource(R.string.add_supplier_title) else stringResource(R.string.edit_supplier_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.supplier_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                if (supplierName.isNotBlank()) {
                    onConfirm()
                }
            }) {
                Text(if (supplierToEdit == null) stringResource(R.string.add_button) else stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
