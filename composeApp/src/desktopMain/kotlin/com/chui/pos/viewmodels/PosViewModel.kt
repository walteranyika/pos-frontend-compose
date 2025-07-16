package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.CartItem
import com.chui.pos.dtos.CreateSaleRequest
import com.chui.pos.dtos.PaymentRequest
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.dtos.SaleItemRequest
import com.chui.pos.services.ProductService
import com.chui.pos.services.SaleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    object Loading : ProductsUiState
    data class Success(val products: List<ProductResponse>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

sealed interface SaleSubmissionState{
    object Idle : SaleSubmissionState
    object Loading : SaleSubmissionState
    object Success : SaleSubmissionState
    data class Error(val message: String) : SaleSubmissionState
}

class PosViewModel(private val productService: ProductService,
    private val saleService: SaleService
) : ScreenModel {

    var productsState by mutableStateOf<ProductsUiState>(ProductsUiState.Loading)
        private set

    private val _cartItems = MutableStateFlow<Map<Int, CartItem>>(emptyMap())
    val cartItems = _cartItems.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal = _cartTotal.asStateFlow()

    var showPaymentDialog by mutableStateOf(false)
        private set

    private val _payments = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val  payments = _payments.asStateFlow()

    var saleSubmissionState by mutableStateOf<SaleSubmissionState>(SaleSubmissionState.Idle)
        private set



    init {
        fetchProducts()
    }

    fun onOpenPaymentDialog(){
        showPaymentDialog = true
    }

    fun onDismissPaymentDialog(){
        showPaymentDialog = false
    }


    private fun fetchProducts() {
        productsState = ProductsUiState.Loading
        screenModelScope.launch {
            productService.getProducts()
                .onSuccess { products -> productsState = ProductsUiState.Success(products) }
                .onFailure { error -> productsState = ProductsUiState.Error(error.message ?: "Failed to load products") }
        }
    }

    fun onProductClicked(product: ProductResponse) {
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            val cartItem = mutableCart[product.id]

            if (cartItem != null) {
                mutableCart[product.id] = cartItem.copy(quantity = cartItem.quantity + 1)
            } else {
                mutableCart[product.id] = CartItem(
                    productId = product.id, name = product.name, price = product.price, quantity = 1
                )
            }
            recalculateTotal(mutableCart)
            mutableCart
        }
    }

    fun addPayment(payment: PaymentRequest){
        _payments.update { it + payment }
    }

    fun removePayment(payment: PaymentRequest){
        _payments.update { it - payment }
    }

    fun submitSale(){
        saleSubmissionState = SaleSubmissionState.Loading
        screenModelScope.launch {
            val saleItems = _cartItems.value.values.map {
                SaleItemRequest(
                    productId = it.productId,
                    quantity = it.quantity.toDouble(),
                    price = it.price,
                    discount = 0.0
                )
            }

            val saleRequest = CreateSaleRequest(
                items = saleItems,
                payments = _payments.value
            )

            saleService.createSale(saleRequest)
                .onSuccess {
                    saleSubmissionState = SaleSubmissionState.Success
                    _cartItems.value = emptyMap()
                    _payments.value = emptyList()
                    recalculateTotal(emptyMap())
                    showPaymentDialog=false
                }.onFailure {
                    saleSubmissionState = SaleSubmissionState.Error(it.message ?: "Failed to submit sale")
                }
        }
    }

    fun resetSaleSubmissionState(){
        saleSubmissionState = SaleSubmissionState.Idle
    }
    fun incrementQuantity(productId: Int) {
        updateItemQuantity(productId, 1)
    }

    fun decrementQuantity(productId: Int) {
        updateItemQuantity(productId, -1)
    }

    fun removeItem(productId: Int) {
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            mutableCart.remove(productId)
            recalculateTotal(mutableCart)
            mutableCart
        }
    }

    private fun updateItemQuantity(productId: Int, delta: Int) {
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            val cartItem = mutableCart[productId]
            if (cartItem != null) {
                val newQuantity = cartItem.quantity + delta
                if (newQuantity > 0) mutableCart[productId] = cartItem.copy(quantity = newQuantity) else mutableCart.remove(productId)
                recalculateTotal(mutableCart)
            }
            mutableCart
        }
    }

    private fun recalculateTotal(cart: Map<Int, CartItem>) {
        _cartTotal.value = cart.values.sumOf { it.total }
    }
}