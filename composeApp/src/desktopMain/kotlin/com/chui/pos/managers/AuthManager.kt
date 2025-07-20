package com.chui.pos.managers

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val settings: Settings) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val FULL_NAME = "full_name"
        private const val USERNAME = "username"
        private const val KEY_PERMISSIONS = "user_permissions"
    }

    private val _isLoggedInState = MutableStateFlow(isLoggedIn())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedInState.asStateFlow()

    fun saveSession(token: String, fullName: String, username: String, permissions: Set<String>) {
        settings[KEY_TOKEN] = token
        settings[FULL_NAME] = fullName
        settings[USERNAME] = username
        settings[KEY_PERMISSIONS] = permissions.joinToString(",")
        _isLoggedInState.value = true
    }

    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)

    fun getUserFullName(): String? {
        val fullName = settings.getStringOrNull(FULL_NAME)
        val username = settings.getStringOrNull(USERNAME)
        return if (fullName != null && username != null) fullName else null
    }

    fun getUserPermissions(): Set<String> {
        val permissionsString = settings.getStringOrNull(KEY_PERMISSIONS) ?: return emptySet()
        return if (permissionsString.isBlank()) emptySet() else permissionsString.split(",").toSet()
    }

    fun hasPermission(permission: String): Boolean {
        return getUserPermissions().contains(permission)
    }


    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(FULL_NAME)
        settings.remove(USERNAME)
        settings.remove(KEY_PERMISSIONS)
        _isLoggedInState.value = false
    }

    fun isLoggedIn(): Boolean = getToken() != null
}