package com.chui.pos.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.viewmodels.ReorderViewModel
import org.koin.compose.koinInject

object ReorderScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<ReorderViewModel>()
        val state by viewModel::uiState
        val filteredItems by viewModel::filteredItems

        LaunchedEffect(Unit) {
            viewModel.loadReorderItems()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Products for Re-order") },
                    actions = {
                        IconButton(onClick = viewModel::loadReorderItems, enabled = !state.isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    label = { Text("Search by Product Name or Code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    singleLine = true
                )

                when {
                    state.isLoading && state.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    filteredItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items need re-ordering or match your search.")
                        }
                    }
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredItems, key = { it.productId }) { item ->
                                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                                    ListItem(
                                        headlineContent = { Text(item.productName, fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Code: ${item.productCode}") },
                                        trailingContent = {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("In Stock: ${item.currentQuantity} ${item.saleUnitName}")
                                                Text(
                                                    "Alert at: ${item.stockAlertLevel}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
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