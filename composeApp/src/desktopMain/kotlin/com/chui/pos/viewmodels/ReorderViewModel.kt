package com.chui.pos.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.ReorderItemResponse
import com.chui.pos.services.ReportService
import kotlinx.coroutines.launch

data class ReorderUiState(
    val items: List<ReorderItemResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class ReorderViewModel(private val reportService: ReportService) : ScreenModel {
    var uiState by mutableStateOf(ReorderUiState())
        private set

    init {
        loadReorderItems()
    }

    fun loadReorderItems() {
        uiState = uiState.copy(isLoading = true)
        screenModelScope.launch {
            reportService.getReorderAlerts()
                .onSuccess { items ->
                    uiState = uiState.copy(isLoading = false, items = items, error = null)
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    val filteredItems by derivedStateOf {
        if (uiState.searchQuery.isBlank()) {
            uiState.items
        } else {
            val query = uiState.searchQuery.trim().lowercase()
            uiState.items.filter {
                it.productName.lowercase().contains(query) ||
                        it.productCode.lowercase().contains(query)
            }
        }
    }
}