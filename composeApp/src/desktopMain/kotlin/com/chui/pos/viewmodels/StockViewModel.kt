package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.dtos.StockAdjustmentRequest
import com.chui.pos.services.ProductService
import com.chui.pos.services.StockService
import kotlinx.coroutines.launch

data class StockUiState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedProduct: ProductResponse? = null,
    val adjustmentMessage: String? = null
)

class StockViewModel(
    private val productService: ProductService,
    private val stockService: StockService
) : ScreenModel {

    var uiState by mutableStateOf(StockUiState())
        private set

    var newQuantity by mutableStateOf("")
        private set

    init {
        loadProducts()
    }

    fun loadProducts() {
        uiState = uiState.copy(isLoading = true, error = null)
        screenModelScope.launch {
            productService.getProducts()
                .onSuccess { products ->
                    uiState = uiState.copy(isLoading = false, products = products)
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun onProductSelected(product: ProductResponse) {
        uiState = uiState.copy(selectedProduct = product)
        newQuantity = product.quantity.toString()
    }

    fun onNewQuantityChanged(value: String) {
        // Allow only numbers and a single decimal point
        if (value.matches(Regex("^\\d*\\.?\\d*\$"))) {
            newQuantity = value
        }
    }

    fun adjustStock() {
        val product = uiState.selectedProduct ?: return
        val quantity = newQuantity.toDoubleOrNull() ?: return

        uiState = uiState.copy(isLoading = true)
        screenModelScope.launch {
            val request = StockAdjustmentRequest(productId = product.id, newQuantity = quantity)
            stockService.adjustStock(request)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false, adjustmentMessage = "Stock updated successfully!")
                    loadProducts() // Refresh the list
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, adjustmentMessage = "Error: ${error.message}")
                }
        }
    }

    fun onAdjustmentMessageShown() {
        uiState = uiState.copy(adjustmentMessage = null)
    }
}