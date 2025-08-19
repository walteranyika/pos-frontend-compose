package com.chui.pos.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.components.HeldOrdersDialog
import com.chui.pos.dtos.CartItem
import com.chui.pos.dtos.CategoryResponse
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
        val saleSubmissionState = viewModel.saleSubmissionState //FIX BY
        val actionMessage = viewModel.actionMessage
        val snackbarHostState = remember { SnackbarHostState() }


        LaunchedEffect(saleSubmissionState) {
            when (saleSubmissionState) {
                is SaleSubmissionState.Success -> {
                    println("Sale submitted successfully!")
                    snackbarHostState.showSnackbar("Sale submitted successfully")
                    viewModel.resetSaleSubmissionState()
                }

                is SaleSubmissionState.Error -> {
                    println("Error submitting sale: ${saleSubmissionState.message}")
                    snackbarHostState.showSnackbar("Error submitting sale: ${saleSubmissionState.message}")
                    viewModel.resetSaleSubmissionState()
                }

                else -> { /* Do nothing for Idle or Loading states */
                }
            }
        }

        LaunchedEffect(actionMessage) {
            actionMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onActionMessageShown()
            }
        }


        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
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
        }



        if (viewModel.showPaymentDialog) {
            PaymentDialog(viewModel)
        }

        if (viewModel.showHeldOrdersDialog) {
            HeldOrdersDialog(
                heldOrders = viewModel.helOrders,
                onDismiss = viewModel::hideHeldOrdersDialog,
                onResume = viewModel::resumeHeldOrder,
                onDelete = viewModel::deleteHeldOrder
            )
        }

        if (viewModel.showAddCustomerDialog){
            AddCustomerDialog(
                onDismiss = viewModel::hideAddCustomerDialog,
                onConfirm = viewModel::createCustomer
            )
        }

        val variablePriceProduct = viewModel.variablePriceProduct
        if (variablePriceProduct != null) {
            VariablePriceDialog(
                product = variablePriceProduct,
                onDismiss = viewModel::hideVariablePriceDialog,
                onConfirm = viewModel::addVariablePriceItemToCart
            )
        }


    }
}

