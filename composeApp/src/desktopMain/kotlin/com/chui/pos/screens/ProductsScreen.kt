package com.chui.pos.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.*
import com.chui.pos.viewmodels.ProductViewModel
import com.chui.pos.viewmodels.ProductsOnlyUiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
object ProductsScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinInject<ProductViewModel>()
        val uiState = viewModel.uiState

        Scaffold(
            topBar = { TopAppBar(title = { Text("Manage Products") },
                /*actions = {
                    IconButton(onClick = viewModel::) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }*/
                ) }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (viewModel.showDeleteConfirmDialog) {
                    DeleteConfirmationDialog(
                        onConfirm = viewModel::confirmDelete,
                        onDismiss = viewModel::onDismissDeleteDialog
                    )
                }

                // Left Panel: Form
                Card(modifier = Modifier.weight(1.5f).padding(end = 8.dp)) {
                    ProductForm(viewModel, uiState)
                }

                // Right Panel: List
                Card(modifier = Modifier.weight(2f).padding(start = 8.dp)) {
                    ProductList(
                        uiState = uiState,
                        viewModel=viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductForm(viewModel: ProductViewModel, uiState: ProductsOnlyUiState) {
    val formState = viewModel.formState
    val onFormChange = viewModel::onFormChange

    val availableCategories = (uiState as? ProductsOnlyUiState.Success)?.categories ?: emptyList()
    val availableUnits = (uiState as? ProductsOnlyUiState.Success)?.units ?: emptyList()

    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = if (viewModel.isEditing) "Edit Product" else "Add New Product",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
        }

        item { OutlinedTextField(value = formState.name, onValueChange = { onFormChange(formState.copy(name = it)) }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = formState.code, onValueChange = { onFormChange(formState.copy(code = it)) }, label = { Text("Product Code") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = formState.barcode ?: "", onValueChange = { onFormChange(formState.copy(barcode = it)) }, label = { Text("Barcode (Optional)") }, modifier = Modifier.fillMaxWidth()) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = formState.cost, onValueChange = { onFormChange(formState.copy(cost = it)) }, label = { Text("Cost") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = formState.price, onValueChange = { onFormChange(formState.copy(price = it)) }, label = { Text("Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }

        item { DropdownSelector(label = "Category", selectedValue = formState.category?.name ?: "Select Category", items = availableCategories, onItemSelected = { onFormChange(formState.copy(category = it)) }, itemToString = { it.name }) }
        item { DropdownSelector(label = "Sale Unit", selectedValue = formState.saleUnit?.name ?: "Select Sale Unit", items = availableUnits, onItemSelected = { onFormChange(formState.copy(saleUnit = it)) }, itemToString = { it.name }) }
        item { DropdownSelector(label = "Purchase Unit", selectedValue = formState.purchaseUnit?.name ?: "Select Purchase Unit", items = availableUnits, onItemSelected = { onFormChange(formState.copy(purchaseUnit = it)) }, itemToString = { it.name }) }
        item { DropdownSelector(label = "Tax Method", selectedValue = formState.taxMethod.name, items = TaxType.values().toList(), onItemSelected = { onFormChange(formState.copy(taxMethod = it)) }, itemToString = { it.name }) }
        item { OutlinedTextField(value = formState.stockAlert, onValueChange = { onFormChange(formState.copy(stockAlert = it)) }, label = { Text("Stock Alert Quantity") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        item { OutlinedTextField(value = formState.note ?: "", onValueChange = { onFormChange(formState.copy(note = it)) }, label = { Text("Note (Optional)") }, modifier = Modifier.fillMaxWidth()) }


        item { FormSwitch(label = "Is Active", checked = formState.isActive, onCheckedChange = { onFormChange(formState.copy(isActive = it)) }) }
        item { FormSwitch(label = "Variable Price (at POS)", checked = formState.isVariablePriced, onCheckedChange = { onFormChange(formState.copy(isVariablePriced = it)) }) }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = viewModel::saveProduct) { Text(if (viewModel.isEditing) "Update" else "Save") }
                if (viewModel.isEditing) {
                    Button(onClick = viewModel::onDeleteClicked, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Delete") }
                }
                Spacer(Modifier.weight(1f))
                if (viewModel.isEditing) {
                    OutlinedButton(onClick = viewModel::clearSelection) { Text("New") }
                }
            }
        }
    }
}

@Composable
private fun FormSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    selectedValue: String,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToString(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}



@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProductList(viewModel: ProductViewModel, uiState: ProductsOnlyUiState) {
    val onProductSelected = viewModel::onProductSelected

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val searchResults = viewModel.searchResults
        val isDropdownVisible = searchResults.isNotEmpty() && viewModel.searchQuery.isNotBlank()

        ExposedDropdownMenuBox(
            expanded = isDropdownVisible,
            onExpandedChange = { /* Controlled by search results */ }
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search Products...") },
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
                        text = { Text("${product.name} (Code: ${product.code})") },
                        onClick = { viewModel.onSearchResultSelected(product) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (uiState) {
            is ProductsOnlyUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ProductsOnlyUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.message, color = MaterialTheme.colorScheme.error) }
            is ProductsOnlyUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(uiState.products, key = { _, product -> product.id }) { index, product ->
                        ListItem(
                            leadingContent = { Text("${index + 1}.") },
                            headlineContent = { Text(product.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text("Code: ${product.code} | Price: ${product.price}") },
                            overlineContent = { Text(product.category.name) },
                            modifier = Modifier.clickable { onProductSelected(product) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}



@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}