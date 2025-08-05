package com.fahdev.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.R
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Supplier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilterStatusRow(
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    selectedCategory: Category?,
    selectedSupplier: Supplier?,
    onResetFilters: () -> Unit
) {
    val context = LocalContext.current
    val filtersActive = selectedStartDate != null || selectedEndDate != null || selectedCategory != null || selectedSupplier != null

    if (filtersActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.active_filters),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    if (selectedStartDate != null && selectedEndDate != null) {
                        val start = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedStartDate))
                        val end = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedEndDate))
                        Text(
                            text = context.getString(R.string.date_range, start, end),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    } else if (selectedStartDate != null) {
                        val start = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedStartDate))
                        Text(
                            text = "From: $start",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (selectedCategory != null) {
                        Text(
                            text = context.getString(R.string.category_filter, selectedCategory.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (selectedSupplier != null) {
                        Text(
                            text = context.getString(R.string.supplier_filter, selectedSupplier.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(onClick = onResetFilters) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = context.getString(R.string.clear_filters),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}