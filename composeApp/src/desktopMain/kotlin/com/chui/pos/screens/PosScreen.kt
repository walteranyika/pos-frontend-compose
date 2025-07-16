package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.CartItem
import com.chui.pos.dtos.PaymentMethod
import com.chui.pos.dtos.PaymentRequest
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.viewmodels.PosViewModel
import com.chui.pos.viewmodels.ProductsUiState
import com.chui.pos.viewmodels.SaleSubmissionState
import org.koin.compose.koinInject

object PosScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinInject<PosViewModel>()
        val saleSubmissionState  = viewModel.saleSubmissionState //FIX BY

        LaunchedEffect(saleSubmissionState) {
            when (saleSubmissionState) {
                is SaleSubmissionState.Success -> {
                    println("Sale submitted successfully!")
                    viewModel.resetSaleSubmissionState()
                }
                is SaleSubmissionState.Error -> {
                    println("Error submitting sale: ${saleSubmissionState.message}")
                    viewModel.resetSaleSubmissionState()
                }
                else -> { /* Do nothing for Idle or Loading states */ }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Side: Cart (1 part of the screen width)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                CartView(viewModel)
            }

            // Right Side: Product List (2 parts of the screen width)
            Box(modifier = Modifier.weight(2f).fillMaxHeight().padding(8.dp)) {
                ProductListView(viewModel)
            }
        }

        if(viewModel.showPaymentDialog){
            PaymentDialog(viewModel)
        }

    }
}

@Composable
private fun CartView(viewModel: PosViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val total by viewModel.cartTotal.collectAsState()

    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Cart", style = MaterialTheme.typography.headlineMedium)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Cart is empty", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cartItems.values.toList(), key = { it.productId }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrement = { viewModel.incrementQuantity(item.productId) },
                            onDecrement = { viewModel.decrementQuantity(item.productId) },
                            onRemove = { viewModel.removeItem(item.productId) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Total section
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "%.2f".format(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.onOpenPaymentDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(size = 0.dp)
                ) {
                    Text("Complete and Pay")
                }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onIncrement: () -> Unit, onDecrement: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("Price: %.2f".format(item.price), style = MaterialTheme.typography.bodySmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, "Decrement") }
            Text(item.quantity.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, "Increment") }
        }

        Text(
            text = "%.2f".format(item.total),
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, "Remove Item", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PaymentDialog(viewModel: PosViewModel) {
    val total by viewModel.cartTotal.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val paidAmount = payments.sumOf { it.amount }
    val remainingBalance = total - paidAmount

    var amountInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.onDismissPaymentDialog() },
        title = { Text("Complete Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total: %.2f".format(total), style = MaterialTheme.typography.titleLarge)
                Text("Remaining: %.2f".format(remainingBalance), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                // Payment input
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        enabled = remainingBalance > 0
                    )
                    Box {
                        OutlinedButton(onClick = { isDropdownExpanded = true }) {
                            Text(selectedMethod.name)
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        selectedMethod = method
                                        isDropdownExpanded = false
                                        // UX Improvement: Auto-fill amount with remaining balance
                                        if (remainingBalance > 0) {
                                            amountInput = "%.2f".format(remainingBalance)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Button(onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.addPayment(PaymentRequest(amount, selectedMethod))
                            amountInput = ""
                        }
                    },
                        enabled = remainingBalance > 0
                    ) { Text("Add") }
                }

                // List of added payments
                LazyColumn {
                    items(payments) { payment ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${payment.method.name}: %.2f".format(payment.amount))
                            IconButton(onClick = { viewModel.removePayment(payment) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Payment", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitSale() },
                // Enable only when the paid amount exactly matches the total
                enabled = kotlin.math.abs(remainingBalance) < 0.01 && payments.isNotEmpty()
            ) { Text("Submit Sale") }
        },
        dismissButton = {
            Button(onClick = { viewModel.onDismissPaymentDialog() }) { Text("Cancel") }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductListView(viewModel: PosViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val searchResults = viewModel.searchResults
        val isDropdownVisible = searchResults.isNotEmpty() && viewModel.searchQuery.isNotBlank()

        ExposedDropdownMenuBox(
            expanded = isDropdownVisible,
            onExpandedChange = { /* Controlled by search results */ }
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search Products by name or code...") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    if (viewModel.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                }
            )

            ExposedDropdownMenu(
                expanded = isDropdownVisible,
                onDismissRequest = { /* We don't want this */ }
            ) {
                searchResults.forEach { product ->
                    DropdownMenuItem(
                        text = { Text("${product.name} (${product.code})") },
                        onClick = { viewModel.onSearchResultSelected(product) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        when (val state = viewModel.productsState) {
            is ProductsUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) { CircularProgressIndicator() }
            }

            is ProductsUiState.Error -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }

            is ProductsUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductGridItem(
                            product = product,
                            onClick = { viewModel.onProductClicked(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGridItem(product: ProductResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).height(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 2)
                Text(product.code, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "%.2f".format(product.price),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}