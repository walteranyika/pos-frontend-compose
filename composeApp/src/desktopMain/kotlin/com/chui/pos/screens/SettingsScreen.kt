package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.viewmodels.SettingsViewModel
import org.koin.compose.koinInject

object SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinInject<SettingsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Show a snackbar message when available
        LaunchedEffect(uiState.toastMessage) {
            uiState.toastMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onToastMessageShown() // Acknowledge message shown
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(modifier = Modifier.widthIn(max = 600.dp).padding(32.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Application Settings", style = MaterialTheme.typography.headlineMedium)

                        // Base URL setting
                        OutlinedTextField(
                            value = viewModel.baseUrl,
                            onValueChange = viewModel::onBaseUrlChanged,
                            label = { Text("API Base URL") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Printer setting
                        var isDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.selectedPrinter.ifEmpty { "Select a printer" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Thermal Printer") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                uiState.availablePrinters.forEach { printerName ->
                                    DropdownMenuItem(
                                        text = { Text(printerName) },
                                        onClick = {
                                            viewModel.onPrinterSelected(printerName)
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSoundToggled(!viewModel.soundEnabled) } // Make the whole row clickable
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text("Sound Effects", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Play sound when adding/removing items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = viewModel.soundEnabled,
                                onCheckedChange = viewModel::onSoundToggled
                            )
                        }



                        Button(
                            onClick = { viewModel.saveSettings() },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text("Save Settings")
                        }
                    }
                }
            }
        }
    }
}