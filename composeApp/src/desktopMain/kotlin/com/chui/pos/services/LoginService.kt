package com.chui.pos.services

import com.chui.pos.dtos.LoginRequest
import com.chui.pos.dtos.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class LoginService(private val httpClient: HttpClient) {
    companion object {
        private const val LOGIN_ENDPOINT = "auth/login"
        private const val DEFAULT_USERNAME = "cashier01"
        private val logger = LoggerFactory.getLogger(LoginService::class.java)
    }

    suspend fun login(pin: String): Result<LoginResponse> {
        val body = LoginRequest(pin = pin, username = DEFAULT_USERNAME)
        return try {
            val response: LoginResponse = httpClient.post(LOGIN_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            logger.error("Login failed for user: $DEFAULT_USERNAME", e)
            Result.failure(e)
        }
    }
}
