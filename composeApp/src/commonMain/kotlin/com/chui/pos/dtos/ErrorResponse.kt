package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
    val status: Int,
)