package com.chui.pos.network

import com.chui.pos.dtos.ErrorResponse
import com.chui.pos.events.AppEvent
import com.chui.pos.events.AppEventBus
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import java.net.ConnectException


suspend inline fun <reified S> safeApiCall(eventBus: AppEventBus, crossinline apiCall: suspend () -> HttpResponse) : Result<S>{
    return try {
        val response = apiCall()
        if (response.status.isSuccess()){
            Result.success(response.body<S>())
        }else{
            // 1. Handle session expiration first and return immediately.
            if(response.status.value == 401 || response.status.value == 403){
                eventBus.sendEvent(AppEvent.TokenExpired)
                // We return a failure here. The event bus will show the main dialog,
                // and this message will be available to the caller's .onFailure block.
                return Result.failure(Exception("Your session has expired. Please login again."))
            }

            // 2. Handle all other server errors.
            // This code now only runs for errors that are NOT 401 or 403.
            val errorMessage = try {
                response.body<ErrorResponse>().message
            } catch (e: Exception) {
                println("Could not parse error response body. Status: ${response.status}. Error: ${e.message}")
                "An unexpected error occurred (Status: ${response.status.value}). Please try again."
            }
            Result.failure(Exception(errorMessage))
        }
    }catch (e: Exception){
        Result.failure(e.toUserFriendlyException())
    }
}

suspend inline fun safeApiCallForUnit(eventBus: AppEventBus, crossinline apiCall: suspend () -> HttpResponse): Result<Unit> {
    return safeApiCall(eventBus, apiCall)
}

fun Exception.toUserFriendlyException(): Exception{
    return when(this){
        is UnresolvedAddressException, is ConnectException -> Exception("Cannot connect to the server. Please check the settings and network connection")
        is SerializationException ->
            Exception("An error occured while processing the server response")
        else -> this
    }
}