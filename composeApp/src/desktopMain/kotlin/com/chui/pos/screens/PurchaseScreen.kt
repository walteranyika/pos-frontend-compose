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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.components.CreatePurchaseDialog
import com.chui.pos.viewmodels.PurchaseViewModel
import org.koin.compose.koinInject

object PurchaseScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<PurchaseViewModel>()
        val state by remember { derivedStateOf { viewModel.uiState } }

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
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(purchase.ref, style = MaterialTheme.typography.titleMedium)
                                        Text(purchase.purchaseDate ?: "", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Supplier: ${purchase.supplier ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Total Cost: ${purchase.totalCost}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Items:", style = MaterialTheme.typography.labelSmall)
                                    purchase.items.forEach { item ->
                                        Text("- ${item.productName} (Qty: ${item.quantity}, Cost: ${item.costPrice})")
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