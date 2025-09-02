package com.fahdev.expensetracker.ui.reporting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.ExpenseViewModel
import com.fahdev.expensetracker.R
import com.fahdev.expensetracker.data.ProductReportDetail
import java.text.NumberFormat

@Composable
fun ProductDetailsReportContent(expenseViewModel: ExpenseViewModel, currencyFormatter: NumberFormat) {
    val productReportDetails by expenseViewModel.productReportDetails.collectAsState()
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (productReportDetails.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        stringResource(R.string.report_no_product_data),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            val groupedProducts = productReportDetails.groupBy { it.categoryName }
            groupedProducts.forEach { (categoryName, productsInCategory) ->
                item {
                    CategoryAccordionHeader(
                        categoryName = categoryName,
                        isExpanded = expandedCategories[categoryName] == true,
                        onToggle = {
                            expandedCategories[categoryName] = expandedCategories[categoryName] != true
                        }
                    )
                }
                item {
                    AnimatedVisibility(visible = expandedCategories[categoryName] == true) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            productsInCategory.forEach { productDetail ->
                                ProductReportItem(productDetail = productDetail, currencyFormatter = currencyFormatter)
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProductReportItem(productDetail: ProductReportDetail, currencyFormatter: NumberFormat) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Text(productDetail.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        ProductStatRow(
            label = stringResource(R.string.report_product_total_spent),
            value = currencyFormatter.format(productDetail.totalAmountSpent ?: 0.0)
        )
        ProductStatRow(
            label = stringResource(R.string.report_product_lowest_price),
            value = currencyFormatter.format(productDetail.lowestTransactionAmount ?: 0.0)
        )
        ProductStatRow(
            label = stringResource(R.string.report_product_cheapest_supplier),
            value = productDetail.cheapestSupplierName ?: stringResource(R.string.report_not_available)
        )
    }
}

@Composable
fun ProductStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CategoryAccordionHeader(
    categoryName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse $categoryName" else "Expand $categoryName",
            modifier = Modifier.size(30.dp).rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
