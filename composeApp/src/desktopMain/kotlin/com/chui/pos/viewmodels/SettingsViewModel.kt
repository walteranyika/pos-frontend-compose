package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.chui.pos.services.PrintingService
import com.chui.pos.services.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val availablePrinters: List<String> = emptyList(),
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val settingsService: SettingsService,
    private val printingService: PrintingService
) : ScreenModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    var baseUrl by mutableStateOf("")
        private set
    var selectedPrinter by mutableStateOf("")
        private set
    var soundEnabled by mutableStateOf(true) // State for the sound toggle
        private set

    init {
        loadSettings()
    }

    private fun loadSettings() {
        baseUrl = settingsService.loadBaseUrl()
        selectedPrinter = settingsService.loadPrinterName()
        soundEnabled = settingsService.loadSoundEnabled() // Load the setting

        _uiState.update {
            it.copy(availablePrinters = printingService.getAvailablePrinters())
        }
    }

    fun onBaseUrlChanged(newUrl: String) {
        baseUrl = newUrl
    }

    fun onPrinterSelected(printerName: String) {
        selectedPrinter = printerName
    }

    // Handler for the new toggle
    fun onSoundToggled(isEnabled: Boolean) {
        soundEnabled = isEnabled
    }

    fun saveSettings() {
        settingsService.saveBaseUrl(baseUrl)
        settingsService.savePrinterName(selectedPrinter)
        settingsService.saveSoundEnabled(soundEnabled) // Save the new setting
        _uiState.update { it.copy(toastMessage = "Settings saved successfully!") }
    }



    fun onToastMessageShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}