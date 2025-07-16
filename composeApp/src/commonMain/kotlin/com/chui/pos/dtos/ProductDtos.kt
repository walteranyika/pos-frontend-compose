package com.chui.pos.dtos

import kotlinx.serialization.Serializable

// This enum should match what your backend expects
@Serializable
enum class TaxType {
    INCLUSIVE, EXCLUSIVE, NONE
}

@Serializable
data class ProductResponse(
    val id: Int,
    val code: String,
    val name: String,
    val barcode: String?,
    val cost: Double,
    val price: Double,
    val isVariablePriced: Boolean,
    val saleUnit: ProductUnitResponse,
    val purchaseUnit: ProductUnitResponse,
    val stockAlert: Double,
    val category: CategoryResponse,
    val taxMethod: TaxType,
    val image: String?,
    val isActive: Boolean,
    val note: String?
)

@Serializable
data class ProductRequest(
    val name: String,
    val code: String,
    val barcode: String?,
    val cost: Double,
    val price: Double,
    val categoryId: Int,
    val saleUnitId: Int,
    val purchaseUnitId: Int,
    val stockAlert: Double,
    val taxMethod: TaxType,
    val isVariablePriced: Boolean,
    val isActive: Boolean,
    val note: String?
)

data class CartItem(
    val productId: Int,
    val name: String,
    val price: Double,
    var quantity: Int
) {
    val total: Double
        get() = price * quantity
}