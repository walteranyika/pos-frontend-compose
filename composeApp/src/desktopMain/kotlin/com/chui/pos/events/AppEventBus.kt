package com.chui.pos.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * A singleton event bus for broadcasting and listening to AppEvents across the application.
 * Implemented using a MutableSharedFlow.
 */
class AppEventBus {
    // Use a dedicated scope to send events without blocking the caller.
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    /**
     * Sends an event to all active listeners.
     */
    fun sendEvent(event: AppEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}