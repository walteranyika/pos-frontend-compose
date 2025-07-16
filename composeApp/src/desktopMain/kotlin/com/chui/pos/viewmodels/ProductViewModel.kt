package com.chui.pos.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.chui.pos.dtos.*
import com.chui.pos.services.CategoryService
import com.chui.pos.services.ProductService
import com.chui.pos.services.UnitService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class ProductFormData(
    val name: String = "",
    val code: String = "",
    val barcode: String? = null,
    val cost: String = "",
    val price: String = "",
    val category: CategoryResponse? = null,
    val saleUnit: ProductUnitResponse? = null,
    val purchaseUnit: ProductUnitResponse? = null,
    val stockAlert: String = "",
    val taxMethod: TaxType = TaxType.INCLUSIVE,
    val isVariablePriced: Boolean = false,
    val isActive: Boolean = true,
    val note: String? = null
)

sealed interface ProductsOnlyUiState {
    object Loading : ProductsOnlyUiState
    data class Success(
        val products: List<ProductResponse>,
        val categories: List<CategoryResponse>,
        val units: List<ProductUnitResponse>
    ) : ProductsOnlyUiState

    data class Error(val message: String) : ProductsOnlyUiState
}

class ProductViewModel(
    private val productService: ProductService,
    private val categoryService: CategoryService,
    private val unitService: UnitService
) : ScreenModel {

    var uiState by mutableStateOf<ProductsOnlyUiState>(ProductsOnlyUiState.Loading)
        private set

    var formState by mutableStateOf(ProductFormData())
        private set

    var selectedProductId by mutableStateOf<Int?>(null)
        private set

    var showDeleteConfirmDialog by mutableStateOf(false)
        private set

    val isEditing: Boolean get() = selectedProductId != null

    init {
        fetchAllData()
    }

    fun onFormChange(newFormState: ProductFormData) {
        formState = newFormState
    }

    fun onProductSelected(product: ProductResponse) {
        selectedProductId = product.id
        formState = ProductFormData(
            name = product.name,
            code = product.code,
            barcode = product.barcode,
            cost = product.cost.toString(),
            price = product.price.toString(),
            category = product.category,
            saleUnit = product.saleUnit,
            purchaseUnit = product.purchaseUnit,
            stockAlert = product.stockAlert.toString(),
            taxMethod = product.taxMethod,
            isVariablePriced = product.isVariablePriced,
            isActive = product.isActive,
            note = product.note
        )
    }

    fun clearSelection() {
        selectedProductId = null
        formState = ProductFormData(code = suggestNextProductCode())
    }

    fun saveProduct() {
        val currentForm = formState
        val request = ProductRequest(
            name = currentForm.name,
            code = currentForm.code,
            barcode = currentForm.barcode,
            cost = currentForm.cost.toDoubleOrNull() ?: 0.0,
            price = currentForm.price.toDoubleOrNull() ?: 0.0,
            categoryId = currentForm.category?.id ?: return,
            saleUnitId = currentForm.saleUnit?.id ?: return,
            purchaseUnitId = currentForm.purchaseUnit?.id ?: return,
            stockAlert = currentForm.stockAlert.toDoubleOrNull() ?: 0.0,
            taxMethod = currentForm.taxMethod,
            isVariablePriced = currentForm.isVariablePriced,
            isActive = currentForm.isActive,
            note = currentForm.note
        )

        screenModelScope.launch {
            val result = selectedProductId?.let {
                productService.updateProduct(it, request)
            } ?: productService.createProduct(request)

            result.onSuccess {
                clearSelection()
                fetchAllData()
            }.onFailure {
                println("Error saving product: ${it.message}")
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
        val id = selectedProductId ?: return
        screenModelScope.launch {
            productService.deleteProduct(id)
                .onSuccess {
                    clearSelection()
                    fetchAllData()
                }.onFailure { println("Error deleting product: ${it.message}") }
            showDeleteConfirmDialog = false
        }
    }

    private fun fetchAllData() {
        uiState = ProductsOnlyUiState.Loading
        screenModelScope.launch {
            coroutineScope {
                val productsResult = async { productService.getProducts() }
                val categoriesResult = async { categoryService.getCategories() }
                val unitsResult = async { unitService.getUnits() }

                val products = productsResult.await().getOrElse {
                    uiState = ProductsOnlyUiState.Error("Failed to load products: ${it.message}")
                    return@coroutineScope
                }
                val categories = categoriesResult.await().getOrElse {
                    uiState = ProductsOnlyUiState.Error("Failed to load categories: ${it.message}")
                    return@coroutineScope
                }
                val units = unitsResult.await().getOrElse {
                    uiState = ProductsOnlyUiState.Error("Failed to load units: ${it.message}")
                    return@coroutineScope
                }

                uiState = ProductsOnlyUiState.Success(products, categories, units)
                if (!isEditing) clearSelection()
            }
        }
    }

    private fun suggestNextProductCode(): String {
        return when (val state = uiState) {
            is ProductsOnlyUiState.Success -> {
                val lastCode = state.products.mapNotNull { it.code.toIntOrNull() }.maxOrNull() ?: 0
                (lastCode + 1).toString().padStart(4, '0')
            }
            else -> "0001"
        }
    }
}