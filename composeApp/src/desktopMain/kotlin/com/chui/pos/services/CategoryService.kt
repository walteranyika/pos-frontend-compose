package com.chui.pos.services

import com.chui.pos.dtos.CategoryRequest
import com.chui.pos.dtos.CategoryResponse
import com.chui.pos.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class CategoryService(private val httpClient: HttpClient) {

    companion object {
        private const val CATEGORIES_ENDPOINT = "categories"
        private val logger = LoggerFactory.getLogger(CategoryService::class.java)
    }

    suspend fun getCategories(): Result<List<CategoryResponse>> =
        safeApiCall<List<CategoryResponse>> {
            httpClient.get(CATEGORIES_ENDPOINT)
        }.onFailure { logger.error("Failed to fetch categories", it) }


    suspend fun createCategory(request: CategoryRequest): Result<CategoryResponse> =
        safeApiCall<CategoryResponse> {
            httpClient.post(CATEGORIES_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error("Failed to create category", it) }

    suspend fun updateCategory(id: Int, request: CategoryRequest): Result<CategoryResponse> =
        safeApiCall<CategoryResponse> {
            httpClient.put("$CATEGORIES_ENDPOINT/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error("Failed to update category with id $id", it) }

    suspend fun deleteCategory(id: Int): Result<Unit> =
        safeApiCall<Unit> {
            httpClient.delete("$CATEGORIES_ENDPOINT/$id")
        }.onFailure { logger.error("Failed to delete category with id $id", it) }


    suspend fun searchCategories(query: String): Result<List<CategoryResponse>> =
        safeApiCall<List<CategoryResponse>> {
            httpClient.get(CATEGORIES_ENDPOINT) {
                url { parameters.append("query", query) }
            }
        }.onFailure { logger.error("Failed to search categories with query $query", it) }
}