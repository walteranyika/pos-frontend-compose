package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UnitResponse(
    val id: Int,
    val name: String,
)

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
)

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val code: String,
    val price: Double,
    val category: CategoryResponse,
    val saleUnit: UnitResponse,
)

// This class will represent an item within our cart state
data class CartItem(
    val productId: Int,
    val name: String,
    val price: Double,
    var quantity: Int
) {
    val total: Double
        get() = price * quantity
}