package com.chui.pos.services

import com.chui.pos.dtos.HeldOrderResponse
import com.chui.pos.dtos.HoldOrderRequest
import com.chui.pos.network.safeApiCall
import com.chui.pos.network.safeApiCallForUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

private val logger = KotlinLogging.logger {}

class HeldOrderService(private val httpClient: HttpClient) {
    companion object {
        private const val HELD_ORDERS_ENDPOINT = "held-orders"
    }

    suspend fun getHeldOrders(): Result<List<HeldOrderResponse>> =
        safeApiCall<List<HeldOrderResponse>> {
            httpClient.get(HELD_ORDERS_ENDPOINT)
        }.onFailure { logger.error(it) { "Failed to fetch held orders" } }

    suspend fun holdOrder(request: HoldOrderRequest): Result<HeldOrderResponse> =
        safeApiCall<HeldOrderResponse> {
            httpClient.post(HELD_ORDERS_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to hold order" } }

    suspend fun updateHeldOrder(id: Long, request: HoldOrderRequest): Result<HeldOrderResponse> =
        safeApiCall<HeldOrderResponse> {
            httpClient.put("$HELD_ORDERS_ENDPOINT/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to update held order with id $id" } }

    suspend fun deleteHeldOrder(id: Long): Result<Unit> =
        safeApiCallForUnit {
            httpClient.delete("$HELD_ORDERS_ENDPOINT/$id")
        }.onFailure { logger.error(it) { "Failed to delete held order with id $id" } }
}