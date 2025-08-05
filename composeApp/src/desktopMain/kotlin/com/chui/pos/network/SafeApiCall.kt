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
            if(response.status.value == 401 || response.status.value == 403){
                eventBus.sendEvent(AppEvent.TokenExpired)
            }
            val errorMessage = try {
                // Attempt to parse the server's specific error message
                response.body<ErrorResponse>().message
            } catch (e: Exception) {
                println("Could not parse error response body. Status: ${response.status}. Error: ${e.message}")
                if(listOf(401, 403).contains(response.status.value)){
                    "Your session has expired. Please login again."
                }else {
                    "An unexpected error occurred Status: ${response.status.value}. Please try again."
                }
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



/*
suspend inline fun safeApiCallForUnit(eventBus: AppEventBus,crossinline apiCall: suspend () -> HttpResponse): Result<Unit>{
    return try {
        val response = apiCall()
        if (response.status.isSuccess()){
            Result.success(Unit)
        }else{
            val errResponse = response.body<ErrorResponse>()
            if(response.status.value == 401){
                eventBus.sendEvent(AppEvent.TokenExpired)
            }
            Result.failure(Exception(errResponse.message))
        }
    }catch (e: Exception){
        Result.failure(e.toUserFriendlyException())
    }
}
*/



fun Exception.toUserFriendlyException(): Exception{
    return when(this){
        is UnresolvedAddressException, is ConnectException -> Exception("Cannot connect to the server. Please check the settings and network connection")
        is SerializationException ->
            Exception("An error occured while processing the server response")
        else -> this
    }
}