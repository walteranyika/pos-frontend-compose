package com.chui.pos

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.chui.pos.di.appModule
import com.chui.pos.managers.AuthManager
import com.chui.pos.screens.CategoriesScreen
import com.chui.pos.screens.LoginScreen
import com.chui.pos.screens.ProductsScreen
import com.chui.pos.screens.UnitsScreen
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

fun main() {

    startKoin {
        modules(appModule)
    }

    application {
        var navigator: Navigator? by remember { mutableStateOf(null) }
        val authManager = koinInject<AuthManager>()
        val isLoggedIn = true //by authManager.isLoggedInState.collectAsState()
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)


        Window(
            onCloseRequest = ::exitApplication,
            title = "Point of Sale System",
            state = windowState
        ) {
            if (isLoggedIn) {
                MenuBar {
                    val menuPadding = " ".repeat(20)
                    Menu("System") {

                        Item("Settings", onClick = { /* TODO */ })
                        Item("Backup", onClick = { /* TODO */ })
                        Separator()
                        Item("Logout", onClick = {
                            authManager.clearSession()
                            navigator?.replaceAll(LoginScreen())
                        })
                        Item("Exit", onClick = ::exitApplication)
                    }

                    Menu("Products") {
                        Item("Manage Products",onClick = { navigator?.push(ProductsScreen) })
                        Item("Manage Units", onClick = { navigator?.push(UnitsScreen) })
                        Item("Manage Categories", onClick = { navigator?.push(CategoriesScreen) })
                    }
                }
            }
            MaterialTheme {
                Navigator(LoginScreen()) { nav ->
                    SideEffect { navigator = nav }
                    SlideTransition(nav)
                }
            }
        }
    }
}