package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val pin: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val fullName: String,
    val username: String,
    val permissions: Set<String>
)