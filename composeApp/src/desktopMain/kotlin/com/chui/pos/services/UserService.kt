package com.chui.pos.services

import com.chui.pos.dtos.*
import com.chui.pos.network.safeApiCall
import com.chui.pos.network.safeApiCallForUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

private val logger = KotlinLogging.logger {}

class UserService(private val httpClient: HttpClient) {
    companion object {
        private const val USERS_ENDPOINT = "users"
        private const val ROLES_ENDPOINT = "roles"
    }

    suspend fun getUsers(): Result<List<UserResponse>> = safeApiCall {
        httpClient.get(USERS_ENDPOINT)
    }

    suspend fun getRoles(): Result<List<RoleResponse>> = safeApiCall {
        httpClient.get(ROLES_ENDPOINT)
    }

    suspend fun updateUser(userId: Long, request: UpdateUserRequest): Result<UserResponse> = safeApiCall {
        httpClient.put("$USERS_ENDPOINT/$userId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun assignRolesToUser(userId: Long, request: AssignRolesRequest): Result<UserResponse> = safeApiCall {
        httpClient.post("$USERS_ENDPOINT/$userId/roles") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun resetUserPin(userId: Long, request: ResetPinRequest): Result<Unit> = safeApiCallForUnit {
        httpClient.post("$USERS_ENDPOINT/$userId/reset-pin") { // Assuming this endpoint exists
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
