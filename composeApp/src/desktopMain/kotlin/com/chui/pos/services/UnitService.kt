package com.chui.pos.services

import com.chui.pos.dtos.ProductUnitRequest
import com.chui.pos.dtos.ProductUnitResponse
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

class UnitService(private val httpClient: HttpClient) {
    companion object {
        private const val UNITS_ENDPOINT = "units"
        private val logger = LoggerFactory.getLogger(UnitService::class.java)
    }

    suspend fun getUnits(): Result<List<ProductUnitResponse>> = try {
        Result.success(httpClient.get(UNITS_ENDPOINT).body())
    } catch (e: Exception) {
        logger.error("Failed to fetch units", e)
        Result.failure(e)
    }

    suspend fun createUnit(request: ProductUnitRequest): Result<ProductUnitResponse> = try {
        Result.success(httpClient.post(UNITS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to create unit", e)
        Result.failure(e)
    }

    suspend fun updateUnit(id: Int, request: ProductUnitRequest): Result<ProductUnitResponse> = try {
        Result.success(httpClient.put("$UNITS_ENDPOINT/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to update unit with id $id", e)
        Result.failure(e)
    }

    suspend fun deleteUnit(id: Int): Result<Unit> = try {
        httpClient.delete("$UNITS_ENDPOINT/$id")
        Result.success(Unit)
    } catch (e: Exception) {
        logger.error("Failed to delete unit with id $id", e)
        Result.failure(e)
    }
}