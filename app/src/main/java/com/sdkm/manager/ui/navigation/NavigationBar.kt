/* Copyright (c) 2025 Rve <rve27github@gmail.com> */
package com.sdkm.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val items = listOf(NavigationRoute.Home, NavigationRoute.SoC, NavigationRoute.Battery, NavigationRoute.KernelParameter)

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        items.forEach { item ->
            val title = stringResource(item.titleRes)
            NavigationBarItem(
                icon = { Icon(if (currentDestination?.route == item.route) item.selectedIcon else item.unselectedIcon, contentDescription = title) },
                label = { Text(title) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
