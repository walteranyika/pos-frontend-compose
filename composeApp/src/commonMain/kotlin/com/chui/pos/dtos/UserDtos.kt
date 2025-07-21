package com.chui.pos.dtos

import kotlinx.serialization.Serializable

// --- DTOs for API Responses ---

@Serializable
data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String?,
    val roles: Set<String>
)

@Serializable
data class RoleResponse(
    val id: Long,
    val name: String,
    val permissions: Set<String>
)


// --- DTOs for API Requests ---

@Serializable
data class UpdateUserRequest(
    val username: String,
    val fullName: String?
)

@Serializable
data class AssignRolesRequest(
    val roleIds: Set<Long>
)

@Serializable
data class ResetPinRequest(
    val newPin: String
)