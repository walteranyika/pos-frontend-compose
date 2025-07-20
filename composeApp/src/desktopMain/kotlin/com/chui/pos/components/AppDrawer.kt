package com.chui.pos.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.chui.pos.dtos.PermissionEnum
import com.chui.pos.managers.AuthManager
import com.chui.pos.screens.*
import org.koin.compose.koinInject

@Composable
fun AppDrawerContent(
    navigator: Navigator,
    authManager: AuthManager,
    applicationScope: ApplicationScope,
    closeDrawer: () -> Unit
) {
    val authManager: AuthManager = koinInject() // Or receive as a parameter
    val userPermissions = authManager.getUserPermissions()
    val mainScreens = listOf(
        DrawerItem("POS", Icons.Default.ShoppingCart, PosScreen),
        DrawerItem("Sales Reports", Icons.Default.Assessment, ReportsScreen, PermissionEnum.VIEW_SALES_REPORTS.name),
    )

    val managementScreens = listOf(
        DrawerItem("Products", Icons.Default.Inventory, ProductsScreen, PermissionEnum.MANAGE_PRODUCTS.name),
        DrawerItem("Categories", Icons.Default.Category, CategoriesScreen, PermissionEnum.MANAGE_CATEGORIES.name),
        DrawerItem("Units", Icons.Default.SquareFoot, UnitsScreen, PermissionEnum.MANAGE_UNITS.name),
        DrawerItem("Purchases", Icons.Default.Inventory2, PurchaseScreen, PermissionEnum.MANAGE_STOCK.name),
        DrawerItem("Stocks", Icons.Default.ReceiptLong, StockScreen,PermissionEnum.MANAGE_STOCK.name),
        DrawerItem("Settings", Icons.Default.Settings, SettingsScreen),
    )

    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))

        // Main Navigation
        mainScreens.forEach { item ->
            if (item.requiredPermission == null || userPermissions.contains(item.requiredPermission)) {
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = navigator.lastItem == item.screen,
                    onClick = {
                        navigator.replaceAll(item.screen)
                        closeDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Management Screens
        Text("Management", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        managementScreens.forEach { item ->
            if (item.requiredPermission == null || userPermissions.contains(item.requiredPermission)) {
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = navigator.lastItem == item.screen,
                    onClick = {
                        navigator.push(item.screen)
                        closeDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // System Actions
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = {
                authManager.clearSession()
                navigator.replaceAll(LoginScreen)
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Exit") },
            label = { Text("Exit") },
            selected = false,
            onClick = { applicationScope.exitApplication() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

private data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
    val requiredPermission: String? = null
)