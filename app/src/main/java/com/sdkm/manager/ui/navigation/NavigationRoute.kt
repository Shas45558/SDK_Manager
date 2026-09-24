/*
 * Copyright (c) 2025 Rve <rve27github@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Dear programmer:
// When I wrote this code, only god and
// I knew how it worked.
// Now, only god knows it!
//
// Therefore, if you are trying to optimize
// this routine and it fails (most surely),
// please increase this counter as a
// warning for the next person:
//
// total hours wasted here = 254
//
package com.sdkm.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sdkm.manager.R
import com.sdkm.manager.ui.battery.BatteryScreen
import com.sdkm.manager.ui.home.HomeScreen
import com.sdkm.manager.ui.home.HomeViewModel
import com.sdkm.manager.ui.kernelParameter.KernelParameterScreen
import com.sdkm.manager.ui.kernelParameter.KernelSettingsScreen
import com.sdkm.manager.ui.soc.SoCScreen
import com.sdkm.manager.ui.soc.SocSection

sealed class NavigationRoute(val route: String, val titleRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : NavigationRoute(
        route = "home",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    object SoC : NavigationRoute(
        route = "soc",
        titleRes = R.string.nav_soc,
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Memory,
    )

    object Battery : NavigationRoute(
        route = "battery",
        titleRes = R.string.nav_battery,
        selectedIcon = Icons.Filled.BatteryFull,
        unselectedIcon = Icons.Outlined.Battery0Bar,
    )

    object KernelParameter : NavigationRoute(
        route = "kernel",
        titleRes = R.string.nav_kernel,
        selectedIcon = Icons.Filled.Storage,
        unselectedIcon = Icons.Outlined.Storage,
    )
}

@Composable
fun SDKMNavHost(startDestination: String = HomeRoute, openDrawerOnStart: Boolean = false) {
    val navController = rememberNavController()
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val deviceInfo by homeViewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by homeViewModel.appVersion.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    DisposableEffect(Unit) {
        homeViewModel.loadDeviceInfo(context)
        homeViewModel.loadAppVersion(context)
        onDispose { }
    }

    androidx.compose.runtime.LaunchedEffect(openDrawerOnStart) {
        if (openDrawerOnStart) {
            drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SDKMDrawer(
                navController = navController,
                selectedRoute = currentRoute,
                deviceInfo = deviceInfo,
                appVersion = appVersion,
                drawerClose = { drawerState.close() },
                onExit = { (context as? android.app.Activity)?.finish() },
            )
        },
    ) {
        CompositionLocalProvider(LocalSDKMDrawer provides { scope.launch { drawerState.open() } }) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable(HomeRoute) {
                    HomeScreen(navController = navController)
                }
                composable(SoCRoute) {
                    SoCScreen(navController = navController, section = SocSection.ALL)
                }
                composable(CpuRoute) {
                    SoCScreen(navController = navController, section = SocSection.CPU)
                }
                composable(GpuRoute) {
                    SoCScreen(navController = navController, section = SocSection.GPU)
                }
                composable(BatteryRoute) {
                    BatteryScreen(navController = navController)
                }
                composable(KernelRoute) {
                    KernelParameterScreen(navController = navController)
                }
                composable(KernelSettingsRoute) {
                    KernelSettingsScreen(navController = navController)
                }
            }
        }
    }
}

const val HomeRoute = "home"
const val SoCRoute = "soc"
const val CpuRoute = "cpu"
const val GpuRoute = "gpu"
const val BatteryRoute = "battery"
const val KernelRoute = "kernel"
const val KernelSettingsRoute = "kernel_settings"
