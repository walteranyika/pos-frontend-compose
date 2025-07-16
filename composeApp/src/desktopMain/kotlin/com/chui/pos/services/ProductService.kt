package com.chui.pos.services

import com.chui.pos.dtos.ProductResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.slf4j.LoggerFactory

class ProductService(private val httpClient: HttpClient) {
    companion object {
        // The base URL is now configured in the Ktor client via DI. We only need the endpoint path.
        private const val PRODUCTS_ENDPOINT = "products"
        private val logger = LoggerFactory.getLogger(ProductService::class.java)
    }

    suspend fun getProducts(): Result<List<ProductResponse>> {
        return try {
            // The Authorization header will be added automatically by the Ktor client plugin
            Result.success(httpClient.get(PRODUCTS_ENDPOINT).body())
        } catch (e: Exception) {
            logger.error("Failed to fetch products", e)
            Result.failure(e)
        }
    }
}