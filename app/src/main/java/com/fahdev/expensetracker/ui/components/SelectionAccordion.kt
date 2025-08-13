package com.fahdev.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.GridItem
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.Supplier

@Composable
fun <T> SelectionAccordion(
    title: String,
    selectedItem: T?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    defaultIcon: ImageVector
) where T : Any {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
    ) {
        AccordionHeader(
            title = title,
            selectedItemName = selectedItem?.let { getItemName(it) },
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(max = 350.dp)
                ) {
                    items(items) { item ->
                        GridItem(
                            item = item,
                            isSelected = item == selectedItem,
                            onClick = { onItemSelected(item) },
                            defaultIcon = defaultIcon
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccordionHeader(
    title: String,
    selectedItemName: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedItemName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedItemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

fun getItemName(item: Any): String {
    return when (item) {
        is Supplier -> item.name
        is Category -> item.name
        is Product -> item.name
        else -> "Unknown"
    }
}
