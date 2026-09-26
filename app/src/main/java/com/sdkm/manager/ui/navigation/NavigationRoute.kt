package com.sdkm.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sdkm.manager.ui.MainActivity
import com.sdkm.manager.ui.home.HomeScreen
import com.sdkm.manager.ui.home.HomeViewModel
import com.sdkm.manager.ui.kernelParameter.MemoryScreen
import com.sdkm.manager.ui.logs.LogsScreen
import com.sdkm.manager.ui.monitor.MonitorScreen
import com.sdkm.manager.ui.profiles.ProfilesScreen
import com.sdkm.manager.ui.soc.SoCScreen
import com.sdkm.manager.ui.soc.SocSection

const val HomeRoute = "home"
const val SoCRoute = "soc"
const val CpuRoute = "cpu"
const val GpuRoute = "gpu"
const val BatteryRoute = "battery"
const val MemoryRoute = "memory"
const val MonitorRoute = "monitor"
const val LogsRoute = "logs"
const val ProfilesRoute = "profiles"

val LocalSDKMTabNavigator = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
fun SDKMNavHost(startDestination: String = HomeRoute, openDrawerOnStart: Boolean = false) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()

    DisposableEffect(Unit) {
        homeViewModel.loadDeviceInfo(context)
        homeViewModel.loadAppVersion(context)
        onDispose { }
    }

    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalSDKMTabNavigator provides navigate) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(HomeRoute) { HomeScreen(navController = navController) }
            composable(CpuRoute) { SoCScreen(navController = navController, section = SocSection.CPU) }
            composable(GpuRoute) { SoCScreen(navController = navController, section = SocSection.GPU) }
            composable(MemoryRoute) { MemoryScreen(navController = navController) }
            composable(MonitorRoute) {
                MonitorScreen(onNotificationRequest = {
                    (context as? MainActivity)?.requestMonitorNotification()
                })
            }
            composable(LogsRoute) { LogsScreen() }
            composable(ProfilesRoute) { ProfilesScreen() }
        }
    }
}
