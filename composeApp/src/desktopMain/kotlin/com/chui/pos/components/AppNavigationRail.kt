package com.chui.pos.components



import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.ApplicationScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.chui.pos.managers.AuthManager
import com.chui.pos.screens.*

@Composable
fun AppNavigationRail(
    navigator: Navigator,
    authManager: AuthManager,
    applicationScope: ApplicationScope
) {
    var showMenu by remember { mutableStateOf(false) }

    val primaryDestinations = listOf(
        RailItem("POS", Icons.Default.ShoppingCart, PosScreen),
        RailItem("Reports", Icons.Default.Assessment, ReportsScreen)
    )

    val secondaryDestinations = listOf(
        RailItem("Products", Icons.Default.Inventory, ProductsScreen),
        RailItem("Categories", Icons.Default.Category, CategoriesScreen),
        RailItem("Units", Icons.Default.SquareFoot, UnitsScreen)
    )

    NavigationRail {
        // Header with dropdown menu for secondary actions
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(Icons.Default.Menu, contentDescription = "More Options")
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            secondaryDestinations.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { Icon(item.icon, contentDescription = item.label) },
                    onClick = {
                        navigator.push(item.screen)
                        showMenu = false
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = { Text("Logout") },
                leadingIcon = { Icon(Icons.Default.Logout, "Logout") },
                onClick = {
                    authManager.clearSession()
                    navigator.replaceAll(LoginScreen)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Exit") },
                leadingIcon = { Icon(Icons.Default.ExitToApp, "Exit") },
                onClick = { applicationScope.exitApplication() }
            )
        }

        Spacer(Modifier.weight(1f))

        // Primary destinations are placed at the bottom for easy access
        primaryDestinations.forEach { item ->
            NavigationRailItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = navigator.lastItem::class == item.screen::class,
                onClick = { navigator.replaceAll(item.screen) }
            )
        }
    }
}

private data class RailItem(val label: String, val icon: ImageVector, val screen: Screen)