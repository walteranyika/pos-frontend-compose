package com.chui.pos.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.PagedResponse
import com.chui.pos.dtos.SaleSummaryResponse
import com.chui.pos.services.ReportService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ReportsUiState {
    object Loading : ReportsUiState
    data class Success(val salesPage: PagedResponse<SaleSummaryResponse>) : ReportsUiState
    data class Error(val message: String) : ReportsUiState
}

class ReportsViewModel(private val reportService: ReportService) : ScreenModel {

    private val _uiState = mutableStateOf<ReportsUiState>(ReportsUiState.Loading)
    val uiState: State<ReportsUiState> = _uiState

    // Filter states
    var searchQuery by mutableStateOf("")
        private set
    var startDate by mutableStateOf<LocalDate?>(null)
        private set
    var endDate by mutableStateOf<LocalDate?>(null)
        private set

    // Selection state
    var selectedSale by mutableStateOf<SaleSummaryResponse?>(null)
        private set

    private var searchJob: Job? = null

    init {
        fetchSales()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            delay(500L) // debounce
            fetchSales()
        }
    }

    fun onDateChange(start: LocalDate?, end: LocalDate?) {
        startDate = start
        endDate = end
        fetchSales()
    }

    fun onSaleSelected(sale: SaleSummaryResponse) {
        selectedSale = sale
    }

    fun fetchSales(page: Int = 0) {
        if (page == 0) {
            _uiState.value = ReportsUiState.Loading
        }

        screenModelScope.launch {
            reportService.getRecentSales(searchQuery, startDate, endDate, page)
                .onSuccess { _uiState.value = ReportsUiState.Success(it) }
                .onFailure { _uiState.value = ReportsUiState.Error(it.message ?: "Failed to fetch reports") }
        }
    }
}