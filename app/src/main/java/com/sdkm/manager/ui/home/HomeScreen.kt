/*
 * Copyright (c) 2025 Rve <rve27github@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sdkm.manager.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_android_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_memory_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_mobile_info_rounded_filled
import com.sdkm.manager.R
import com.sdkm.manager.ui.contributor.ContributorActivity
import com.sdkm.manager.ui.monitor.GameMonitorService
import com.sdkm.manager.ui.settings.SettingsActivity
import com.sdkm.manager.ui.soc.SoCViewModel
import com.sdkm.manager.ui.taskKiller.TaskKillerActivity
import com.sdkm.manager.utils.KernelUtils
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ColumnScope
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val socViewModel: SoCViewModel = viewModel()
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var isFullKernelVersion by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDeviceInfo(context)
                viewModel.loadAppVersion(context)
                socViewModel.startJob()
            }
            if (event == Lifecycle.Event.ON_PAUSE) socViewModel.stopJob()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            socViewModel.stopJob()
        }
    }

    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by viewModel.appVersion.collectAsStateWithLifecycle()
    val cpuState by socViewModel.cpu0State.collectAsStateWithLifecycle()
    val gpuState by socViewModel.gpuState.collectAsStateWithLifecycle()
    val cpuUsage by socViewModel.cpuUsage.collectAsStateWithLifecycle()
    val gpuUsage by socViewModel.gpuUsage.collectAsStateWithLifecycle()
    val ramState by socViewModel.ramState.collectAsStateWithLifecycle()

    val cpuHistory = remember { mutableStateListOf<Float>() }
    val gpuHistory = remember { mutableStateListOf<Float>() }
    val cpuPercent = cpuUsage.filter { it.isDigit() || it == '.' }.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val gpuPercent = gpuUsage.filter { it.isDigit() || it == '.' }.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    if (cpuHistory.lastOrNull() != cpuPercent) {
        cpuHistory.add(cpuPercent)
        if (cpuHistory.size > 32) cpuHistory.removeAt(0)
    }
    if (gpuHistory.lastOrNull() != gpuPercent) {
        gpuHistory.add(gpuPercent)
        if (gpuHistory.size > 32) gpuHistory.removeAt(0)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
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
                DrawerItem("Home", Icons.Rounded.Home, selected = true) { scope.launch { drawerState.close() } }
                DrawerItem("CPU", Icons.Rounded.Memory) {
                    scope.launch { drawerState.close() }
                    navController.navigate("soc")
                }
                DrawerItem("GPU", Icons.Rounded.DeveloperBoard) {
                    scope.launch { drawerState.close() }
                    navController.navigate("soc")
                }
                DrawerItem("Monitor", Icons.Rounded.MonitorHeart) {
                    scope.launch { drawerState.close() }
                    if (Settings.canDrawOverlays(context)) {
                        ContextCompat.startForegroundService(context, Intent(context, GameMonitorService::class.java).setAction(GameMonitorService.ACTION_START))
                    } else {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    }
                }
                DrawerItem("Task Killer", Icons.Rounded.DeleteSweep) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, TaskKillerActivity::class.java))
                }
                DrawerItem("Settings", Icons.Rounded.Settings) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
                DrawerItem("About", Icons.Rounded.Info) {
                    scope.launch { drawerState.close() }
                    showAbout = true
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem("Exit", Icons.Rounded.ExitToApp) {
                    (context as? Activity)?.finish()
                }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SDKM", maxLines = 1)
                            Text("Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = innerPadding.calculateTopPadding() + 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    DashboardHeader(
                        device = "${deviceInfo.manufacturer} ${deviceInfo.deviceName}",
                        codename = deviceInfo.deviceCodename,
                        android = "Android ${deviceInfo.androidVersion} • SDK ${deviceInfo.sdkVersion}",
                    )
                }
                item {
                    DashboardSection("CPU") {
                        MetricHeader("Current", cpuState.currentFreq, "Load", cpuUsage)
                        MiniUsageGraph(cpuHistory, Modifier.fillMaxWidth().height(76.dp))
                        CompactRow("Governor", cpuState.gov)
                        CompactRow("Max", cpuState.maxFreq)
                    }
                }
                item {
                    DashboardSection("GPU") {
                        MetricHeader("Current", gpuState.currentFreq, "Load", gpuUsage)
                        CompactRow("Governor", gpuState.gov)
                        CompactRow("Max", gpuState.maxFreq)
                    }
                }
                item {
                    DashboardSection("RAM & ZRAM") {
                        val ramUsed = formatBytes(ramState.usedBytes)
                        val ramTotal = formatBytes(ramState.totalBytes)
                        CompactRow("RAM", "$ramUsed / $ramTotal")
                        CompactRow("ZRAM", deviceInfo.zram)
                        val ratio = if (ramState.totalBytes > 0) (ramState.usedBytes.toFloat() / ramState.totalBytes).coerceIn(0f, 1f) else 0f
                        MiniBar(ratio)
                    }
                }
                item {
                    DashboardSection("SYSTEM") {
                        CompactRow("Kernel", if (isFullKernelVersion) deviceInfo.fullKernelVersion else deviceInfo.kernelVersion) {
                            isFullKernelVersion = !isFullKernelVersion
                        }
                        CompactRow("WireGuard", deviceInfo.wireGuard)
                        CompactRow("Root", "ACTIVE")
                    }
                }
                item {
                    DashboardSection("ABOUT SDKM") {
                        CompactRow("Version", appVersion)
                        CompactRow("Source", "GitHub") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Shas45558/SDK_Manager")))
                        }
                        CompactRow("Telegram", "t.me/ocmt6768") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ocmt6768")))
                        }
                        CompactRow("Contributors", "View") {
                            context.startActivity(Intent(context, ContributorActivity::class.java))
                        }
                    }
                }
            }
        }
    }

    if (showAbout) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About SDKM") },
            text = { Text("SDKM — Kernel & System Manager\n\nVersion $appVersion\nRoot-focused Android system controls and monitoring.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAbout = false }) { Text("OK") }
            },
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
private fun DashboardHeader(device: String, codename: String, android: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(device, style = MaterialTheme.typography.titleMedium)
            Text(codename, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(android, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 3.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(9.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), content = content)
        }
    }
}

@Composable
private fun MetricHeader(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(leftTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(leftValue, style = MaterialTheme.typography.titleMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(rightTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rightValue, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CompactRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
    }
}

@Composable
private fun MiniUsageGraph(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(6.dp)) {
        if (values.size < 2) return@Canvas
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).coerceAtLeast(1)
            val y = size.height - (value / 100f * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, MaterialTheme.colorScheme.primary, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable
private fun MiniBar(value: Float) {
    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxWidth(value).height(5.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "N/A"
    val gb = bytes / 1073741824f
    return if (gb >= 1f) String.format(Locale.US, "%.1f GB", gb) else String.format(Locale.US, "%.0f MB", bytes / 1048576f)
}
