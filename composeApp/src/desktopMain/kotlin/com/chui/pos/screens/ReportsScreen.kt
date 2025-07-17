package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.SaleSummaryResponse
import com.chui.pos.viewmodels.ReportsUiState
import com.chui.pos.viewmodels.ReportsViewModel
import org.koin.compose.koinInject

object ReportsScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinInject<ReportsViewModel>()

        Row(Modifier.fillMaxSize().padding(16.dp)) {
            // Left Panel: Details
            Box(modifier = Modifier.weight(1.2f).padding(end = 8.dp)) {
                SaleDetailView(viewModel.selectedSale)
            }

            // Right Panel: List and Filters
            Card(modifier = Modifier.weight(2f).padding(start = 8.dp)) {
                SaleListView(viewModel)
            }
        }
    }
}

@Composable
private fun SaleDetailView(sale: SaleSummaryResponse?) {
    Card(modifier = Modifier.fillMaxSize()) {
        if (sale == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a sale to view details", style = MaterialTheme.typography.titleMedium)
            }
            return@Card
        }

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sale Summary
            item {
                Text("Sale Details", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Ref: ${sale.ref}", style = MaterialTheme.typography.bodyLarge)
                Text("Cashier: ${sale.cashier}", style = MaterialTheme.typography.bodyMedium)
                Text("Date: ${formatDateTime(sale.saleDate)}", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            // Items
            item {
                Text("Items (${sale.items.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
            }
            items(sale.items) { item ->
                ListItem(
                    headlineContent = { Text(item.productName) },
                    supportingContent = { Text("Qty: ${item.quantity} @ ${"%.2f".format(item.price)}") },
                    trailingContent = { Text("%.2f".format(item.total)) }
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

            // Payments
            item {
                Text("Payments (${sale.payments.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
            }
            items(sale.payments) { payment ->
                ListItem(
                    headlineContent = { Text(payment.method) },
                    trailingContent = { Text("%.2f".format(payment.amount)) }
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

            // Totals
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text("Grand Total: %.2f".format(sale.grandTotal), style = MaterialTheme.typography.titleMedium)
                    Text("Discount: %.2f".format(sale.discount), style = MaterialTheme.typography.bodyMedium)
                    Text("Paid Amount: %.2f".format(sale.paidAmount), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SaleListView(viewModel: ReportsViewModel) {
    val uiState by viewModel.uiState
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recent Sales", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Filter Controls
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("Search by cashier or product...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (viewModel.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            }
        )
        // TODO: Add Date Pickers for start and end date if needed
        Spacer(Modifier.height(16.dp))

        // Sales List
        when (val state = uiState) {
            is ReportsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ReportsUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message) }
            is ReportsUiState.Success -> {
                if (state.salesPage.content.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sales found for the selected criteria.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.salesPage.content, key = { it.id }) { sale ->
                            ListItem(
                                headlineContent = { Text(sale.ref, style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { Text("Cashier: ${sale.cashier} • ${formatDateTime(sale.saleDate)}") },
                                trailingContent = { Text("%.2f".format(sale.grandTotal)) },
                                modifier = Modifier.clickable { viewModel.onSaleSelected(sale) }
                            )
                            HorizontalDivider()
                        }
                        // TODO: Add pagination controls (e.g., "Load More" button)
                    }
                }
            }
        }
    }
}

private fun formatDateTime(dateTimeString: String): String {
    return try {
        val odt = java.time.OffsetDateTime.parse(dateTimeString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        odt.format(formatter)
    } catch (e: Exception) {
        dateTimeString // fallback
    }
}