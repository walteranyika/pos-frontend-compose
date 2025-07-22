package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.components.CreatePurchaseDialog
import com.chui.pos.dtos.PurchaseResponse
import com.chui.pos.viewmodels.PurchaseViewModel
import org.koin.compose.koinInject
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Helper to format currency
private fun formatCurrency(value: Double): String {
    val kenyaLocale = Locale("en", "KE") // "en" for English, "KE" for Kenya
    return NumberFormat.getCurrencyInstance(kenyaLocale).format(value)
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
            },
            topBar = {
                TopAppBar(
                    title = { Text("Manage Purchases") },
                    actions = {
                        IconButton(onClick = viewModel::loadPurchases, enabled = !state.isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // Left Panel: Purchase Details
                Card(modifier = Modifier.weight(1.5f).padding(end = 8.dp)) {
                    PurchaseDetailsView(state.selectedPurchase)
                }

                // Right Panel: List of Purchases
                Card(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    // Pass the whole viewModel to the list composable
                    PurchaseList(viewModel = viewModel)
                }
            }
        }
    }
}


@Composable
private fun PurchaseList(viewModel: PurchaseViewModel) {
    val state by remember { derivedStateOf { viewModel.uiState } }
    // Use the filtered list from the viewModel
    val filteredPurchases by remember { derivedStateOf { viewModel.filteredPurchases } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Purchases", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Search Bar Added Here ---
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            label = { Text("Search by Ref or Supplier...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            singleLine = true
        )

        when {
            state.isLoading && state.purchases.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("${state.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            // Check the filtered list for emptiness
            filteredPurchases.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No purchases found.")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Iterate over the filtered list
                    items(filteredPurchases, key = { it.id }) { purchase ->
                        ListItem(
                            headlineContent = { Text(purchase.ref, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Supplier: ${purchase.supplier ?: "N/A"}") },
                            overlineContent = { Text(formatDate(purchase.purchaseDate)) },
                            trailingContent = { Text(formatCurrency(purchase.totalCost)) },
                            modifier = Modifier.clickable { viewModel.onPurchaseSelected(purchase) },
                            colors = if (purchase.id == state.selectedPurchase?.id) {
                                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ListItemDefaults.colors()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseDetailsView(selectedPurchase: PurchaseResponse?) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedPurchase == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a purchase to view its details.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Purchase Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Reference: ${selectedPurchase.ref}", style = MaterialTheme.typography.titleMedium)
            Text("Date: ${formatDate(selectedPurchase.purchaseDate)}", style = MaterialTheme.typography.bodySmall)
            Text("Supplier: ${selectedPurchase.supplier ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Total Cost: ${formatCurrency(selectedPurchase.totalCost)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text("Items (${selectedPurchase.items.size})", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn {
                items(selectedPurchase.items) { item ->
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