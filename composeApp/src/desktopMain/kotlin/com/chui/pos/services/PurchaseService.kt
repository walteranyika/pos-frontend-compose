package com.chui.pos.services


import com.chui.pos.dtos.PurchaseRequest
import com.chui.pos.dtos.PurchaseResponse
import com.chui.pos.network.safeApiCall
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

private val logger = KotlinLogging.logger {}

class PurchaseService(private val httpClient: HttpClient) {
    companion object {
        private const val PURCHASES_ENDPOINT = "purchases"
    }

    suspend fun getAllPurchases(): Result<List<PurchaseResponse>> =
        safeApiCall<List<PurchaseResponse>> {
            httpClient.get(PURCHASES_ENDPOINT)
        }.onFailure { logger.error(it) { "Failed to fetch purchases" } }

    suspend fun createPurchase(request: PurchaseRequest): Result<PurchaseResponse> =
        safeApiCall<PurchaseResponse> {
            httpClient.post(PURCHASES_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to create purchase" } }
}