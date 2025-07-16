package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.ProductUnitResponse
import com.chui.pos.viewmodels.UnitViewModel
import com.chui.pos.viewmodels.UnitsUiState
import org.koin.compose.koinInject

object UnitsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<UnitViewModel>()
        val uiState = viewModel.uiState

        Scaffold(
            topBar = { TopAppBar(title = { Text("Manage Product Units") }) }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // Left Panel: Form
                if (viewModel.showDeleteConfirmDialog) {
                    DeleteConfirmationDialog(
                        onConfirm = viewModel::confirmDelete,
                        onDismiss = viewModel::onDismissDeleteDialog
                    )
                }

                Card(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    UnitForm(viewModel)
                }

                // Right Panel: List
                Card(modifier = Modifier.weight(2f).padding(start = 8.dp)) {
                    UnitList(
                        uiState = uiState,
                        onUnitSelected = viewModel::onUnitSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitForm(viewModel: UnitViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (viewModel.isEditing) "Edit Unit" else "Add New Unit",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.formName,
            onValueChange = viewModel::onNameChange,
            label = { Text("Unit Name (e.g., Kilogram)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.formShortName,
            onValueChange = viewModel::onShortNameChange,
            label = { Text("Short Name (e.g., KG)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = viewModel::saveUnit, enabled = viewModel.formName.isNotBlank() && viewModel.formShortName.isNotBlank()) {
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
private fun UnitList(uiState: UnitsUiState, onUnitSelected: (ProductUnitResponse) -> Unit) {
    when (uiState) {
        is UnitsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UnitsUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.message) }
        is UnitsUiState.Success -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.units, key = { it.id }) { unit ->
                    ListItem(
                        headlineContent = { Text(unit.name) },
                        supportingContent = { Text("Short Name: ${unit.shortName}") },
                        modifier = Modifier.clickable { onUnitSelected(unit) }
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
        text = { Text("Are you sure you want to delete this unit? This action cannot be undone.") },
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