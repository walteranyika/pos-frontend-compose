package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.managers.AppSettings
import com.chui.pos.managers.SettingsManager
import com.chui.pos.services.PrintingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val availablePrinters: List<String> = emptyList(),
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val printingService: PrintingService
) : ScreenModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    var baseUrl by mutableStateOf("")
        private set

    var selectedPrinter by mutableStateOf("")
        private set

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        screenModelScope.launch {
            val printers = printingService.getAvailablePrinters()
            val currentSettings = settingsManager.settings.value
            _uiState.update { it.copy(availablePrinters = printers) }
            baseUrl = currentSettings.baseUrl
            selectedPrinter = currentSettings.printerName
        }
    }

    fun onBaseUrlChanged(newUrl: String) {
        baseUrl = newUrl
    }

    fun onPrinterSelected(printerName: String) {
        selectedPrinter = printerName
    }

    fun saveSettings() {
        val newSettings = AppSettings(
            baseUrl = baseUrl.trim(),
            printerName = selectedPrinter
        )
        settingsManager.saveSettings(newSettings)
        _uiState.update { it.copy(toastMessage = "Settings saved successfully!") }
    }

    fun onToastMessageShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}