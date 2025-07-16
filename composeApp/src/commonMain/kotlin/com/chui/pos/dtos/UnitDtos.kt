package com.chui.pos.dtos


import kotlinx.serialization.Serializable

@Serializable
data class ProductUnitResponse(
    val id: Int,
    val name: String,
    val shortName: String,
    val createdAt: String? = null, // Using String for simplicity with kotlinx.serialization
    val updatedAt: String? = null
)

@Serializable
data class ProductUnitRequest(
    val name: String,
    val shortName: String
)