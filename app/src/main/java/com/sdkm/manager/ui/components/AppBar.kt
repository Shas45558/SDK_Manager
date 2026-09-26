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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.sdkm.manager.ui.home.HomeViewModel
import com.sdkm.manager.ui.settings.SettingsActivity
import com.sdkm.manager.ui.taskKiller.TaskKillerActivity
import com.sdkm.manager.utils.Utils
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkm.manager.ui.MainActivity

@Composable
fun SimpleTopAppBar(
    title: String = "SDKM",
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val tabNavigator = com.sdkm.manager.ui.navigation.LocalSDKMTabNavigator.current
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val deviceInfo by homeViewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by homeViewModel.appVersion.collectAsStateWithLifecycle()
    var showReboot by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadDeviceInfo(context)
        homeViewModel.loadAppVersion(context)
    }

    val tabs = listOf(
        "Home" to HomeRoute,
        "CPU" to CpuRoute,
        "GPU" to GpuRoute,
        "Memory" to MemoryRoute,
        "Monitor" to MonitorRoute,
        "Task Killer" to "task_killer",
        "Logs" to LogsRoute,
        "Backup" to ProfilesRoute,
    )
    val selectedRoute = when (title) {
        "CPU" -> CpuRoute
        "GPU" -> GpuRoute
        "Memory" -> MemoryRoute
        "Monitor" -> MonitorRoute
        "Task Killer" -> "task_killer"
        "Logs" -> LogsRoute
        "Backup", "Backup / Profiles" -> ProfilesRoute
        else -> HomeRoute
    }

    fun openTab(route: String) {
        if (route == "task_killer") {
            context.startActivity(Intent(context, TaskKillerActivity::class.java))
            return
        }
        if (tabNavigator != null) {
            tabNavigator(route)
        } else {
            context.startActivity(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_START_ROUTE, route))
            (context as? android.app.Activity)?.finish()
        }
    }

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            navigationIcon = {
                Row {
                    IconButton(onClick = { showReboot = true }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reboot")
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "About")
                    }
                }
            },
            actions = actions,
        )
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.second == selectedRoute }.coerceAtLeast(0),
            edgePadding = 4.dp,
            divider = {},
        ) {
            tabs.forEach { (label, route) ->
                Tab(
                    selected = route == selectedRoute,
                    onClick = { openTab(route) },
                    text = { Text(label, maxLines = 1) },
                )
            }
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
            text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SDKM", style = MaterialTheme.typography.titleMedium)
                Text("Version: $appVersion")
                Text("Device: ${deviceInfo.manufacturer} ${deviceInfo.deviceName}")
                Text("Android: ${deviceInfo.androidVersion}")
                Text("Kernel: ${deviceInfo.fullKernelVersion.ifBlank { deviceInfo.kernelVersion }}")
                Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Shas45558/SDK_Manager"))) } }, modifier = Modifier.fillMaxWidth()) { Text("Source") }
                Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ocmt6768"))) } }, modifier = Modifier.fillMaxWidth()) { Text("Telegram") }
            } },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
        )
    }
}

@Composable
fun SDKMStandaloneDrawerHost(selectedItem: String? = null, content: @Composable () -> Unit) {
    content()
}

@Composable
fun SDKMStandaloneHamburgerMenu() {
    Row {
        val context = LocalContext.current
        IconButton(onClick = { Utils.reboot("") }) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reboot") }
        IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
        IconButton(onClick = { }) { Icon(Icons.Rounded.Info, contentDescription = "About") }
    }
}

@Composable
fun TopAppBarWithBackButton(text: String, onBack: () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    SimpleTopAppBar(title = text)
}
