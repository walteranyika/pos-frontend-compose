package com.chui.pos.viewmodels

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.services.HealthService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * A singleton ViewModel responsible for monitoring the server's health.
 * Its lifecycle is tied to the application, not a specific screen.
 */
class ServerStatusViewModel(private val healthService: HealthService) : ScreenModel {

    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    init {
        startHealthChecks()
    }

    private fun startHealthChecks() {
        // Use the ViewModel's scope to launch a long-running coroutine
        screenModelScope.launch {
            flow {
                while (true) {
                    emit(Unit)
                    delay(30_000L) // Check every 15 seconds
                }
            }.collect {
                val healthCheckResult = healthService.checkHealth()
                // Only update the state if it has changed
                if (_isServerOnline.value != healthCheckResult.isSuccess) {
                    _isServerOnline.value = healthCheckResult.isSuccess
                }
            }
        }
    }
}