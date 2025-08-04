package com.chui.pos.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chui.pos.viewmodels.PurchaseViewModel

@Composable
fun CreatePurchaseDialog(viewModel: PurchaseViewModel) {
    val state by remember { derivedStateOf { viewModel.createPurchaseState } }

    Dialog(onDismissRequest = viewModel::hideCreateDialog) {
        Card(modifier = Modifier.width(800.dp).heightIn(max = 600.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create New Purchase", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.supplier,
                    onValueChange = viewModel::onSupplierChanged,
                    label = { Text("Supplier (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // Product Search
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = { Text("Search for products to add") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.searchResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(state.searchResults) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                modifier = Modifier.clickable { viewModel.addProductToPurchase(product) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Added Items
                Text("Items to Purchase", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.items, key = { it.productId }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${item.code}: ${item.name}", // Replace with product name if you fetch it
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { viewModel.updatePurchaseItem(item.productId, it, item.costPrice.toString()) },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.costPrice.toString(),
                                onValueChange = { viewModel.updatePurchaseItem(item.productId, item.quantity.toString(), it) },
                                label = { Text("Cost") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeProductFromPurchase(item.productId) }) {
                                Icon(Icons.Default.Delete, "Remove")
                            }
                        }
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = viewModel::hideCreateDialog) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = viewModel::submitPurchase) {
                        Text("Submit Purchase")
                    }
                }
            }
        }
    }
}