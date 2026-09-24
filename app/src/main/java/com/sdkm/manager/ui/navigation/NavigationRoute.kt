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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sdkm.manager.R
import com.sdkm.manager.ui.battery.BatteryScreen
import com.sdkm.manager.ui.home.HomeScreen
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
fun SDKMNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
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

const val HomeRoute = "home"
const val SoCRoute = "soc"
const val CpuRoute = "cpu"
const val GpuRoute = "gpu"
const val BatteryRoute = "battery"
const val KernelRoute = "kernel"
const val KernelSettingsRoute = "kernel_settings"
