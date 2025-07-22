package com.chui.pos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.chui.pos.components.AppDrawerContent
import com.chui.pos.components.ServerStatusIndicator
import com.chui.pos.di.appModule
import com.chui.pos.managers.AuthManager
import com.chui.pos.screens.LoginScreen
import com.chui.pos.viewmodels.ServerStatusViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

@OptIn(ExperimentalMaterial3Api::class)
fun main() {

    startKoin {
        modules(appModule)
    }

    application {
        var navigator: Navigator? by remember { mutableStateOf(null) }
        val authManager = koinInject<AuthManager>()
        val serverStatusViewModel = koinInject<ServerStatusViewModel>()
        val isLoggedIn by authManager.isLoggedInState.collectAsState()
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val isServerOnline by serverStatusViewModel.isServerOnline.collectAsState()
        val user by remember { mutableStateOf(authManager.getUserFullName()) }
//    if (authManager.hasPermission("MANAGE_USERS")) {
        Window(
            onCloseRequest = {
                scope.launch {
                    authManager.clearSession()
                    exitApplication()
                }
            },
            title = "Point of Sale System",
            state = windowState
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Navigator(LoginScreen) { nav ->
                        SideEffect { navigator = nav }
                        if (isLoggedIn) {
                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    AppDrawerContent(
                                        navigator = nav,
                                        authManager = authManager,
                                        applicationScope = this@application,
                                        closeDrawer = { scope.launch { drawerState.close() } }
                                    )
                                }
                            ) {
                                Scaffold(
                                    topBar = {
                                        TopAppBar(
                                            title = { Text(nav.lastItem.key.substringAfterLast('.').replace("Screen", "")) },
                                            navigationIcon = {
                                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                                }
                                            }
                                        )
                                    },
                                    bottomBar = {
                                        BottomAppBar {
                                            ServerStatusIndicator(isServerOnline, user)
                                        }
                                    }
                                ) { padding ->
                                    Box(modifier = Modifier.padding(padding)) {
                                        SlideTransition(nav)
                                    }
                                }
                            }
                        } else {
                            // When not logged in, just show the login screen directly
                            SlideTransition(nav)
                        }
                    }
                }
            }
        }
    }
}