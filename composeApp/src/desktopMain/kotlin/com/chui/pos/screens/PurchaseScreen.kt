package com.chui.pos.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.components.CreatePurchaseDialog
import com.chui.pos.viewmodels.PurchaseViewModel
import org.koin.compose.koinInject
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Helper to format currency
private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)
}

// Helper to format date string
private fun formatDate(dateString: String?): String {
    if (dateString == null) return "N/A"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        odt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"))
    } catch (e: Exception) {
        dateString // Fallback to original string if parsing fails
    }
}


object PurchaseScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<PurchaseViewModel>()
        val state by remember { derivedStateOf { viewModel.uiState } }

        // This ensures that purchases are loaded when the screen is first displayed.
        LaunchedEffect(Unit) {
            viewModel.loadPurchases()
        }

        if (state.isCreateDialogVisible) {
            CreatePurchaseDialog(viewModel)
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = viewModel::showCreateDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Create Purchase")
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                TopAppBar(title = { Text("Purchases") })

                if (state.isLoading && state.purchases.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (state.error != null) {
                    Text(
                        "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(state.purchases, key = { it.id }) { purchase ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(purchase.ref, style = MaterialTheme.typography.titleMedium)
                                        Text(formatDate(purchase.purchaseDate), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Supplier: ${purchase.supplier ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Total Cost: ${formatCurrency(purchase.totalCost)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("Items:", style = MaterialTheme.typography.titleSmall)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    // Use a Column for the list of items inside the card
                                    Column {
                                        purchase.items.forEach { item ->
                                            ListItem(
                                                headlineContent = { Text(item.productName) },
                                                supportingContent = { Text("Quantity: ${item.quantity}") },
                                                trailingContent = { Text(formatCurrency(item.costPrice)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}