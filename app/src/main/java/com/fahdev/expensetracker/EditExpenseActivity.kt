package com.fahdev.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
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

    var selectedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Initialize state when expense is loaded
    LaunchedEffect(expense) {
        expense?.let {
            selectedDate = it.expense.date
            amountText = it.expense.amount.toString()
        }
    }

    val dateFormatter = remember { SimpleDateFormat.getDateInstance() }
    val displayedDate = selectedDate?.let { dateFormatter.format(Date(it)) }
        ?: stringResource(R.string.select_date)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Product (read-only)
        OutlinedTextField(
            value = expense?.productWithCategory?.product?.name.orEmpty(),
            onValueChange = {},
            label = { Text(stringResource(R.string.product)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Supplier (read-only)
        OutlinedTextField(
            value = expense?.supplier?.name.orEmpty(),
            onValueChange = {},
            label = { Text(stringResource(R.string.supplier)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Date (editable via picker)
        OutlinedTextField(
            value = displayedDate,
            onValueChange = {},
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
            val dialogDatePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
            )
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
                DatePicker(state = dialogDatePickerState)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Amount (editable)
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text(stringResource(R.string.amount)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Save button
        Button(
            onClick = {
                val pid = expense?.productWithCategory?.product?.id ?: return@Button
                val sid = expense?.supplier?.id ?: return@Button
                val date = selectedDate ?: System.currentTimeMillis()
                val amount = amountText.toDoubleOrNull() ?: return@Button

                viewModel.updateExpense(
                    Expense(
                        id = expense?.expense?.id ?: 0,
                        productId = pid,
                        supplierId = sid,
                        amount = amount,
                        date = date
                    )
                )
                onFinished()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }

        Spacer(Modifier.height(12.dp))

        // Delete button
        Button(
            onClick = {
                expense?.expense?.let {
                    viewModel.deleteExpense(it)
                }
                onFinished()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.delete))
        }
    }
}
