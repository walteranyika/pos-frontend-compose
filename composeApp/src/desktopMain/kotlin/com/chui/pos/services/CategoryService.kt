package com.chui.pos.services

import com.chui.pos.dtos.CategoryRequest
import com.chui.pos.dtos.CategoryResponse
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

    suspend fun getCategories(): Result<List<CategoryResponse>> = try {
        Result.success(httpClient.get(CATEGORIES_ENDPOINT).body())
    } catch (e: Exception) {
        logger.error("Failed to fetch categories", e)
        Result.failure(e)
    }

    suspend fun createCategory(request: CategoryRequest): Result<CategoryResponse> = try {
        Result.success(httpClient.post(CATEGORIES_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to create category", e)
        Result.failure(e)
    }

    suspend fun updateCategory(id: Int, request: CategoryRequest): Result<CategoryResponse> = try {
        Result.success(httpClient.put("$CATEGORIES_ENDPOINT/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to update category with id $id", e)
        Result.failure(e)
    }

    suspend fun deleteCategory(id: Int): Result<Unit> = try {
        httpClient.delete("$CATEGORIES_ENDPOINT/$id")
        Result.success(Unit)
    } catch (e: Exception) {
        logger.error("Failed to delete category with id $id", e)
        Result.failure(e)
    }
}