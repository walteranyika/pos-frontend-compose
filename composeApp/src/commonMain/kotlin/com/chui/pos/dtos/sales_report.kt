package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SaleSummaryResponse(
    val id: Long,
    val ref: String,
    val grandTotal: Double,
    val discount: Double,
    val paidAmount: Double,
    val paymentStatus: String,
    val isCreditSale: Boolean,
    val customer: Customer,
    val cashier: String,
    val saleDate: String, // Keep as String for deserialization, parse in UI
    val items: List<SaleItemDetail>,
    val payments: List<SalePaymentDetail>
)

@Serializable
data class SaleItemDetail(
    val productName: String,
    val quantity: Double,
    val price: Double,
    val discount: Double,
    val total: Double
)

@Serializable
data class SalePaymentDetail(
    val amount: Double,
    val method: String,
    val paidAt: String
)
@Serializable
data class PagedResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val number: Int, // current page number
    val size: Int, // page size
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

@Serializable
data class Customer(
    val id: Long,
    val name: String,
    val phoneNumber: String?=null
)