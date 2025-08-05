package com.chui.pos.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.viewmodels.StockViewModel
import org.koin.compose.koinInject

object StockScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<StockViewModel>()
        val state by remember { derivedStateOf { viewModel.uiState } }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.adjustmentMessage) {
            state.adjustmentMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onAdjustmentMessageShown()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Stock Management") },
                    actions = {
                        IconButton(onClick = viewModel::loadProducts, enabled = !state.isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    state.selectedProduct?.let { product ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Adjust Stock", style = MaterialTheme.typography.headlineSmall)
                            Text(product.name, style = MaterialTheme.typography.titleLarge)
                            Text("Current Quantity: ${product.quantity} ${product.saleUnit.name}")

                            OutlinedTextField(
                                value = viewModel.newQuantity,
                                onValueChange = viewModel::onNewQuantityChanged,
                                label = { Text("New Quantity") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = viewModel::adjustStock,
                                enabled = !state.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("Update Stock")
                            }
                        }
                    } ?: run {
                        Text(
                            "Select a product from the list to adjust its stock.",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                VerticalDivider()
                // Product List Pane
                Column(modifier = Modifier.weight(2f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // --- Search Bar Added Here ---
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        label = { Text("Search Products...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        singleLine = true
                    )

                    if (state.isLoading && state.products.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (state.error != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Error: ${state.error}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // --- Use the new filteredProducts list ---
                            items(viewModel.filteredProducts, key = { it.id }) { product ->
                                ListItem(
                                    headlineContent = { Text(product.name) },
                                    supportingContent = { Text("Code: ${product.code}") },
                                    trailingContent = {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${product.quantity}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                product.saleUnit.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable { viewModel.onProductSelected(product) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}