package com.chui.pos.di

import com.chui.pos.data.ApiConfig
import com.chui.pos.managers.AuthManager
import com.chui.pos.managers.SettingsManager
import com.chui.pos.services.CategoryService
import com.chui.pos.services.LoginService
import com.chui.pos.services.PrintingService
import com.chui.pos.services.ProductService
import com.chui.pos.services.ReportService
import com.chui.pos.services.SaleService
import com.chui.pos.services.UnitService
import com.chui.pos.viewmodels.CategoryViewModel
import com.chui.pos.viewmodels.LoginViewModel
import com.chui.pos.viewmodels.PosViewModel
import com.chui.pos.viewmodels.ProductViewModel
import com.chui.pos.viewmodels.ReportsViewModel
import com.chui.pos.viewmodels.SettingsViewModel
import com.chui.pos.viewmodels.UnitViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    // Provide the HttpClient as a singleton, which depends on AuthManager
    single {
        val authManager: AuthManager = get()
        val settingsManager: SettingsManager = get()
        HttpClient(CIO) {
            // Configure JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            // This block is applied to every outgoing request
            defaultRequest {
                // Set the base URL from our central config
                url(settingsManager.settings.value.baseUrl)

                // If a token exists, add the Authorization header
                authManager.getToken()?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }

    // Persistence Layer
    single { Settings() }
    single { SettingsManager() }
    single { AuthManager(get()) }

    // Services (now receive HttpClient via constructor)
    single { LoginService(get()) }
    single { ProductService(get()) }
    single { SaleService(get()) }
    single { UnitService(get()) }
    single { CategoryService(get()) }
    single { ProductService(get()) }
    single { ReportService(get()) }
    single { PrintingService(get()) }

    // ViewModels
    factory { LoginViewModel(get(), get()) }
    factory { PosViewModel(get(), get(), get(), get()) }
    factory { UnitViewModel(get() ) }
    factory { ReportsViewModel(get() ) }
    factory { CategoryViewModel(get() ) }
    factory { ProductViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
}