package com.chui.pos.dtos


import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class StockAdjustmentRequest(
    val productId: Int,
    val newQuantity: Double
)


@Serializable
data class ReorderItemResponse(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val currentQuantity: Double,
    val stockAlertLevel: Double,
    val saleUnitName: String
)