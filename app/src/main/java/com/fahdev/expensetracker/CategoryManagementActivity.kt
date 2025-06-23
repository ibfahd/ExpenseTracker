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
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import android.content.Intent

class CategoryManagementActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(application))
                CategoryManagementScreen(expenseViewModel = expenseViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(expenseViewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allCategories by expenseViewModel.allCategories.collectAsState(initial = emptyList())

    var showAddEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryNameInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories)) },
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
                    categoryToEdit = null
                    categoryNameInput = ""
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_new_category_desc))
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
            if (allCategories.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_categories_placeholder),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(allCategories) { category ->
                        CategoryItem(
                            category = category,
                            onEditClick = {
                                categoryToEdit = it
                                categoryNameInput = it.name
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                categoryToDelete = it
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
                val titleRes = if (categoryToEdit == null) R.string.add_category_title else R.string.edit_category_title
                Text(stringResource(titleRes))
            },
            text = {
                TextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (categoryNameInput.isNotBlank()) {
                        showAddEditDialog = false
                        scope.launch {
                            val existingCategory = expenseViewModel.getCategoryByName(categoryNameInput)
                            if (existingCategory != null && existingCategory.id != categoryToEdit?.id) {
                                snackbarHostState.showSnackbar(context.getString(R.string.category_exists_error))
                            } else {
                                if (categoryToEdit == null) {
                                    expenseViewModel.addCategory(Category(name = categoryNameInput))
                                    //snackbarHostState.showSnackbar(context.getString(R.string.category_added_success))
                                } else {
                                    val updatedCategory = categoryToEdit!!.copy(name = categoryNameInput)
                                    expenseViewModel.updateCategory(updatedCategory)
                                    //snackbarHostState.showSnackbar(context.getString(R.string.category_updated_success))
                                }
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.category_name_empty_error)) }
                    }
                }) {
                    val textRes = if (categoryToEdit == null) R.string.add_button else R.string.save_button
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
            text = { Text(stringResource(R.string.delete_category_confirmation_message, categoryToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { category ->
                            scope.launch {
                                if (expenseViewModel.hasProductsInCategory(category.id)) {
                                    // If it has products, show an error message
                                    snackbarHostState.showSnackbar(context.getString(R.string.delete_category_error_has_products))
                                } else {
                                    // Otherwise, proceed with the deletion
                                    expenseViewModel.deleteCategory(category)
                                }
                                //snackbarHostState.showSnackbar(context.getString(R.string.category_deleted_success))
                            }
                        }
                        showDeleteDialog = false
                        categoryToDelete = null
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

@Composable
fun CategoryItem(
    category: Category,
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit
) {
    val context = LocalContext.current // Get context for the Intent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { // Make the whole card clickable
                val intent = Intent(context, ProductManagementActivity::class.java).apply {
                    putExtra("CATEGORY_ID", category.id)
                    putExtra("CATEGORY_NAME", category.name)
                }
                context.startActivity(intent)
            },
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
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onEditClick(category) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_category_desc, category.name),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { onDeleteClick(category) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_category_desc, category.name),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryManagementScreenPreview() {
    ExpenseTrackerTheme {
        // Correctly create ViewModel for preview using the factory
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val factory = ExpenseViewModelFactory(application)
        val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
        CategoryManagementScreen(expenseViewModel = expenseViewModel)
    }
}
