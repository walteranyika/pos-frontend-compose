package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SaleItemRequest(
    val productId: Int,
    val quantity: Double,
    val price: Double,
    val discount: Double = 0.0
)

@Serializable
enum class PaymentMethod {
    CASH, MPESA, CARD, CREDIT, COMPLIMENTARY
}

@Serializable
data class PaymentRequest(
    val amount: Double,
    val method: PaymentMethod,
    val notes: String? = null
)

@Serializable
data class CreateSaleRequest(
    val items: List<SaleItemRequest>,
    val payments: List<PaymentRequest>,
    val discount: Double = 0.0,
    val isCreditSale: Boolean = false
)