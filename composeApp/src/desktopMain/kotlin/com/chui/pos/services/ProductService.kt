package com.chui.pos.services

import com.chui.pos.dtos.ProductRequest
import com.chui.pos.dtos.ProductResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class ProductService(private val httpClient: HttpClient) {

    companion object {
        private const val PRODUCTS_ENDPOINT = "products"
        private val logger = LoggerFactory.getLogger(ProductService::class.java)
    }

    suspend fun getProducts(): Result<List<ProductResponse>> = try {
        Result.success(httpClient.get(PRODUCTS_ENDPOINT).body())
    } catch (e: Exception) {
        logger.error("Failed to fetch products", e)
        Result.failure(e)
    }

    suspend fun createProduct(request: ProductRequest): Result<ProductResponse> = try {
        Result.success(httpClient.post(PRODUCTS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to create product", e)
        Result.failure(e)
    }

    suspend fun updateProduct(id: Int, request: ProductRequest): Result<ProductResponse> = try {
        Result.success(httpClient.put("${PRODUCTS_ENDPOINT}/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to update product with id $id", e)
        Result.failure(e)
    }

    suspend fun deleteProduct(id: Int): Result<Unit> = try {
        httpClient.delete("${PRODUCTS_ENDPOINT}/$id")
        Result.success(Unit)
    } catch (e: Exception) {
        logger.error("Failed to delete product with id $id", e)
        Result.failure(e)
    }


    suspend fun searchProducts(query: String): Result<List<ProductResponse>> = try {
        Result.success(httpClient.get(PRODUCTS_ENDPOINT) {
            url { parameters.append("q", query) }
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to search products with query '$query'", e)
        Result.failure(e)
    }

    suspend fun getProductsByCategory(categoryId: Int): Result<List<ProductResponse>> = try {
        Result.success(httpClient.get(PRODUCTS_ENDPOINT) {
            url { parameters.append("categoryId", categoryId.toString()) }
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to fetch products for category id $categoryId", e)
        Result.failure(e)
    }
}