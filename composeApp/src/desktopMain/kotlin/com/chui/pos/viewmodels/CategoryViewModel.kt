package com.chui.pos.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.CategoryRequest
import com.chui.pos.dtos.CategoryResponse
import com.chui.pos.services.CategoryService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryResponse>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

class CategoryViewModel(private val categoryService: CategoryService) : ScreenModel {

    private val _uiState = mutableStateOf<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: State<CategoriesUiState> = _uiState

    // Form State
    var formName by mutableStateOf("")
        private set
    var formCode by mutableStateOf("")
        private set
    var selectedCategoryId by mutableStateOf<Int?>(null)
        private set
    var showDeleteConfirmDialog by mutableStateOf(false)
        private set

    // Search State
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<CategoryResponse>>(emptyList())
        private set
    private var searchJob: Job? = null

    val isEditing: Boolean
        get() = selectedCategoryId != null

    init {
        fetchCategories()
    }

    fun onNameChange(name: String) {
        formName = name
    }

    fun onCodeChange(code: String) {
        formCode = code
    }

    fun onCategorySelected(category: CategoryResponse) {
        selectedCategoryId = category.id
        formName = category.name
        formCode = category.code
    }

    fun clearSelection() {
        selectedCategoryId = null
        formName = ""
        formCode = suggestNextCategoryCode()
    }

    fun saveCategory() {
        val id = selectedCategoryId
        val request = CategoryRequest(name = formName, code = formCode)
        screenModelScope.launch {
            val result = if (id != null) {
                categoryService.updateCategory(id, request)
            } else {
                categoryService.createCategory(request)
            }

            result.onSuccess {
                clearSelection()
                fetchCategories() // Refresh list
            }.onFailure {
                println("Error saving category: ${it.message}")
            }
        }
    }

    fun onDeleteClicked() {
        showDeleteConfirmDialog = true
    }

    fun onDismissDeleteDialog() {
        showDeleteConfirmDialog = false
    }

    fun confirmDelete() {
        val id = selectedCategoryId ?: return
        screenModelScope.launch {
            categoryService.deleteCategory(id)
                .onSuccess {
                    clearSelection()
                    fetchCategories()
                }.onFailure { println("Error deleting category: ${it.message}") }
            showDeleteConfirmDialog = false
        }
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
            categoryService.searchCategories(query)
                .onSuccess { searchResults = it }
                .onFailure { searchResults = emptyList() }
        }
    }

    fun onSearchResultSelected(category: CategoryResponse) {
        onCategorySelected(category)
        searchQuery = ""
        searchResults = emptyList()
        searchJob?.cancel()
    }

    private fun fetchCategories() {
        _uiState.value = CategoriesUiState.Loading
        screenModelScope.launch {
            categoryService.getCategories().onSuccess { categories ->
                _uiState.value = CategoriesUiState.Success(categories)
                if (!isEditing) {
                    clearSelection()
                }
            }
                .onFailure { error -> _uiState.value = CategoriesUiState.Error(error.message ?: "Unknown error") }
        }
    }

    private fun suggestNextCategoryCode(): String {
        val currentState = _uiState.value
        if (currentState is CategoriesUiState.Success) {
            val lastCode = currentState.categories.mapNotNull { it.code.toIntOrNull() }.maxOrNull() ?: 0
            return (lastCode + 1).toString().padStart(3, '0')
        }
        return "001" // Default if no categories are loaded yet
    }
}