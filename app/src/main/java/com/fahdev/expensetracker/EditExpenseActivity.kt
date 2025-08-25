package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EditExpenseActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expenseId = intent.getIntExtra("EXPENSE_ID", -1)
        if (expenseId == -1) finish()

        setContent {
            ExpenseTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EditExpenseScreen(
                        viewModel = viewModel,
                        expenseId = expenseId,
                        onFinished = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    viewModel: ExpenseViewModel,
    expenseId: Int,
    onFinished: () -> Unit
) {
    val expense by viewModel.getExpenseWithDetailsById(expenseId).collectAsStateWithLifecycle(null)
    if (expense == null) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        return
    }

    var selectedDate by rememberSaveable { mutableLongStateOf(expense!!.expense.date) }
    var amountText by rememberSaveable { mutableStateOf(expense!!.expense.amount.toString()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat.getDateInstance() }
    val displayedDate = dateFormatter.format(Date(selectedDate))

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.edit_expense_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = expense!!.productWithCategory.product.name,
                onValueChange = {},
                label = { Text(stringResource(R.string.product)) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = expense!!.supplier.name,
                onValueChange = {},
                label = { Text(stringResource(R.string.supplier)) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.clickable { showDatePickerDialog = true }) {
                OutlinedTextField(
                    value = displayedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date)) },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
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

            if (showDatePickerDialog) {
                val dialogDatePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dialogDatePickerState.selectedDateMillis?.let {
                                selectedDate = it
                            }
                            showDatePickerDialog = false
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = dialogDatePickerState)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.invalid_amount),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        showError = true
                        return@Button
                    }
                    showError = false

                    viewModel.updateExpense(
                        Expense(
                            id = expense!!.expense.id,
                            productId = expense!!.productWithCategory.product.id,
                            supplierId = expense!!.supplier.id,
                            amount = amount,
                            date = selectedDate
                        )
                    )
                    onFinished()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.deleteExpense(expense!!.expense)
                    onFinished()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}