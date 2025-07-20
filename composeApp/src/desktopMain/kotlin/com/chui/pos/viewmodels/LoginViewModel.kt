package com.chui.pos.viewmodels


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.managers.AuthManager
import com.chui.pos.services.LoginService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginService: LoginService,
    private val authManager: AuthManager
) : ScreenModel {
    var pin by mutableStateOf("")
        private set

    var loginState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    private val _events = MutableSharedFlow<LoginEvent>()
    val events = _events.asSharedFlow()

    fun onPinChange(newPin: String) {
        if (newPin.length <= 4) {
            pin = newPin
        }
    }

    fun onLoginClicked() {
        if (pin.length == 4) {
            loginState = LoginUiState.Loading
            screenModelScope.launch {
                val result = loginService.login(pin)
                result.onSuccess { response ->
                    authManager.saveSession(
                        token = response.token,
                        fullName = response.fullName,
                        username = response.username,
                        permissions = response.permissions
                    )
                    // Reset the UI state and send a one-time event to navigate
                    loginState = LoginUiState.Idle
                    _events.emit(LoginEvent.NavigateToHome)
                }.onFailure { error ->
                    loginState = LoginUiState.Error(error.message?:"Invalid PIN or connection error.")
                    pin = "" // Clear PIN on failure
                }
            }
        }
    }

    fun onClear() {
        pin = ""
    }
}

// Represents the different states of the Login UI
sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

// Represents one-time events for the UI to consume
sealed interface LoginEvent {
    object NavigateToHome : LoginEvent
}