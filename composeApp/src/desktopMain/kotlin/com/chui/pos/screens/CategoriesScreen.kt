package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.CategoryResponse
import com.chui.pos.viewmodels.CategoriesUiState
import com.chui.pos.viewmodels.CategoryViewModel
import org.koin.compose.koinInject

object CategoriesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<CategoryViewModel>()
        val uiState = viewModel.uiState

        Scaffold(
            topBar = { TopAppBar(title = { Text("Manage Product Categories") }) }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (viewModel.showDeleteConfirmDialog) {
                    DeleteConfirmationDialog(
                        onConfirm = viewModel::confirmDelete,
                        onDismiss = viewModel::onDismissDeleteDialog
                    )
                }

                // Left Panel: Form
                Card(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    CategoryForm(viewModel)
                }

                // Right Panel: List
                Card(modifier = Modifier.weight(2f).padding(start = 8.dp)) {
                    CategoryList(
                        uiState = uiState,
                        onCategorySelected = viewModel::onCategorySelected
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryForm(viewModel: CategoryViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (viewModel.isEditing) "Edit Category" else "Add New Category",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.formName,
            onValueChange = viewModel::onNameChange,
            label = { Text("Category Name (e.g., Beverages)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.formCode,
            onValueChange = viewModel::onCodeChange,
            label = { Text("Category Code (e.g., BEV)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = viewModel::saveCategory,
                enabled = viewModel.formName.isNotBlank() && viewModel.formCode.isNotBlank()
            ) {
                Text(if (viewModel.isEditing) "Update" else "Save")
            }
            if (viewModel.isEditing) {
                Button(
                    onClick = viewModel::onDeleteClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete")
                }
            }
            Spacer(Modifier.weight(1f))
            if (viewModel.isEditing) {
                OutlinedButton(onClick = viewModel::clearSelection) {
                    Text("New")
                }
            }
        }
    }
}

@Composable
private fun CategoryList(uiState: CategoriesUiState, onCategorySelected: (CategoryResponse) -> Unit) {
    when (uiState) {
        is CategoriesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is CategoriesUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.message) }
        is CategoriesUiState.Success -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(uiState.categories, key = { _, category -> category.id }) { index, category ->
                    ListItem(
                        leadingContent = { Text("${index + 1}.") },
                        headlineContent = { Text(category.name) },
                        supportingContent = { Text("Code: ${category.code}") },
                        modifier = Modifier.clickable { onCategorySelected(category) }
                    )
                    HorizontalDivider()
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
        text = { Text("Are you sure you want to delete this category? This action cannot be undone.") },
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