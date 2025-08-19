package com.chui.pos.di

import com.chui.pos.events.AppEventBus
import com.chui.pos.managers.AuthManager
import com.chui.pos.services.*
import com.chui.pos.viewmodels.*
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    try {
        // Provide the HttpClient as a singleton, which depends on AuthManager
        single {
            val authManager: AuthManager = get()
            val settingsManager: SettingsService = get()
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
                    url(settingsManager.loadBaseUrl())

                    // If a token exists, add the Authorization header
                    authManager.getToken()?.let { token ->
                        header("Authorization", "Bearer $token")
                    }
                }
            }
        }

        // Persistence Layer
        single { Settings() }

        single { AuthManager(get()) }

        single { AppEventBus() }

        // Services (now receive HttpClient via constructor)
        single { LoginService(get(), get()) }
        single { ProductService(get(), get()) }
        single { SaleService(get(), get()) }
        single { UnitService(get(), get()) }
        single { CategoryService(get(), get()) }
        single { ReportService(get(), get()) }
        single { PrintingService(get()) }
        single { StockService(get(), get()) }
        single { PurchaseService(get(), get()) }
        single { HealthService(get(), get()) }
        single { ServerStatusViewModel(get()) }
        single { HeldOrderService(get(), get()) }
        single { SoundService() }
        single { UserService(get(), get()) }
        single { CustomerService(get(), get()) }
        single { SettingsService() }



        // ViewModels
        factory { LoginViewModel(get(), get()) }
        factory { PosViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
        factory { UnitViewModel(get() ) }
        factory { ReportsViewModel(get() ) }
        factory { CategoryViewModel(get() ) }
        factory { ProductViewModel(get(), get(), get()) }
        factory { UserViewModel(get()) }
        factory { SettingsViewModel(get(), get()) }
        factory { StockViewModel(get(), get()) }
        factory { PurchaseViewModel(get(), get()) }
        factory { ReorderViewModel(get()) }

    } catch (e: Exception) {
        TODO("Not yet implemented")
    } finally {
    }
}