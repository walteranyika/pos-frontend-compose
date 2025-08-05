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



fun Exception.toUserFriendlyException(): Exception{
    return when(this){
        is UnresolvedAddressException, is ConnectException -> Exception("Cannot connect to the server. Please check the settings and network connection")
        is SerializationException ->
            Exception("An error occured while processing the server response")
        else -> this
    }
}