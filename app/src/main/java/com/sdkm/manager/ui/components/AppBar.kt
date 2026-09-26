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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.sdkm.manager.ui.components

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.sdkm.manager.ui.navigation.LocalSDKMDrawer
import com.sdkm.manager.ui.navigation.HomeRoute
import com.sdkm.manager.ui.navigation.CpuRoute
import com.sdkm.manager.ui.navigation.GpuRoute
import com.sdkm.manager.ui.navigation.MemoryRoute
import com.sdkm.manager.ui.navigation.MonitorRoute
import com.sdkm.manager.ui.navigation.LogsRoute
import com.sdkm.manager.ui.navigation.ProfilesRoute
import com.sdkm.manager.ui.home.HomeViewModel
import com.sdkm.manager.ui.monitor.GameMonitorService
import com.sdkm.manager.ui.settings.SettingsActivity
import com.sdkm.manager.ui.taskKiller.TaskKillerActivity
import com.sdkm.manager.utils.Utils
import androidx.core.content.ContextCompat
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkm.manager.ui.MainActivity

@Composable
fun SimpleTopAppBar(title: String = "SDKM", subtitle: String? = null) {
    TopAppBar(
        title = {
            if (subtitle == null) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                androidx.compose.foundation.layout.Column {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = LocalSDKMDrawer.current) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
    )
}

val LocalStandaloneDrawer = staticCompositionLocalOf<() -> Unit> { { } }

@Composable
fun SDKMStandaloneDrawerHost(selectedItem: String? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val deviceInfo by homeViewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by homeViewModel.appVersion.collectAsStateWithLifecycle()
    var showReboot by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadDeviceInfo(context)
        homeViewModel.loadAppVersion(context)
    }

    fun openMain(route: String) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_START_ROUTE, route)
        )
        (context as? android.app.Activity)?.finish()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
            ) {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("SDKM", style = MaterialTheme.typography.titleLarge)
                        Text("Kernel & System Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                StandaloneDrawerItem("Home", Icons.Rounded.Home) { scope.launch { drawerState.close(); openMain(HomeRoute) } }
                StandaloneDrawerItem("CPU", Icons.Rounded.Memory) { scope.launch { drawerState.close(); openMain(CpuRoute) } }
                StandaloneDrawerItem("GPU", Icons.Rounded.DeveloperBoard) { scope.launch { drawerState.close(); openMain(GpuRoute) } }
                StandaloneDrawerItem("Monitor", Icons.Rounded.MonitorHeart, selected = selectedItem == "Monitor") {
                    scope.launch { drawerState.close(); openMain(MonitorRoute) }
                }
                StandaloneDrawerItem("Task Killer", Icons.Rounded.DeleteSweep) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, TaskKillerActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                }
                StandaloneDrawerItem("Memory", Icons.Rounded.Memory) { scope.launch { drawerState.close(); openMain(MemoryRoute) } }
                StandaloneDrawerItem("Logs", Icons.Rounded.ListAlt) { scope.launch { drawerState.close(); openMain(LogsRoute) } }
                StandaloneDrawerItem("Backup / Profiles", Icons.Rounded.Backup) { scope.launch { drawerState.close(); openMain(ProfilesRoute) } }
                StandaloneDrawerItem("Settings", Icons.Rounded.Settings) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                }
                StandaloneDrawerItem("Reboot", Icons.Rounded.RestartAlt) { scope.launch { drawerState.close(); showReboot = true } }
                StandaloneDrawerItem("About", Icons.Rounded.Info) { scope.launch { drawerState.close(); showAbout = true } }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                StandaloneDrawerItem("Exit", Icons.Rounded.ExitToApp) { (context as? android.app.Activity)?.finish() }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        CompositionLocalProvider(LocalStandaloneDrawer provides { scope.launch { drawerState.open() } }) {
            content()
        }
    }

    if (showReboot) {
        AlertDialog(
            onDismissRequest = { showReboot = false },
            title = { Text("Reboot") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showReboot = false; Utils.reboot("") }, modifier = Modifier.fillMaxWidth()) { Text("Normal") }
                Button(onClick = { showReboot = false; Utils.reboot("recovery") }, modifier = Modifier.fillMaxWidth()) { Text("Recovery") }
                Button(onClick = { showReboot = false; Utils.reboot("bootloader") }, modifier = Modifier.fillMaxWidth()) { Text("Bootloader") }
            } },
            confirmButton = { TextButton(onClick = { showReboot = false }) { Text("Cancel") } },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About") },
            text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SDKM About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Version: $appVersion")
                Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Shas45558/SDK_Manager"))) } }, modifier = Modifier.fillMaxWidth()) { Text("Source") }
                Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ocmt6768"))) } }, modifier = Modifier.fillMaxWidth()) { Text("Telegram") }
                Text("System About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Device: ${deviceInfo.manufacturer} ${deviceInfo.deviceName}")
                Text("Android: ${deviceInfo.androidVersion}")
                Text("Kernel: ${deviceInfo.fullKernelVersion.ifBlank { deviceInfo.kernelVersion }}")
            } },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun StandaloneDrawerItem(title: String, icon: ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(21.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SDKMStandaloneHamburgerMenu() {
    IconButton(onClick = LocalStandaloneDrawer.current) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
    }
}

@Composable
fun TopAppBarWithBackButton(text: String, onBack: () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    LargeFlexibleTopAppBar(
        title = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = { SDKMStandaloneHamburgerMenu() },
        scrollBehavior = scrollBehavior,
    )
}
