package com.fahdev.expensetracker

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.ui.components.EmptyState
import com.fahdev.expensetracker.ui.components.LetterAvatar
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import com.fahdev.expensetracker.ui.utils.IconAndColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryManagementActivity : AppCompatActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
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
    val allSuppliers by expenseViewModel.allSuppliers.collectAsState(initial = emptyList())

    var showAddEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories)) },
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
                    categoryToEdit = null
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
                EmptyState(
                    icon = Icons.Outlined.Category,
                    title = stringResource(id = R.string.no_categories_title),
                    description = stringResource(id = R.string.no_categories_description)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            onEditClick = {
                                categoryToEdit = it
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
        AddEditCategoryDialog(
            categoryToEdit = categoryToEdit,
            allSuppliers = allSuppliers,
            expenseViewModel = expenseViewModel,
            onDismiss = { showAddEditDialog = false },
            snackbarHostState = snackbarHostState
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
                                    snackbarHostState.showSnackbar(context.getString(R.string.delete_category_error_has_products))
                                } else {
                                    expenseViewModel.deleteCategory(category)
                                }
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
    val context = LocalContext.current
    val categoryColor = category.colorHex?.let { IconAndColorUtils.colorMap[it] } ?: MaterialTheme.colorScheme.surfaceVariant
    val onCategoryColor = if (categoryColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, ProductManagementActivity::class.java).apply {
                    putExtra("CATEGORY_ID", category.id)
                    putExtra("CATEGORY_NAME", category.name)
                }
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(containerColor = categoryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val icon: ImageVector = category.iconName?.let { IconAndColorUtils.iconMap[it] } ?: Icons.Outlined.Category
            Icon(
                imageVector = icon,
                contentDescription = category.name,
                tint = onCategoryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = onCategoryColor,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onEditClick(category) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_category_desc, category.name),
                        tint = onCategoryColor
                    )
                }
                IconButton(onClick = { onDeleteClick(category) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_category_desc, category.name),
                        tint = onCategoryColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    categoryToEdit: Category?,
    allSuppliers: List<Supplier>,
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var categoryName by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var selectedIconName by remember { mutableStateOf(categoryToEdit?.iconName) }
    var selectedColorHex by remember { mutableStateOf(categoryToEdit?.colorHex) }

    val linkedSupplierIds by if (categoryToEdit != null) {
        expenseViewModel.getLinkedSupplierIds(categoryToEdit.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val checkedSupplierIds = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(linkedSupplierIds) {
        checkedSupplierIds.clear()
        linkedSupplierIds.forEach { id ->
            checkedSupplierIds[id] = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoryToEdit == null) stringResource(R.string.add_category_title) else stringResource(R.string.edit_category_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
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
                            onClick = { selectedIconName = iconInfo.name },
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
                                .clickable { selectedColorHex = colorInfo.hex }
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

                if (allSuppliers.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("Link Suppliers", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn {
                            items(allSuppliers, key = { it.id }) { supplier ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val isChecked = checkedSupplierIds[supplier.id] ?: false
                                            checkedSupplierIds[supplier.id] = !isChecked
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkedSupplierIds[supplier.id] ?: false,
                                        onCheckedChange = { isChecked ->
                                            checkedSupplierIds[supplier.id] = isChecked
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    LetterAvatar(name = supplier.name, size = 24.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = supplier.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.category_name_empty_error)) }
                        return@Button
                    }
                    scope.launch {
                        val existingCategory = expenseViewModel.getCategoryByName(categoryName)
                        if (existingCategory != null && existingCategory.id != categoryToEdit?.id) {
                            snackbarHostState.showSnackbar(context.getString(R.string.category_exists_error))
                        } else {
                            // --- FIX IS HERE ---
                            val newOrUpdatedCategory = (categoryToEdit ?: Category(name = "")).copy(
                                name = categoryName,
                                iconName = selectedIconName,
                                colorHex = selectedColorHex
                            )

                            val linkedIds = checkedSupplierIds.filter { it.value }.keys.toList()

                            if (categoryToEdit == null) {
                                val newId = expenseViewModel.addCategory(newOrUpdatedCategory)
                                expenseViewModel.saveSupplierLinksForCategory(newId.toInt(), linkedIds)
                            } else {
                                expenseViewModel.updateCategory(newOrUpdatedCategory)
                                expenseViewModel.saveSupplierLinksForCategory(newOrUpdatedCategory.id, linkedIds)
                            }
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (categoryToEdit == null) stringResource(R.string.add_button) else stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
