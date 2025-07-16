package com.chui.pos.services

import com.chui.pos.dtos.CreateSaleRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class SaleService(private val httpClient: HttpClient) {
    companion object {
        private const val SALES_ENDPOINT = "sales"
        private val logger = LoggerFactory.getLogger(SaleService::class.java)
    }

    suspend fun createSale(request: CreateSaleRequest): Result<Unit> {
        return try {
            httpClient.post(SALES_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to create sale: ${e.message}", e)
            Result.failure(e)
        }
    }
}