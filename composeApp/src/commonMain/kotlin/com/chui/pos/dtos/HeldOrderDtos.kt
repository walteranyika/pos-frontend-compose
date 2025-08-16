package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class HoldOrderRequest(
    val items: List<HoldOrderItemRequest>,
    val customerId: Long
)

@Serializable
data class HoldOrderItemRequest(
    val productId: Int,
    val quantity: Double
)

@Serializable
data class HeldOrderResponse(
    val id: Long,
    val ref: String,
    val items: List<HeldOrderItemResponse>,
    val customerId: Long,
    val customerName: String,
    // Using String for simplicity, can be parsed with a custom serializer if needed
    val createdAt: String? = null
)

@Serializable
data class HeldOrderItemResponse(
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val price: Double,
    var isVariablePriced : Boolean= false
)