package com.chui.pos.services

import com.chui.pos.dtos.PagedResponse
import com.chui.pos.dtos.SaleSummaryResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.slf4j.LoggerFactory
import java.time.LocalDate



class ReportService(private val httpClient: HttpClient) {
    companion object{
        private const val  REPORTS_ENDPOINT = "services/sales/recent"
        private val logger = LoggerFactory.getLogger(ReportService::class.java)
    }

    suspend fun getRecentSales(
        query: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        page: Int = 0,
        size: Int = 15
    ): Result<PagedResponse<SaleSummaryResponse>> = try {
        Result.success(httpClient.get(REPORTS_ENDPOINT) {
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                query?.takeIf { it.isNotBlank() }?.let { parameters.append("q", it) }
                startDate?.let { parameters.append("startDate", it.toString()) }
                endDate?.let { parameters.append("endDate", it.toString()) }
            }
        }.body())
    } catch (e: Exception) {
        logger.error("Failed to fetch recent sales", e)
        Result.failure(e)
    }
}