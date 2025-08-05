package com.chui.pos.services

import com.chui.pos.dtos.LoginRequest
import com.chui.pos.dtos.LoginResponse
import com.chui.pos.events.AppEventBus
import com.chui.pos.network.safeApiCall
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class LoginService(private val httpClient: HttpClient,  private val eventBus: AppEventBus) {
    companion object {
        private const val LOGIN_ENDPOINT = "auth/login"
        private const val DEFAULT_USERNAME = "admin"
        private val logger = KotlinLogging.logger {  }
    }

    suspend fun login(pin: String): Result<LoginResponse> {
        val body = LoginRequest(pin = pin, username = DEFAULT_USERNAME)
        return safeApiCall<LoginResponse>(eventBus) {
            httpClient.post(LOGIN_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }.onFailure { logger.error(it){"Login failed for user $DEFAULT_USERNAME"}  }
    }
}
