package com.chui.pos.services

import com.chui.pos.dtos.CreateSaleRequest
import com.chui.pos.events.AppEventBus
import com.chui.pos.network.safeApiCallForUnit
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class SaleService(private val httpClient: HttpClient,  private val eventBus: AppEventBus) {
    companion object {
        private const val SALES_ENDPOINT = "sales"
        private val logger = LoggerFactory.getLogger(SaleService::class.java)
    }

    suspend fun createSale(request: CreateSaleRequest): Result<Unit> =
        safeApiCallForUnit(eventBus) {
            httpClient.post(SALES_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error("Failed to create sale: ${it.message}", it) }
}