package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.ProductUnitRequest
import com.chui.pos.dtos.ProductUnitResponse
import com.chui.pos.services.UnitService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface UnitsUiState {
    object Loading : UnitsUiState
    data class Success(val units: List<ProductUnitResponse>) : UnitsUiState
    data class Error(val message: String) : UnitsUiState
}

class UnitViewModel(private val unitService: UnitService) : ScreenModel {

    var uiState by mutableStateOf<UnitsUiState>(UnitsUiState.Loading)
        private set

    // Search State
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<ProductUnitResponse>>(emptyList())
        private set
    private var searchJob: Job? = null

    // Form State
    var formName by mutableStateOf("")
        private set
    var formShortName by mutableStateOf("")
        private set

    var selectedUnitId by mutableStateOf<Int?>(null)
        private set


    var showDeleteConfirmDialog by mutableStateOf(false)
        private set

    val isEditing: Boolean
        get() = selectedUnitId != null

    init {
        fetchUnits()
    }

    fun onNameChange(name: String) {
        formName = name
    }

    fun onShortNameChange(shortName: String) {
        formShortName = shortName
    }

    fun onUnitSelected(unit: ProductUnitResponse) {
        selectedUnitId = unit.id
        formName = unit.name
        formShortName = unit.shortName
    }

    fun clearSelection() {
        selectedUnitId = null
        formName = ""
        formShortName = ""
    }

    fun onDeleteClicked() {
        if (isEditing) {
            showDeleteConfirmDialog = true
        }
    }

    fun onDismissDeleteDialog() {
        showDeleteConfirmDialog = false
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        searchJob = screenModelScope.launch {
            delay(300L) // debounce
            unitService.searchUnits(query)
                .onSuccess { searchResults = it }
                .onFailure {
                    println("Search failed: ${it.message}")
                    searchResults = emptyList()
                }
        }
    }

    fun onSearchResultSelected(unit: ProductUnitResponse) {
        onUnitSelected(unit)
        searchQuery = ""
        searchResults = emptyList()
        searchJob?.cancel()
    }


    fun confirmDelete() {
        val id = selectedUnitId ?: return // Guard against null id
        screenModelScope.launch {
            unitService.deleteUnit(id)
                .onSuccess {
                    clearSelection()
                    fetchUnits() // Refresh the list
                }.onFailure {
                    // In a real app, you'd show a snackbar or toast
                    println("Error deleting unit: ${it.message}")
                }
            showDeleteConfirmDialog = false // Close dialog regardless of result
        }
    }



    fun saveUnit() {
        screenModelScope.launch {
            val request = ProductUnitRequest(name = formName, shortName = formShortName)
            val result = if (isEditing) {
                unitService.updateUnit(selectedUnitId!!, request)
            } else {
                unitService.createUnit(request)
            }

            result.onSuccess {
                clearSelection()
                fetchUnits() // Refresh the list
            }.onFailure {
                // In a real app, you'd show a snackbar or toast
                println("Error saving unit: ${it.message}")
            }
        }
    }

    fun fetchUnits() {
        uiState = UnitsUiState.Loading
        screenModelScope.launch {
            unitService.getUnits()
                .onSuccess { units ->
                    uiState = UnitsUiState.Success(units)
                }
                .onFailure { error ->
                    uiState = UnitsUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}