package com.fahdev.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.ExpenseViewModel
import com.fahdev.expensetracker.R
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Supplier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    expenseViewModel: ExpenseViewModel,
    allCategories: List<Category>,
    allSuppliers: List<Supplier>,
    onDismiss: () -> Unit
) {
    val selectedStartDate by expenseViewModel.selectedStartDate.collectAsState(initial = null)
    val selectedEndDate by expenseViewModel.selectedEndDate.collectAsState(initial = null)
    val selectedCategoryId by expenseViewModel.selectedCategoryId.collectAsState(initial = null)
    val selectedSupplierId by expenseViewModel.selectedSupplierId.collectAsState(initial = null)
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedStartDate,
        initialSelectedEndDateMillis = selectedEndDate
    )
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.filter_expenses)) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(context.getString(R.string.filter_by_date), style = MaterialTheme.typography.titleSmall)
                DateFilterDropdown(
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onDateOptionSelected = { option ->
                        expenseViewModel.setDateRangeFilter(option)
                    }
                )
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(context.getString(R.string.select_custom_date_range))
                }
                Spacer(Modifier.height(8.dp))
                Text(context.getString(R.string.filter_by_category), style = MaterialTheme.typography.titleSmall)
                CategoryFilterDropdown(
                    allCategories = allCategories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { categoryId ->
                        expenseViewModel.setCategoryFilter(categoryId)
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(context.getString(R.string.filter_by_supplier), style = MaterialTheme.typography.titleSmall)
                SupplierFilterDropdown(
                    allSuppliers = allSuppliers,
                    selectedSupplierId = selectedSupplierId,
                    onSupplierSelected = { supplierId ->
                        expenseViewModel.setSupplierFilter(supplierId)
                    }
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        expenseViewModel.resetFilters()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text(context.getString(R.string.clear_all_filters))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(context.getString(R.string.apply_filters_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startDateMillis = dateRangePickerState.selectedStartDateMillis
                        var endDateMillis = dateRangePickerState.selectedEndDateMillis
                        if (startDateMillis != null && endDateMillis == null) {
                            val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.set(Calendar.MILLISECOND, 999)
                            endDateMillis = cal.timeInMillis
                        }
                        val normalizedStartDate = startDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        val normalizedEndDate = endDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        }
                        expenseViewModel.setCustomDateRangeFilter(normalizedStartDate, normalizedEndDate)
                        showDatePicker = false
                    }
                ) {
                    Text(context.getString(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.padding(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterDropdown(
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    onDateOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateOptions = listOf(
        "ThisMonth" to context.getString(R.string.this_month),
        "Last7Days" to context.getString(R.string.last_7_days),
        "LastMonth" to context.getString(R.string.last_month),
        "ThisYear" to context.getString(R.string.this_year),
        "All" to context.getString(R.string.all_time)
    )
    val currentSelectionText = remember(selectedStartDate, selectedEndDate) {
        when {
            selectedStartDate == null && selectedEndDate == null -> context.getString(R.string.all_time)
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val startStr = selectedStartDate?.let { sdf.format(Date(it)) } ?: "N/A"
                val endStr = selectedEndDate?.let { sdf.format(Date(it)) } ?: "N/A"
                if (selectedStartDate != null && selectedEndDate != null && selectedStartDate == selectedEndDate) {
                    startStr
                } else if (selectedStartDate != null && selectedEndDate != null) {
                    "$startStr - $endStr"
                } else {
                    context.getString(R.string.select_date_range)
                }
            }
        }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = currentSelectionText,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.date_range_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dateOptions.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onDateOptionSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    allCategories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedCategoryName = allCategories.find { it.id == selectedCategoryId }?.name ?: context.getString(R.string.all_categories)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedCategoryName,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.all_categories)) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            allCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFilterDropdown(
    allSuppliers: List<Supplier>,
    selectedSupplierId: Int?,
    onSupplierSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedSupplierName = allSuppliers.find { it.id == selectedSupplierId }?.name ?: context.getString(R.string.all_suppliers)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedSupplierName,
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.supplier)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.all_suppliers)) },
                onClick = {
                    onSupplierSelected(null)
                    expanded = false
                }
            )
            allSuppliers.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplier.name) },
                    onClick = {
                        onSupplierSelected(supplier.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
