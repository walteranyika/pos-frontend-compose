package com.chui.pos.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

private val logger = LoggerFactory.getLogger(SettingsManager::class.java)

data class AppSettings(
    val baseUrl: String = "http://127.0.0.1:9000/api/pos/",
    val printerName: String = ""
)

class SettingsManager {
    private val propertiesFile = File(System.getProperty("user.home"), "chui_pos_settings.properties")
    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    companion object {
        const val KEY_BASE_URL = "api.baseurl"
        const val KEY_PRINTER_NAME = "printer.name"
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            if (propertiesFile.exists()) {
                val properties = Properties()
                FileInputStream(propertiesFile).use { properties.load(it) }
                _settings.value = AppSettings(
                    baseUrl = properties.getProperty(KEY_BASE_URL, "http://127.0.0.1:9000/api/pos/"),
                    printerName = properties.getProperty(KEY_PRINTER_NAME, "")
                )
                logger.info("Settings loaded from ${propertiesFile.absolutePath}")
            } else {
                logger.info("Settings file not found, using default settings and creating file.")
                saveSettings(_settings.value)
            }
        } catch (e: Exception) {
            logger.error("Failed to load settings.", e)
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        try {
            val properties = Properties()
            properties.setProperty(KEY_BASE_URL, newSettings.baseUrl)
            properties.setProperty(KEY_PRINTER_NAME, newSettings.printerName)
            FileOutputStream(propertiesFile).use { properties.store(it, "ChuiPOS Application Settings") }
            _settings.value = newSettings
            logger.info("Settings saved to ${propertiesFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save settings.", e)
        }
    }
}