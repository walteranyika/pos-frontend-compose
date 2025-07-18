package com.chui.pos.viewmodels


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.*
import com.chui.pos.services.ProductService
import com.chui.pos.services.PurchaseService
import kotlinx.coroutines.launch

data class PurchaseUiState(
    val purchases: List<PurchaseResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreateDialogVisible: Boolean = false
)

data class CreatePurchaseState(
    val supplier: String = "",
    val items: List<PurchaseItemRequest> = emptyList(),
    val searchResults: List<ProductResponse> = emptyList(),
    val searchQuery: String = ""
)

class PurchaseViewModel(
    private val purchaseService: PurchaseService,
    private val productService: ProductService
) : ScreenModel {

    var uiState by mutableStateOf(PurchaseUiState())
        private set

    var createPurchaseState by mutableStateOf(CreatePurchaseState())
        private set

    init {
        loadPurchases()
    }

    fun loadPurchases() {
        uiState = uiState.copy(isLoading = true, error = null)
        screenModelScope.launch {
            purchaseService.getAllPurchases()
                .onSuccess { purchases ->
                    uiState = uiState.copy(isLoading = false, purchases = purchases)
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun showCreateDialog() {
        uiState = uiState.copy(isCreateDialogVisible = true)
    }

    fun hideCreateDialog() {
        uiState = uiState.copy(isCreateDialogVisible = false)
        createPurchaseState = CreatePurchaseState() // Reset on close
    }

    fun onSupplierChanged(supplier: String) {
        createPurchaseState = createPurchaseState.copy(supplier = supplier)
    }

    fun onSearchQueryChanged(query: String) {
        createPurchaseState = createPurchaseState.copy(searchQuery = query)
        if (query.length > 2) {
            screenModelScope.launch {
                productService.searchProducts(query)
                    .onSuccess { results ->
                        createPurchaseState = createPurchaseState.copy(searchResults = results)
                    }
            }
        } else {
            createPurchaseState = createPurchaseState.copy(searchResults = emptyList())
        }
    }

    fun addProductToPurchase(product: ProductResponse) {
        val existingItem = createPurchaseState.items.find { it.productId == product.id }
        if (existingItem == null) {
            val newItem = PurchaseItemRequest(
                productId = product.id,
                quantity = 1.0,
                costPrice = product.cost
            )
            createPurchaseState = createPurchaseState.copy(
                items = createPurchaseState.items + newItem,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun removeProductFromPurchase(productId: Int) {
        createPurchaseState = createPurchaseState.copy(
            items = createPurchaseState.items.filterNot { it.productId == productId }
        )
    }

    fun updatePurchaseItem(productId: Int, newQuantity: String, newCost: String) {
        val updatedItems = createPurchaseState.items.map {
            if (it.productId == productId) {
                it.copy(
                    quantity = newQuantity.toDoubleOrNull() ?: it.quantity,
                    costPrice = newCost.toDoubleOrNull() ?: it.costPrice
                )
            } else {
                it
            }
        }
        createPurchaseState = createPurchaseState.copy(items = updatedItems)
    }

    fun submitPurchase() {
        if (createPurchaseState.items.isEmpty()) return

        uiState = uiState.copy(isLoading = true)
        screenModelScope.launch {
            val request = PurchaseRequest(
                supplier = createPurchaseState.supplier.takeIf { it.isNotBlank() },
                items = createPurchaseState.items
            )
            purchaseService.createPurchase(request)
                .onSuccess {
                    hideCreateDialog()
                    loadPurchases() // Refresh list
                }
                .onFailure { error ->
                    // You might want to show this error in the dialog
                    uiState = uiState.copy(isLoading = false, error = error.message)
                }
        }
    }
}