package com.chui.pos.services

import com.chui.pos.dtos.PagedResponse
import com.chui.pos.dtos.ReorderItemResponse
import com.chui.pos.dtos.SaleSummaryResponse
import com.chui.pos.network.safeApiCall
import io.ktor.client.*
import io.ktor.client.request.*
import org.slf4j.LoggerFactory
import java.time.LocalDate


class ReportService(private val httpClient: HttpClient) {
    companion object{
        private const val  REPORTS_ENDPOINT = "services/sales/recent"
        private const val  STOCK_LEVEL_ENDPOINT = "services/reorder-alerts"
        private val logger = LoggerFactory.getLogger(ReportService::class.java)
    }

    suspend fun getRecentSales(
        query: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        page: Int = 0,
        size: Int = 15
    ): Result<PagedResponse<SaleSummaryResponse>> =
        safeApiCall<PagedResponse<SaleSummaryResponse>> {
            httpClient.get(REPORTS_ENDPOINT) {
                url {
                    parameters.append("page", page.toString())
                    parameters.append("size", size.toString())
                    query?.takeIf { it.isNotBlank() }?.let { parameters.append("q", it) }
                    startDate?.let { parameters.append("startDate", it.toString()) }
                    endDate?.let { parameters.append("endDate", it.toString()) }
                }
            }
        }.onFailure { logger.error("Failed to fetch recent sales", it) }


    suspend fun getReorderAlerts(): Result<List<ReorderItemResponse>> = safeApiCall<List<ReorderItemResponse>> {
        httpClient.get(STOCK_LEVEL_ENDPOINT)
    }.onFailure { logger.error("Failed to fetch stock level alerts", it) }
}