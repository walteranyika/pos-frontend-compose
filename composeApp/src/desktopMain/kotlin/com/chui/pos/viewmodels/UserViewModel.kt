package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.*
import com.chui.pos.services.UserService
import kotlinx.coroutines.launch

// State for the main UI (lists, loading, dialogs)
data class UserManagementUiState(
    val users: List<UserResponse> = emptyList(),
    val roles: List<RoleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedUser: UserResponse? = null,
    val isResetPinDialogVisible: Boolean = false,
    val actionMessage: String? = null
)

// State specifically for the Add/Edit form
data class UserFormState(
    val fullName: String = "",
    val username: String = "",
    val pin: String = "",
    val assignedRoleIds: Set<Long> = emptySet()
)

class UserViewModel(private val userService: UserService) : ScreenModel {

    var uiState by mutableStateOf(UserManagementUiState())
        private set

    var formState by mutableStateOf(UserFormState())
        private set

    val isEditing: Boolean
        get() = uiState.selectedUser != null

    init {
        loadUsersAndRoles()
    }

    private fun loadUsersAndRoles() {
        uiState = uiState.copy(isLoading = true)
        screenModelScope.launch {
            val usersResult = userService.getUsers()
            val rolesResult = userService.getRoles()

            val users = usersResult.getOrNull() ?: emptyList()
            val roles = rolesResult.getOrNull() ?: emptyList()
            val error = usersResult.exceptionOrNull()?.message ?: rolesResult.exceptionOrNull()?.message

            uiState = uiState.copy(
                users = users,
                roles = roles,
                isLoading = false,
                error = error
            )
        }
    }

    // --- Form State Management ---

    fun onUserSelected(user: UserResponse) {
        uiState = uiState.copy(selectedUser = user)
        // Populate the form with the selected user's data
        formState = UserFormState(
            fullName = user.fullName ?: "",
            username = user.username,
            assignedRoleIds = uiState.roles.filter { it.name in user.roles }.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        uiState = uiState.copy(selectedUser = null)
        formState = UserFormState() // Reset form to default
    }

    fun onFormChange(newFormState: UserFormState) {
        formState = newFormState
    }

    fun toggleRoleSelection(roleId: Long) {
        val currentIds = formState.assignedRoleIds.toMutableSet()
        if (roleId in currentIds) {
            currentIds.remove(roleId)
        } else {
            currentIds.add(roleId)
        }
        formState = formState.copy(assignedRoleIds = currentIds)
    }

    // --- Data Actions ---

    fun saveUser() {
        if (isEditing) {
            updateUser()
        } else {
            createUser()
        }
    }

    private fun createUser() {
        screenModelScope.launch {
            val request = CreateUserRequest(
                username = formState.username,
                fullName = formState.fullName,
                password = formState.pin,
                roleIds = formState.assignedRoleIds
            )
            userService.createUser(request)
                .onSuccess {
                    uiState = uiState.copy(actionMessage = "User '${it.username}' created successfully.")
                    clearSelection() // Clear form for next entry
                    loadUsersAndRoles() // Refresh list
                }
                .onFailure {
                    uiState = uiState.copy(actionMessage = "Error creating user: ${it.message}")
                }
        }
    }

    private fun updateUser() {
        val userToUpdate = uiState.selectedUser ?: return
        screenModelScope.launch {
            // 1. Update user details (name, username)
            userService.updateUser(userToUpdate.id, UpdateUserRequest(formState.username, formState.fullName))
                .onFailure {
                    uiState = uiState.copy(actionMessage = "Error updating user: ${it.message}")
                    return@launch
                }

            // 2. Update roles
            userService.assignRolesToUser(userToUpdate.id, AssignRolesRequest(formState.assignedRoleIds))
                .onSuccess {
                    uiState = uiState.copy(actionMessage = "User '${it.username}' updated successfully.")
                    clearSelection()
                    loadUsersAndRoles() // Refresh list
                }
                .onFailure {
                    uiState = uiState.copy(actionMessage = "Error assigning roles: ${it.message}")
                }
        }
    }

    fun resetPin(newPin: String) {
        val userToUpdate = uiState.selectedUser ?: return
        screenModelScope.launch {
            userService.resetUserPin(userToUpdate.id, ResetPinRequest(newPin))
                .onSuccess {
                    uiState = uiState.copy(actionMessage = "PIN for '${userToUpdate.username}' has been reset.")
                    hideDialogs()
                }
                .onFailure {
                    uiState = uiState.copy(actionMessage = "Error resetting PIN: ${it.message}")
                }
        }
    }

    // --- Dialog and Snackbar Management ---

    fun showResetPinDialog(user: UserResponse) {
        // We need to set the selected user here so the dialog knows who to reset
        uiState = uiState.copy(selectedUser = user, isResetPinDialogVisible = true)
    }

    fun hideDialogs() {
        uiState = uiState.copy(isResetPinDialogVisible = false)
        // Important: Don't clear the main selection here, as the user might still want to see the form populated.
        // The dialog is a separate action.
    }

    fun onActionMessageShown() {
        uiState = uiState.copy(actionMessage = null)
    }
}