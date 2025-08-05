package com.chui.pos.events

/**
 * A sealed class representing all possible global events in the application.
 */
sealed class AppEvent {
    /**
     * Fired when an API call returns a 401 Unauthorized, indicating the session has expired.
     */
    data object TokenExpired : AppEvent()

    // You can add more events here later, e.g.:
    // data class ShowSnackbar(val message: String, val type: SnackbarType) : AppEvent()
}