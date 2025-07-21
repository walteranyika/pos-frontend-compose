package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.*
import com.chui.pos.services.UserService
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val users: List<UserResponse> = emptyList(),
    val roles: List<RoleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedUser: UserResponse? = null,
    val isEditDialogVisible: Boolean = false,
    val isResetPinDialogVisible: Boolean = false,
    val actionMessage: String? = null
)

class UserViewModel(private val userService: UserService) : ScreenModel {

    var uiState by mutableStateOf(UserManagementUiState())
        private set

    init {
        loadUsersAndRoles()
    }

    fun loadUsersAndRoles() {
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

    fun showEditDialog(user: UserResponse) {
        uiState = uiState.copy(selectedUser = user, isEditDialogVisible = true)
    }

    fun showResetPinDialog(user: UserResponse) {
        uiState = uiState.copy(selectedUser = user, isResetPinDialogVisible = true)
    }

    fun hideDialogs() {
        uiState = uiState.copy(
            isEditDialogVisible = false,
            isResetPinDialogVisible = false,
            selectedUser = null
        )
    }

    fun onActionMessageShown() {
        uiState = uiState.copy(actionMessage = null)
    }

    fun updateUser(fullName: String, username: String, assignedRoleIds: Set<Long>) {
        val userToUpdate = uiState.selectedUser ?: return
        screenModelScope.launch {
            // Update user details
            userService.updateUser(userToUpdate.id, UpdateUserRequest(username, fullName))
                .onFailure {
                    uiState = uiState.copy(actionMessage = "Error updating user: ${it.message}")
                    return@launch
                }

            // Update roles
            userService.assignRolesToUser(userToUpdate.id, AssignRolesRequest(assignedRoleIds))
                .onSuccess {
                    uiState = uiState.copy(actionMessage = "User '${it.username}' updated successfully.")
                    hideDialogs()
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
}