package com.chui.pos.dtos


import kotlinx.serialization.Serializable

// Using a custom serializable wrapper for date/time
@Serializable
data class PurchaseRequest(
    val supplier: String?,
    val items: List<PurchaseItemRequest>
)

@Serializable
data class PurchaseItemRequest(
    val productId: Int,
    val quantity: Double,
    val costPrice: Double,
    val name: String?=null,
    val code: String? =null
)

@Serializable
data class PurchaseResponse(
    val id: Int,
    val ref: String,
    val supplier: String?,
    val totalCost: Double,
    val purchaseDate: String?, // Using String for simplicity, can be parsed later
    val items: List<PurchaseItemResponse>
)

@Serializable
data class PurchaseItemResponse(
    val productName: String,
    val quantity: Double,
    val costPrice: Double,
    val totalCost: Double
)