@Composable
private fun CartView(viewModel: PosViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
   // val activeHeldOrderId by remember { mutableStateOf(viewModel.activeHeldOrderId) }
    val activeHeldOrderId = viewModel.activeHeldOrderId

    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CustomerSelectionView(viewModel)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            // Cart section
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
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.holdCurrentOrder() },
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(if (activeHeldOrderId != null) "Update Held Order" else "Hold Order")
                }

                OutlinedButton(
                    onClick = viewModel::showHeldOrdersDialog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("View held orders")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelectionView(viewModel: PosViewModel) {
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()
    val filteredCustomers by viewModel.filteredCustomers.collectAsState()
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Customer", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded && filteredCustomers.isNotEmpty(),
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = customerSearchQuery,
                    onValueChange = {
                        viewModel.onCustomerSearchQueryChanged(it)
                        isDropdownExpanded = true
                    },
                    label = { Text("Search or Select Customer") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = false,
                    trailingIcon = {
                        if (customerSearchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onCustomerSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded && filteredCustomers.isNotEmpty(),
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    filteredCustomers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = {
                                viewModel.onCustomerSelected(customer)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = viewModel::showAddCustomerDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add New Customer")
            }
        }
    }
}

// --- NEW: Composable for the Add Customer dialog ---
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val isFormValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer") },
        shape = RoundedCornerShape(3.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name*") },
                    isError = name.isBlank()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone) },
                enabled = isFormValid
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            // Disable this button for variable priced items
            IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp), enabled = !item.isVariablePriced) {
                Icon(
                    Icons.Default.Remove,
                    "Decrement"
                )
            }
            // Format quantity differently based on item type
            Text(
                text = if (item.isVariablePriced) "%.3f".format(item.quantity) else "%.0f".format(item.quantity),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            // Disable this button for variable priced items
            IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp), enabled = !item.isVariablePriced) {
                Icon(Icons.Default.Add, "Increment")
            }
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

    // State for the input fields
    var amountInput by remember { mutableStateOf("") }
    // 1. Default payment method is now MPESA
    var selectedMethod by remember { mutableStateOf(PaymentMethod.MPESA) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Effect to automatically fill the input with the remaining balance.
    // This runs when the dialog opens and whenever a payment is added or removed.
    LaunchedEffect(remainingBalance) {
        if (remainingBalance > 0.009) { // Use a small epsilon for float comparison
            amountInput = "%.2f".format(remainingBalance)
        } else {
            amountInput = "" // Clear the input if the sale is fully paid
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.onDismissPaymentDialog() },
        title = { Text("Complete Payment") },
        shape = RoundedCornerShape(3.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total: %.2f".format(total), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Remaining: %.2f".format(remainingBalance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                // Payment input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                viewModel.addPayment(PaymentRequest(amount, selectedMethod))
                                // 3. The LaunchedEffect now handles updating the input field,
                                // so we don't need to clear it manually.
                            }
                        },
                        // Disable button if input is invalid or nothing is owed
                        enabled = remainingBalance > 0 && (amountInput.toDoubleOrNull() ?: 0.0) > 0
                    ) { Text("Add") }
                }

                // List of added payments
                LazyColumn {
                    items(payments) { payment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${payment.method.name}: %.2f".format(payment.amount))
                            IconButton(onClick = { viewModel.removePayment(payment) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Payment",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }


                // Print Receipt Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onPrintReceiptChanged(!viewModel.printReceipt) }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = viewModel.printReceipt,
                        onCheckedChange = { viewModel.onPrintReceiptChanged(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Print Receipt?")
                }

                Spacer(Modifier.height(16.dp))
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
                onDismissRequest = { viewModel.onSearchQueryChange("") }            ) {
                searchResults.forEach { product ->
                    val varyingPrice = if (product.isVariablePriced){
                        "(V)"
                    }else{
                        ""
                    }
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("${product.name} (${product.code})")
                                Text("$varyingPrice ${product.price} / ${product.saleUnit.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        },
                        onClick = { viewModel.onSearchResultSelected(product) }
                    )
                }
            }
        }



        Spacer(Modifier.height(8.dp))
        val productsUiState = viewModel.productsState
        if (productsUiState is ProductsUiState.Success) {
            CategoryFilterRow(
                categories = productsUiState.categories,
                selectedCategoryId = viewModel.selectedCategoryId,
                onCategorySelected = viewModel::onCategorySelected
            )
            Spacer(Modifier.height(16.dp))
        }

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

// ...

@Composable
private fun CategoryFilterRow(
    categories: List<CategoryResponse>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    // A list of vibrant, Material-style colors for the buttons
    val vibrantColors = remember {
        listOf(
            Color(0xFFEF5350), // Red 400
            Color(0xFF66BB6A), // Green 400
            Color(0xFF42A5F5), // Blue 400
            Color(0xFFFFCA28), // Amber 400
            Color(0xFFAB47BC), // Purple 400
            Color(0xFFFF7043), // Deep Orange 400
            Color(0xFF26A69A), // Teal 400
            Color(0xFF78909C)  // Blue Grey 400
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing
    ) {
        // "All" button - styled with theme colors to stand out
        item {
            val isSelected = selectedCategoryId == null
            Button(
                onClick = { onCategorySelected(null) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("All", fontWeight = FontWeight.Bold)
            }
        }

        // Category buttons
        items(categories.indices.toList()) { index ->
            val category = categories[index]
            val isSelected = selectedCategoryId == category.id
            // Cycle through the vibrant colors
            val color = vibrantColors[index % vibrantColors.size]

            Button(
                onClick = { onCategorySelected(category.id) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp) // Larger height
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = Color.White
                )
            ) {
                Text(category.name, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductGridItem(product: ProductResponse, onClick: () -> Unit) {
    val varyingPrice = if (product.isVariablePriced){
        "(V)"
    }else{
        ""
    }
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).height(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(product.code, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "%.2f".format(product.price)+" $varyingPrice",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Add this new composable function within your PosScreen.kt file

@Composable
private fun VariablePriceDialog(
    product: ProductResponse,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    // Validate that the input is a positive number
    val isAmountValid = amountInput.toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Amount for ${product.name}") },
        shape = RoundedCornerShape(8.dp),
        text = {
            Column {
                Text(
                    "Unit Price: %.2f per ${product.saleUnit.name}".format(product.price),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountInput,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { amountInput = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    isError = amountInput.isNotBlank() && !isAmountValid
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountInput.toDouble()) },
                enabled = isAmountValid
            ) {
                Text("Add to Cart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}