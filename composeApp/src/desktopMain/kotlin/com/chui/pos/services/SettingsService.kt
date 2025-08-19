package com.chui.pos.services

import java.util.prefs.Preferences

/**
 * A service to manage persistent application settings using Java's Preferences API.
 */
class SettingsService {
    // Creates a preference node unique to the application
    private val prefs = Preferences.userNodeForPackage(SettingsService::class.java)

    companion object {
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_PRINTER_NAME = "printer_name"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }

    fun saveBaseUrl(url: String) = prefs.put(KEY_BASE_URL, url)
    fun loadBaseUrl(): String = prefs.get(KEY_BASE_URL, "http://localhost:9000/api/pos/")

    fun savePrinterName(name: String) = prefs.put(KEY_PRINTER_NAME, name)
    fun loadPrinterName(): String = prefs.get(KEY_PRINTER_NAME, "")

    fun saveSoundEnabled(enabled: Boolean) = prefs.putBoolean(KEY_SOUND_ENABLED, enabled)
    fun loadSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true) // Default to ON
}