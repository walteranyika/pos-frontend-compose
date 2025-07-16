package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val code: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CategoryRequest(
    val name: String,
    val code: String
)