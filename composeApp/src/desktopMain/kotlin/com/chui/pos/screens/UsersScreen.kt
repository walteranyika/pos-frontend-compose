package com.chui.pos.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import com.chui.pos.dtos.RoleResponse
import com.chui.pos.dtos.UserResponse
import com.chui.pos.viewmodels.UserViewModel
import org.koin.compose.koinInject

object UsersScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: UserViewModel = koinInject()
        val state = viewModel.uiState
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.actionMessage) {
            state.actionMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onActionMessageShown()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.error != null) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                } else {
                    UserList(
                        users = state.users,
                        onEditClick = viewModel::showEditDialog,
                        onResetPinClick = viewModel::showResetPinDialog
                    )
                }

                if (state.isEditDialogVisible && state.selectedUser != null) {
                    EditUserDialog(
                        user = state.selectedUser,
                        allRoles = state.roles,
                        onDismiss = viewModel::hideDialogs,
                        onConfirm = viewModel::updateUser
                    )
                }

                if (state.isResetPinDialogVisible && state.selectedUser != null) {
                    ResetPinDialog(
                        user = state.selectedUser,
                        onDismiss = viewModel::hideDialogs,
                        onConfirm = viewModel::resetPin
                    )
                }
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<UserResponse>,
    onEditClick: (UserResponse) -> Unit,
    onResetPinClick: (UserResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.id }) { user ->
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                ListItem(
                    headlineContent = { Text(user.fullName ?: "No Name", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("@${user.username} | Roles: ${user.roles.joinToString(", ")}") },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { onEditClick(user) }) {
                                Text("Edit")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { onResetPinClick(user) }) {
                                Text("Reset PIN")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: UserResponse,
    allRoles: List<RoleResponse>,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, username: String, assignedRoleIds: Set<Long>) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName ?: "") }
    var username by remember { mutableStateOf(user.username) }
    val assignedRoleNames = remember { user.roles.toMutableStateList() }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(500.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit User: @${user.username}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Roles", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Role selection checkboxes
                allRoles.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = role.name in assignedRoleNames,
                            onCheckedChange = { isChecked ->
                                if (isChecked) assignedRoleNames.add(role.name) else assignedRoleNames.remove(role.name)
                            }
                        )
                        Text(role.name)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val assignedIds = allRoles.filter { it.name in assignedRoleNames }.map { it.id }.toSet()
                        onConfirm(fullName, username, assignedIds)
                    }) {
                        Text("Save Changes")
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
                label = { Text("New 6-Digit PIN") },
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
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}