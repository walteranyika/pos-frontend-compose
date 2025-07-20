package com.chui.pos.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.CartItem
import com.chui.pos.dtos.CreateSaleRequest
import com.chui.pos.dtos.CategoryResponse
import com.chui.pos.dtos.HeldOrderResponse
import com.chui.pos.dtos.HoldOrderItemRequest
import com.chui.pos.dtos.HoldOrderRequest
import com.chui.pos.dtos.PaymentRequest
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.dtos.SaleItemRequest
import com.chui.pos.services.CategoryService
import com.chui.pos.services.HeldOrderService
import com.chui.pos.services.PrintingService
import com.chui.pos.services.ProductService
import com.chui.pos.services.SaleService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    object Loading : ProductsUiState
    data class Success(val products: List<ProductResponse>, val categories: List<CategoryResponse>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

sealed interface SaleSubmissionState {
    object Idle : SaleSubmissionState
    object Loading : SaleSubmissionState
    object Success : SaleSubmissionState
    data class Error(val message: String) : SaleSubmissionState
}

sealed interface PosUiState {
    object Idle : PosUiState
    object Loading : PosUiState
    data class Success(val products: List<ProductResponse>) : PosUiState
    data class Error(val message: String) : PosUiState
}

class PosViewModel(
    private val productService: ProductService,
    private val saleService: SaleService,
    private val categoryService: CategoryService,
    private val printingService: PrintingService,
    private val heldOrderService: HeldOrderService,
) : ScreenModel {

    private val _uiState = mutableStateOf<PosUiState>(PosUiState.Loading)
    val uiState: MutableState<PosUiState> = _uiState

    var productsState by mutableStateOf<ProductsUiState>(ProductsUiState.Loading)
        private set

    private val _cartItems = MutableStateFlow<Map<Int, CartItem>>(emptyMap())
    val cartItems = _cartItems.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal = _cartTotal.asStateFlow()

    var showPaymentDialog by mutableStateOf(false)
        private set

    private val _payments = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val payments = _payments.asStateFlow()

    var saleSubmissionState by mutableStateOf<SaleSubmissionState>(SaleSubmissionState.Idle)
        private set

    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<ProductResponse>>(emptyList())
        private set
    private var searchJob: Job? = null

    var selectedCategoryId by mutableStateOf<Int?>(null)
        private set

    var printReceipt by mutableStateOf(true) // Default to true
        private set

    var showHeldOrdersDialog by mutableStateOf(false)
        private set
    var helOrders by mutableStateOf<List<HeldOrderResponse>>(emptyList())
        private set
    var activeHeldOrderId by mutableStateOf<Long?>(null)
        private set
    var actionMessage by mutableStateOf<String?>(null)
        private set




    init {
        fetchProductsAndCategories()
    }

    fun onOpenPaymentDialog() {
        showPaymentDialog = true
    }

    fun onDismissPaymentDialog() {
        showPaymentDialog = false
    }

    fun onActionMessageShown(){
        actionMessage = null
    }




    private fun fetchProductsAndCategories() {
        productsState = ProductsUiState.Loading
        screenModelScope.launch {
            coroutineScope {
                val productsDeferred = async { productService.getProducts() }
                val categoriesDeferred = async { categoryService.getCategories() }

                val products = productsDeferred.await().getOrElse {
                    productsState = ProductsUiState.Error("Failed to load products: ${it.message}")
                    return@coroutineScope
                }
                val categories = categoriesDeferred.await().getOrElse {
                    productsState = ProductsUiState.Error("Failed to load categories: ${it.message}")
                    return@coroutineScope
                }

                productsState = ProductsUiState.Success(products, categories)
            }
        }
    }

    fun onCategorySelected(categoryId: Int?) {
        val currentState = productsState
        if (currentState !is ProductsUiState.Success) return

        selectedCategoryId = categoryId
        screenModelScope.launch {
            val productsResult = if (categoryId == null) {
                productService.getProducts()
            } else {
                productService.getProductsByCategory(categoryId)
            }

            productsResult.onSuccess { filteredProducts ->
                productsState = currentState.copy(products = filteredProducts)
            }.onFailure { error ->
                println("Error filtering products: ${error.message}")
            }
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

    fun addPayment(payment: PaymentRequest) {
        _payments.update { it + payment }
    }

    fun removePayment(payment: PaymentRequest) {
        _payments.update { it - payment }
    }

    fun submitSale() {
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
                    clearCart()
                    showPaymentDialog = false
                    if (printReceipt) {
                        //printingService.printReceipt(saleResponse)
                    }
                }.onFailure {
                    saleSubmissionState = SaleSubmissionState.Error(it.message ?: "Failed to submit sale")
                }
        }
    }


    fun onPrintReceiptChanged(shouldPrint: Boolean) {
        printReceipt = shouldPrint
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
            productService.searchProducts(query)
                .onSuccess { searchResults = it }
                .onFailure {
                    println("Search failed: ${it.message}")
                    searchResults = emptyList()
                }
        }
    }

    fun onSearchResultSelected(product: ProductResponse) {
        onProductClicked(product)
        searchQuery = ""
        searchResults = emptyList()
        searchJob?.cancel()
    }

    fun resetSaleSubmissionState() {
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

    fun holdCurrentOrder() {
       if (_cartItems.value.isEmpty()){
           actionMessage = "No items in cart"
           return
       }
        val request = HoldOrderRequest(
            items = _cartItems.value.values.map {
                HoldOrderItemRequest(
                    productId = it.productId,
                    quantity = it.quantity.toDouble()
                )
            }
        )

        screenModelScope.launch {
            val result = if (activeHeldOrderId == null){
                heldOrderService.holdOrder(request)
            } else {
                heldOrderService.updateHeldOrder(activeHeldOrderId!!, request)
            }

            result.onSuccess {
                clearCart()
                actionMessage = "Order held successfully"
            }.onFailure {
                actionMessage = "Error holding order"
            }
        }

    }

    fun showHeldOrdersDialog(){
        showHeldOrdersDialog = true
        loadHeldOrders()
    }

    fun hideHeldOrdersDialog(){
        showHeldOrdersDialog = false
    }

    private fun loadHeldOrders(){
        screenModelScope.launch {
          heldOrderService.getHeldOrders().onSuccess {
              helOrders = it
          }
        }
    }

    fun clearCart(clearHeldContext: Boolean = true) {
        _cartItems.value = emptyMap()
        _payments.value = emptyList()
        recalculateTotal(emptyMap())
        if (clearHeldContext) {
            activeHeldOrderId = null
        }
    }

    fun resumeHeldOrder(heldOrder: HeldOrderResponse){
//        clearCart(clearHeldContext = false)
        val resumeCartItems = heldOrder.items.associate {
            it.productId to CartItem(
                productId = it.productId,
                name = it.productName,
                price = it.price,
                quantity = it.quantity.toInt()
            )
        }
        _cartItems.value = resumeCartItems
        recalculateTotal(resumeCartItems)
        activeHeldOrderId = heldOrder.id
        showHeldOrdersDialog = false
        actionMessage = "Order Resumed ${heldOrder.id}"
    }

    fun deleteHeldOrder(orderId: Long){
        screenModelScope.launch {
            heldOrderService.deleteHeldOrder(orderId)
            loadHeldOrders()
        }
    }



    private fun updateItemQuantity(productId: Int, delta: Int) {
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            val cartItem = mutableCart[productId]
            if (cartItem != null) {
                val newQuantity = cartItem.quantity + delta
                if (newQuantity > 0) mutableCart[productId] =
                    cartItem.copy(quantity = newQuantity) else mutableCart.remove(productId)
                recalculateTotal(mutableCart)
            }
            mutableCart
        }
    }

    private fun recalculateTotal(cart: Map<Int, CartItem>) {
        _cartTotal.value = cart.values.sumOf { it.total }
    }
}