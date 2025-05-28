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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class CategoryManagementActivity : ComponentActivity() {
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

    // State for showing the Add/Edit Category dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    // State to hold the category being edited (null for adding new)
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    // State for the text field in the dialog
    var categoryNameInput by remember { mutableStateOf("") }

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
                    categoryToEdit = null // Indicate adding a new category
                    categoryNameInput = "" // Clear input field
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, "Add new category")
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
                    text = "No categories yet. Tap '+' to add one!",
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

    // Add/Edit Category Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (categoryToEdit == null) "Add Category" else "Edit Category") },
            text = {
                TextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (categoryNameInput.isNotBlank()) {
                        scope.launch {
                            val existingCategory = expenseViewModel.getCategoryByName(categoryNameInput)
                            if (existingCategory != null && existingCategory.id != categoryToEdit?.id) {
                                snackbarHostState.showSnackbar("Category with this name already exists!")
                            } else {
                                if (categoryToEdit == null) {
                                    // Add new category
                                    expenseViewModel.addCategory(Category(name = categoryNameInput))
                                    snackbarHostState.showSnackbar("Category added!")
                                } else {
                                    // Update existing category
                                    val updatedCategory = categoryToEdit!!.copy(name = categoryNameInput)
                                    expenseViewModel.updateCategory(updatedCategory)
                                    snackbarHostState.showSnackbar("Category updated!")
                                }
                                showAddEditDialog = false
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Category name cannot be empty.") }
                    }
                }) {
                    Text(if (categoryToEdit == null) "Add" else "Save")
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
            text = { Text("Are you sure you want to delete category '${categoryToDelete?.name}'? This will also affect expenses using this category.") },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { category ->
                            scope.launch {
                                expenseViewModel.deleteCategory(category)
                                snackbarHostState.showSnackbar("Category deleted!")
                            }
                        }
                        showDeleteDialog = false
                        categoryToDelete = null
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
fun CategoryItem(
    category: Category,
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit
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
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEditClick(category) },
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { onDeleteClick(category) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryManagementScreenPreview() {
    ExpenseTrackerTheme {
        CategoryManagementScreen(expenseViewModel = ExpenseViewModel(Application()))
    }
}