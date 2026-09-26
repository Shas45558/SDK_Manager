/*
 * Copyright (c) 2026 Rve <rve27github@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.sdkm.manager.ui.navigation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.sdkm.manager.ui.home.HomeViewModel
import com.sdkm.manager.ui.monitor.GameMonitorService
import com.sdkm.manager.ui.settings.SettingsActivity
import com.sdkm.manager.ui.taskKiller.TaskKillerActivity
import com.sdkm.manager.utils.Utils
import kotlinx.coroutines.launch

@Composable
fun SDKMDrawer(
    navController: NavController,
    selectedRoute: String?,
    deviceInfo: HomeViewModel.DeviceInfo,
    appVersion: String,
    drawerClose: suspend () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReboot by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("SDKM", style = MaterialTheme.typography.titleLarge)
                Text("Kernel & System Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        DrawerItem("Home", Icons.Rounded.Home, selectedRoute == HomeRoute) {
            scope.launch { drawerClose() }
            navController.navigate(HomeRoute) { launchSingleTop = true }
        }
        DrawerItem("CPU", Icons.Rounded.Memory, selectedRoute == CpuRoute) {
            scope.launch { drawerClose() }
            navController.navigate(CpuRoute) { launchSingleTop = true }
        }
        DrawerItem("GPU", Icons.Rounded.DeveloperBoard, selectedRoute == GpuRoute) {
            scope.launch { drawerClose() }
            navController.navigate(GpuRoute) { launchSingleTop = true }
        }
        DrawerItem("Memory", Icons.Rounded.Memory, selectedRoute == MemoryRoute) {
            scope.launch { drawerClose() }
            navController.navigate(MemoryRoute) { launchSingleTop = true }
        }
        DrawerItem("Monitor", Icons.Rounded.MonitorHeart, selectedRoute == MonitorRoute) {
            scope.launch { drawerClose() }
            navController.navigate(MonitorRoute) { launchSingleTop = true }
        }
        DrawerItem("Kernel Tuning", Icons.Rounded.Tune, selectedRoute == KernelTuningRoute) {
            scope.launch { drawerClose() }
            navController.navigate(KernelTuningRoute) { launchSingleTop = true }
        }
        DrawerItem("Task Killer", Icons.Rounded.DeleteSweep) {
            scope.launch { drawerClose() }
            context.startActivity(Intent(context, TaskKillerActivity::class.java))
        }
        DrawerItem("Logs", Icons.Rounded.ListAlt, selectedRoute == LogsRoute) {
            scope.launch { drawerClose() }
            navController.navigate(LogsRoute) { launchSingleTop = true }
        }
        DrawerItem("Backup / Profiles", Icons.Rounded.Backup, selectedRoute == ProfilesRoute) {
            scope.launch { drawerClose() }
            navController.navigate(ProfilesRoute) { launchSingleTop = true }
        }
        DrawerItem("Settings", Icons.Rounded.Settings) {
            scope.launch { drawerClose() }
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
        DrawerItem("Reboot", Icons.Rounded.RestartAlt) {
            scope.launch { drawerClose() }
            showReboot = true
        }
        DrawerItem("About", Icons.Rounded.Info) {
            scope.launch { drawerClose() }
            showAbout = true
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerItem("Exit", Icons.Rounded.ExitToApp) { onExit() }
        Spacer(Modifier.height(16.dp))
    }

    if (showReboot) {
        AlertDialog(
            onDismissRequest = { showReboot = false },
            title = { Text("Reboot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showReboot = false; Utils.reboot("") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Normal") }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showReboot = false; Utils.reboot("recovery") }) { Text("Recovery") }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showReboot = false; Utils.reboot("bootloader") }) { Text("Bootloader") }
                }
            },
            confirmButton = { TextButton(onClick = { showReboot = false }) { Text("Cancel") } },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AboutSection("SDKM About") {
                        CompactRow("Version", appVersion)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Shas45558/SDK_Manager"))) } },
                        ) { Text("Source") }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ocmt6768"))) } },
                        ) { Text("Telegram") }
                    }
                    AboutSection("System About") {
                        CompactRow("Device", "${deviceInfo.manufacturer} ${deviceInfo.deviceName}")
                        CompactRow("Codename", deviceInfo.deviceCodename)
                        CompactRow("Android", deviceInfo.androidVersion)
                        CompactRow("SDK", deviceInfo.sdkVersion.toString())
                        CompactRow("Kernel", deviceInfo.fullKernelVersion.ifBlank { deviceInfo.kernelVersion })
                        if (deviceInfo.hasWireGuard) CompactRow("WireGuard", deviceInfo.wireGuard)
                        CompactRow("Root", "ACTIVE")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun DrawerItem(title: String, icon: ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(21.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 3.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(9.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), content = content)
        }
    }
}

@Composable
private fun CompactRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
    }
}
