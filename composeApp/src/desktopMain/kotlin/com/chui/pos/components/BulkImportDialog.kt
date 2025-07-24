package com.chui.pos.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chui.pos.dtos.BulkImportResponse
import com.chui.pos.dtos.ImportValidationResult
import com.chui.pos.viewmodels.ProductViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun BulkImportDialog(viewModel: ProductViewModel) {
    val state = viewModel.bulkImportState
    val fileChooser = remember {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("CSV Files", "csv")
            isAcceptAllFileFilterUsed = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!state.isImporting) viewModel.hideImportDialog() },
        title = { Text("Bulk Import Products") },
        text = {
            when {
                state.importResponse != null -> ImportResultView(state.importResponse)
                state.isImporting -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Importing...")
                        }
                    }
                }
                else -> FileSelectionView(
                    selectedFile = state.selectedFile,
                    validationResult = state.validationResult,
                    onFileSelect = {
                        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            viewModel.onFileSelectedForImport(fileChooser.selectedFile)
                        }
                    }
                )
            }
        },
        confirmButton = {
            if (state.importResponse != null) {
                Button(onClick = viewModel::hideImportDialog) { Text("Done") }
            } else {
                Button(
                    onClick = viewModel::startBulkImport,
                    enabled = state.validationResult is ImportValidationResult.Valid && !state.isImporting
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            if (state.importResponse == null && !state.isImporting) {
                TextButton(onClick = viewModel::hideImportDialog) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun FileSelectionView(
    selectedFile: File?,
    validationResult: ImportValidationResult,
    onFileSelect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select a CSV file with the required columns. The first row must be the header.")
        Button(onClick = onFileSelect) {
            Text("Select File...")
        }
        if (selectedFile != null) {
            Text("Selected: ${selectedFile.name}", style = MaterialTheme.typography.bodyMedium)
        }
        when (validationResult) {
            is ImportValidationResult.Invalid -> Text(
                validationResult.reason,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            is ImportValidationResult.Valid -> Text(
                "✓ File is valid and ready to import.",
                color = Color(0xFF388E3C),
                style = MaterialTheme.typography.bodyMedium
            )
            else -> {}
        }
    }
}

@Composable
private fun ImportResultView(response: BulkImportResponse) {
    Column {
        Text("Import Complete", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Total Records in File: ${response.totalRecords}")
        Text("Successful Imports: ${response.successfulImports}", color = Color(0xFF388E3C))
        Text(
            "Failed Imports: ${response.failedImports}",
            color = if (response.failedImports > 0) MaterialTheme.colorScheme.error else Color.Unspecified
        )

        if (response.errors.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Error Details:", fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(response.errors) { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}