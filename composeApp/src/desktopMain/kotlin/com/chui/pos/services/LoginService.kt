package com.chui.pos.services

import com.chui.pos.dtos.ErrorResponse
import com.chui.pos.dtos.LoginRequest
import com.chui.pos.dtos.LoginResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
            val response: HttpResponse = httpClient.post(LOGIN_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<LoginResponse>())
            }else{
                val errorResponse = response.body<ErrorResponse>()
                logger.error("Login failed for user: $DEFAULT_USERNAME, Server says ${errorResponse.message} Code ${errorResponse.status}")
                Result.failure(Exception(errorResponse.message))
            }

        } catch (e: Exception){
            logger.error("Login failed for user: $DEFAULT_USERNAME", e)
            Result.failure(Exception("Cannot connect to the server. Please check network connection"))
        }
    }
}
