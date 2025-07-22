package com.chui.pos.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.UserResponse
import com.chui.pos.viewmodels.UserViewModel
import org.koin.compose.koinInject

object UsersScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: UserViewModel = koinInject()
        val state = viewModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }

        // Show feedback messages from the ViewModel
        LaunchedEffect(state.actionMessage) {
            state.actionMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onActionMessageShown()
            }
        }

        // Handle the Reset PIN Dialog
        if (state.isResetPinDialogVisible && state.selectedUser != null) {
            ResetPinDialog(
                user = state.selectedUser,
                onDismiss = viewModel::hideDialogs,
                onConfirm = viewModel::resetPin
            )
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Manage Users") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // Left Panel: Form for Add/Edit
                Card(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    UserForm(viewModel)
                }

                // Right Panel: List of Users
                Card(modifier = Modifier.weight(2f).padding(start = 8.dp)) {
                    UserList(viewModel)
                }
            }
        }
    }
}

@Composable
private fun UserForm(viewModel: UserViewModel) {
    val formState = viewModel.formState
    val allRoles = viewModel.uiState.roles
    var pinVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (viewModel.isEditing) "Edit User" else "Add New User",
                style = MaterialTheme.typography.headlineSmall
            )
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        item {
            OutlinedTextField(
                value = formState.fullName,
                onValueChange = { viewModel.onFormChange(formState.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = formState.username,
                onValueChange = { viewModel.onFormChange(formState.copy(username = it)) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // PIN fields are only shown when creating a new user
        if (!viewModel.isEditing) {
            item {
                OutlinedTextField(
                    value = formState.pin,
                    onValueChange = { viewModel.onFormChange(formState.copy(pin = it)) },
                    label = { Text("4-Digit PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        val image = if (pinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { pinVisible = !pinVisible }) { Icon(image, "Toggle PIN visibility") }
                    }
                )
            }
        }

        item {
            Text("Roles", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }

        // Role selection checkboxes
        items(allRoles) { role ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleRoleSelection(role.id) }
            ) {
                Checkbox(
                    checked = role.id in formState.assignedRoleIds,
                    onCheckedChange = { viewModel.toggleRoleSelection(role.id) }
                )
                Text(role.name, modifier = Modifier.padding(start = 8.dp))
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::saveUser) {
                    Text(if (viewModel.isEditing) "Update User" else "Save User")
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
private fun UserList(viewModel: UserViewModel) {
    val state = viewModel.uiState

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // You can add a search bar here if needed, similar to ProductsScreen
        // ...

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.error, color = MaterialTheme.colorScheme.error) }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.users, key = { it.id }) { user ->
                        ListItem(
                            headlineContent = { Text(user.fullName ?: "No Name", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("@${user.username}") },
                            overlineContent = { Text("Roles: ${user.roles.joinToString(", ")}") },
                            modifier = Modifier.clickable { viewModel.onUserSelected(user) },
                            trailingContent = {
                                TextButton(onClick = { viewModel.showResetPinDialog(user) }) {
                                    Text("Reset PIN")
                                }
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
private fun ResetPinDialog(
    user: UserResponse,
    onDismiss: () -> Unit,
    onConfirm: (newPin: String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset PIN for @${user.username}") },
        text = {
            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it },
                label = { Text("New 4-Digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPin) },
                enabled = newPin.length == 6 && newPin.all { it.isDigit() }
            ) {
                Text("Confirm Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}