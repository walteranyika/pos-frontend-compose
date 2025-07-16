package com.chui.pos.managers

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val settings: Settings) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
    }

    private val _isLoggedInState = MutableStateFlow(isLoggedIn())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedInState.asStateFlow()

    fun saveSession(token: String, firstName: String, lastName: String) {
        settings[KEY_TOKEN] = token
        settings[KEY_FIRST_NAME] = firstName
        settings[KEY_LAST_NAME] = lastName
        _isLoggedInState.value = true
    }

    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)

    fun getUserFullName(): String? {
        val firstName = settings.getStringOrNull(KEY_FIRST_NAME)
        val lastName = settings.getStringOrNull(KEY_LAST_NAME)
        return if (firstName != null && lastName != null) "$firstName $lastName" else null
    }

    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_FIRST_NAME)
        settings.remove(KEY_LAST_NAME)
        _isLoggedInState.value = false
    }

    fun isLoggedIn(): Boolean = getToken() != null
}