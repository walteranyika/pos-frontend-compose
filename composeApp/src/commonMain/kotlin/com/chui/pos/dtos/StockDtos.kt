package com.chui.pos.dtos


import kotlinx.serialization.Serializable

@Serializable
data class StockAdjustmentRequest(
    val productId: Int,
    val newQuantity: Double
)