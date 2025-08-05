package com.chui.pos.services

import com.chui.pos.dtos.CreateCustomerRequest
import com.chui.pos.dtos.CustomerResponse
import com.chui.pos.events.AppEventBus
import com.chui.pos.network.safeApiCall
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class CustomerService(private val httpClient: HttpClient, private val eventBus: AppEventBus) {
    companion object {
        private const val CUSTOMERS_ENDPOINT = "customers"
    }

    /**
     * Fetches a list of all customers from the backend.
     */
    suspend fun getCustomers(): Result<List<CustomerResponse>> = safeApiCall(eventBus) {
        httpClient.get(CUSTOMERS_ENDPOINT)
    }

    /**
     * Creates a new customer.
     */
    suspend fun createCustomer(request: CreateCustomerRequest): Result<CustomerResponse> = safeApiCall(eventBus) {
        httpClient.post(CUSTOMERS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}