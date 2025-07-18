package com.chui.pos.services

import com.chui.pos.dtos.ProductRequest
import com.chui.pos.dtos.ProductResponse
import com.chui.pos.network.safeApiCall
import com.chui.pos.network.safeApiCallForUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class ProductService(private val httpClient: HttpClient) {

    companion object {
        private const val PRODUCTS_ENDPOINT = "products"
        private val logger = KotlinLogging.logger { }
    }

    suspend fun getProducts(): Result<List<ProductResponse>> =
        safeApiCall<List<ProductResponse>> {
            httpClient.get(PRODUCTS_ENDPOINT)
        }.onFailure { logger.error(it) { "Failed to fetch products" } }

    suspend fun createProduct(request: ProductRequest): Result<ProductResponse> =
        safeApiCall<ProductResponse> {
            httpClient.post(PRODUCTS_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to create product" } }

    suspend fun updateProduct(id: Int, request: ProductRequest): Result<ProductResponse> =
        safeApiCall<ProductResponse> {
            httpClient.put("${PRODUCTS_ENDPOINT}/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error(it) { "Failed to update product with id $id" } }

    suspend fun deleteProduct(id: Int): Result<Unit> =
        safeApiCallForUnit { httpClient.delete("${PRODUCTS_ENDPOINT}/$id") }
            .onFailure { logger.error(it) { "Failed to delete product with id $id" } }


    suspend fun searchProducts(query: String): Result<List<ProductResponse>> =
        safeApiCall<List<ProductResponse>> {
            httpClient.get(PRODUCTS_ENDPOINT) {
                url { parameters.append("q", query) }
            }
        }.onFailure { logger.error(it) { "Failed to search products with query '$query'" }  }


    suspend fun getProductsByCategory(categoryId: Int): Result<List<ProductResponse>> =
        safeApiCall<List<ProductResponse>>{
            httpClient.get(PRODUCTS_ENDPOINT){ url {parameters.append("categoryId", categoryId.toString())}}
        }.onFailure { logger.error(it){ "Failed to search products with query '$categoryId'" }
        }
}