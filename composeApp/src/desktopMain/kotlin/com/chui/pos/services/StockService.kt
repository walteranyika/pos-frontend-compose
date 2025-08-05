package com.chui.pos.services

import com.chui.pos.dtos.StockAdjustmentRequest
import com.chui.pos.events.AppEventBus
import com.chui.pos.network.safeApiCallForUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

private val logger = KotlinLogging.logger {}

class StockService(private val httpClient: HttpClient,  private val eventBus: AppEventBus) {
    companion object {
        private const val STOCK_ENDPOINT = "stock"
    }

    suspend fun adjustStock(request: StockAdjustmentRequest): Result<Unit> =
        safeApiCallForUnit(eventBus) {
            httpClient.post("$STOCK_ENDPOINT/adjust") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to adjust stock for product ID ${request.productId}" } }
}