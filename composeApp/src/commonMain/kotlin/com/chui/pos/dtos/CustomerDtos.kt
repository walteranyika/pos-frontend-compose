package com.chui.pos.dtos

import kotlinx.serialization.Serializable

/**
 * Represents a customer in API responses.
 */
@Serializable
data class CustomerResponse(
    val id: Long,
    val name: String,
    val phoneNumber: String?
)

/**
 * Represents the payload for creating a new customer.
 */
@Serializable
data class CreateCustomerRequest(
    val name: String,
    val phoneNumber: String?
)