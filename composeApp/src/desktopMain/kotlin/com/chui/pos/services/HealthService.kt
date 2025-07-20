package com.chui.pos.services

import com.chui.pos.network.safeApiCallForUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*

private val logger = KotlinLogging.logger {}

class HealthService(private val httpClient: HttpClient) {
    companion object {
        private const val HEALTH_ENDPOINT = "health"
    }

    /**
     * Pings the server's health endpoint.
     * Returns a successful Result if the server responds with 2xx, otherwise a failure.
     */
    suspend fun checkHealth(): Result<Unit> =
        safeApiCallForUnit {
            httpClient.get(HEALTH_ENDPOINT)
        }.onFailure { logger.warn { "Health check failed: ${it.message}" } }
}