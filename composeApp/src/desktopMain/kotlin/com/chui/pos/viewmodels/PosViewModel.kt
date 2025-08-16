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
import com.chui.pos.dtos.CreateCustomerRequest
import com.chui.pos.dtos.CustomerResponse
import com.chui.pos.dtos.HeldOrderResponse
import com.chui.pos.dtos.HoldOrderItemRequest
import com.chui.pos.dtos.HoldOrderRequest
import com.chui.pos.dtos.PaymentRequest
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.dtos.SaleItemRequest
import com.chui.pos.services.CategoryService
import com.chui.pos.services.CustomerService
import com.chui.pos.services.HeldOrderService
import com.chui.pos.services.PrintingService
import com.chui.pos.services.ProductService
import com.chui.pos.services.SaleService
import com.chui.pos.services.SoundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val soundService: SoundService,
    private val customerService: CustomerService,
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

    private val _allCustomers = MutableStateFlow<List<CustomerResponse>>(emptyList())
    val selectedCustomer = MutableStateFlow<CustomerResponse?>(null)
    val customerSearchQuery = MutableStateFlow("")
    var showAddCustomerDialog by mutableStateOf(false)
        private set


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

    var variablePriceProduct by mutableStateOf<ProductResponse?>(null)
        private set


    init {
        loadInitialData()
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

    // Derived state to filter customers based on the search query
    val filteredCustomers = combine(
        _allCustomers,
        customerSearchQuery
    ) { customers, query ->
        if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber?.contains(query) == true
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    fun showAddCustomerDialog() {
        showAddCustomerDialog = true
    }

    fun hideAddCustomerDialog() {
        showAddCustomerDialog = false
    }

    fun onCustomerSelected(customer: CustomerResponse) {
        selectedCustomer.value = customer
        // Set search query to the customer's name for display, but don't trigger a new search
        customerSearchQuery.value = customer.name
    }

    fun onCustomerSearchQueryChanged(query: String) {
        customerSearchQuery.value = query
    }

    fun createCustomer(name: String, phone: String) {
        if (name.isBlank()) return
        val request = CreateCustomerRequest(name, phone.ifBlank { null })
        screenModelScope.launch {
            customerService.createCustomer(request)
                .onSuccess { newCustomer ->
                    hideAddCustomerDialog()
                    // Refresh customer list and select the new one
                    loadCustomers {
                        onCustomerSelected(newCustomer)
                    }
                    actionMessage = "Customer '${newCustomer.name}' created."
                }
                .onFailure { error ->
                    actionMessage = "Error: ${error.message}"
                }
        }
    }


    private fun loadInitialData() {
        productsState = ProductsUiState.Loading
        screenModelScope.launch {
            coroutineScope {
                // Load products, categories, and customers in parallel
                val productsJob = async { productService.getProducts() }
                val categoriesJob = async { categoryService.getCategories() }
                val customersJob = async { customerService.getCustomers() }

                val productsResult = productsJob.await()
                val categoriesResult = categoriesJob.await()
                val customersResult = customersJob.await()

                // Handle results
                val products = productsResult.getOrElse {
                    productsState = ProductsUiState.Error("Failed to load products: ${it.message}")
                    return@coroutineScope
                }
                val categories = categoriesResult.getOrElse {
                    productsState = ProductsUiState.Error("Failed to load categories: ${it.message}")
                    return@coroutineScope
                }
                _allCustomers.value = customersResult.getOrElse {
                    actionMessage = "Failed to load customers: ${it.message}"
                    emptyList()
                }

                productsState = ProductsUiState.Success(products, categories)
                selectDefaultCustomer() // Select "Walk-in Customer" on startup
            }
        }
    }

    private fun loadCustomers(onComplete: () -> Unit = {}) {
        screenModelScope.launch {
            customerService.getCustomers()
                .onSuccess {
                    _allCustomers.value = it
                    onComplete()
                }
                .onFailure { actionMessage = "Failed to refresh customers: ${it.message}" }
        }
    }

    private fun selectDefaultCustomer() {
        val walkIn = _allCustomers.value.find { it.name.equals("Walk-in Customer", ignoreCase = true) }
        if (walkIn != null) {
            onCustomerSelected(walkIn)
        }
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
        if (product.isVariablePriced) {
            // If variable, show the dialog
            variablePriceProduct = product
        } else {
            // If standard, use existing logic to add to cart
            val existingItem = _cartItems.value[product.id]
            if (existingItem != null && !existingItem.isVariablePriced) {
                _cartItems.value = _cartItems.value + (product.id to existingItem.copy(quantity = existingItem.quantity + 1.0))
            } else if (existingItem == null) {
                val newItem = CartItem(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = 1.0, // Use Double
                    isVariablePriced = false // Mark as standard
                )
                _cartItems.value = _cartItems.value + (product.id to newItem)
            }

        }

        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            recalculateTotal(mutableCart)
            mutableCart
        }
        soundService.playAddToCartSound()

    }

    /*    fun onProductClicked(product: ProductResponse) {
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
            soundService.playAddToCartSound()
        }*/

    fun addPayment(payment: PaymentRequest) {
        _payments.update { it + payment }
    }

    fun removePayment(payment: PaymentRequest) {
        _payments.update { it - payment }
    }

    fun submitSale() {
        val customerId = selectedCustomer.value?.id ?: run {
            actionMessage = "Please select a customer before submitting a sale."
            return
        }
        saleSubmissionState = SaleSubmissionState.Loading

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
            payments = _payments.value,
            customerId = customerId
        )
        screenModelScope.launch {


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

    fun hideVariablePriceDialog() {
        variablePriceProduct = null
    }


    fun addVariablePriceItemToCart(amount: Double) {
        val product = variablePriceProduct ?: return

        // Avoid division by zero
        if (product.price <= 0) {
            actionMessage = "Cannot calculate quantity for a product with zero price."
            hideVariablePriceDialog()
            return
        }

        val calculatedQuantity = amount / product.price

        val newItem = CartItem(
            productId = product.id,
            name = product.name,
            price = product.price, // The unit price
            quantity = calculatedQuantity,
            isVariablePriced = true // Mark as variable
        )

        // Add to existing quantity if already in cart, otherwise add as new
        val existingItem = _cartItems.value[product.id]
        val updatedItem = if (existingItem != null) {
            newItem.copy(quantity = existingItem.quantity + calculatedQuantity)
        } else {
            newItem
        }

        _cartItems.value = _cartItems.value + (product.id to updatedItem)
        hideVariablePriceDialog()
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            recalculateTotal(mutableCart)
            mutableCart
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

  /*  fun incrementQuantity(productId: Int) {
        updateItemQuantity(productId, 1)
        soundService.playAddToCartSound()
    }

    fun decrementQuantity(productId: Int) {
        updateItemQuantity(productId, -1)
        soundService.playAddToCartSound()
    }*/


    // 4. Update increment/decrement to respect the new flag
    fun incrementQuantity(productId: Int) {
        val item = _cartItems.value[productId] ?: return
        // Only allow for standard items
        if (!item.isVariablePriced) {
            _cartItems.value = _cartItems.value + (productId to item.copy(quantity = item.quantity + 1.0))
        }
        computeTotals()
        soundService.playAddToCartSound()
    }

    fun decrementQuantity(productId: Int) {
        val item = _cartItems.value[productId] ?: return
        // Only allow for standard items
        if (!item.isVariablePriced) {
            val newQuantity = item.quantity - 1.0
            if (newQuantity > 0) {
                _cartItems.value = _cartItems.value + (productId to item.copy(quantity = newQuantity))
            } else {
                removeItem(productId)
            }
        }
        computeTotals()
        soundService.playAddToCartSound()
    }

    fun removeItem(productId: Int) {
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            mutableCart.remove(productId)
            recalculateTotal(mutableCart)
            mutableCart
        }
        computeTotals()
        soundService.playAddToCartSound()
    }

    fun holdCurrentOrder() {
       if (_cartItems.value.isEmpty()){
           actionMessage = "No items in cart"
           return
       }
        val customerId = selectedCustomer.value?.id ?: run {
            actionMessage = "Please select a customer to hold the order."
            return
        }
        val request = HoldOrderRequest(
            items = _cartItems.value.values.map {
                HoldOrderItemRequest(
                    productId = it.productId,
                    quantity = it.quantity.toDouble()
                )
            },
            customerId = customerId
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
        selectDefaultCustomer()
        if (clearHeldContext) {
            activeHeldOrderId = null
        }
    }

    fun resumeHeldOrder(heldOrder: HeldOrderResponse){
        clearCart(clearHeldContext = false)
        val customer = _allCustomers.value.find { it.id == heldOrder.customerId }
        if (customer != null) {
            onCustomerSelected(customer)
        } else {
            // Fallback to walk-in if customer was deleted, or handle error
            selectDefaultCustomer()
            actionMessage = "Warning: Original customer not found."
        }

        val resumeCartItems = heldOrder.items.associate {
            it.productId to CartItem(
                productId = it.productId,
                name = it.productName,
                price = it.price,
                quantity = it.quantity,
                isVariablePriced = it.isVariablePriced
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

    private fun computeTotals(){
        _cartItems.update { currentCart ->
            val mutableCart = currentCart.toMutableMap()
            recalculateTotal(mutableCart)
            mutableCart
        }
    }

    private fun recalculateTotal(cart: Map<Int, CartItem>) {
        _cartTotal.value = cart.values.sumOf { it.total }
    }
}