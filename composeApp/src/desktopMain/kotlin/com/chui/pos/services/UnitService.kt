package com.chui.pos.services

import com.chui.pos.dtos.ProductUnitRequest
import com.chui.pos.dtos.ProductUnitResponse
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

class UnitService(private val httpClient: HttpClient) {
    companion object {
        private const val UNITS_ENDPOINT = "units"
        private val logger = LoggerFactory.getLogger(UnitService::class.java)
    }

    suspend fun getUnits(): Result<List<ProductUnitResponse>> =
        safeApiCall<List<ProductUnitResponse>> {
            httpClient.get(UNITS_ENDPOINT)
        }.onFailure { logger.error("Failed to fetch product units", it) }


    suspend fun createUnit(request: ProductUnitRequest): Result<ProductUnitResponse> =
        safeApiCall<ProductUnitResponse> {
            httpClient.post(UNITS_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error("Failed to create unit", it) }

    suspend fun updateUnit(id: Int, request: ProductUnitRequest): Result<ProductUnitResponse> =
        safeApiCall<ProductUnitResponse> {
            httpClient.put("$UNITS_ENDPOINT/$id"){
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.onFailure { logger.error("Failed to update unit with id $id", it) }

    suspend fun deleteUnit(id: Int): Result<Unit> =
        safeApiCall<Unit> {
            httpClient.delete("$UNITS_ENDPOINT/$id")
        }.onFailure { logger.error("Failed to delete unit with id $id", it) }


    suspend fun searchUnits(query: String): Result<List<ProductUnitResponse>> =
        safeApiCall<List<ProductUnitResponse>> {
            httpClient.get(UNITS_ENDPOINT) {
                url { parameters.append("q", query) }
            }
        }.onFailure { logger.error("Failed to search units with query '$query'", it) }
}