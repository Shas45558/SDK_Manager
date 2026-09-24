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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sdkm.manager.ui.battery.BatteryViewModel
import com.sdkm.manager.ui.soc.SoCViewModel
import com.sdkm.manager.ui.components.SimpleTopAppBar

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val socViewModel: SoCViewModel = viewModel()
    val batteryViewModel: BatteryViewModel = viewModel()

    DisposableEffect(context) {
        viewModel.loadAppVersion(context)
        onDispose { }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDeviceInfo(context)
                socViewModel.startJob()
                batteryViewModel.initializeBatteryInfo(context)
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                socViewModel.stopJob()
                batteryViewModel.unregisterBatteryListeners(context)
                batteryViewModel.stopJob()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            socViewModel.stopJob()
        }
    }

    val zramMemory by viewModel.zramMemory.collectAsStateWithLifecycle()
    val cpuState by socViewModel.cpu0State.collectAsStateWithLifecycle()
    val hasBigCluster by socViewModel.hasBigCluster.collectAsStateWithLifecycle()
    val bigClusterState by socViewModel.bigClusterState.collectAsStateWithLifecycle()
    val hasPrimeCluster by socViewModel.hasPrimeCluster.collectAsStateWithLifecycle()
    val primeClusterState by socViewModel.primeClusterState.collectAsStateWithLifecycle()
    val gpuState by socViewModel.gpuState.collectAsStateWithLifecycle()
    val cpuUsage by socViewModel.cpuUsage.collectAsStateWithLifecycle()
    val gpuUsage by socViewModel.gpuUsage.collectAsStateWithLifecycle()
    val ramState by socViewModel.ramState.collectAsStateWithLifecycle()
    val cpuCoreMetrics by socViewModel.cpuCoreMetrics.collectAsStateWithLifecycle()
    val batteryInfo by batteryViewModel.batteryInfo.collectAsStateWithLifecycle()

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

    Scaffold(
        topBar = { SimpleTopAppBar(title = "SDKM", subtitle = "Dashboard") },
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    DashboardSection("CPU") {
                        MetricHeader("Current", cpuState.currentFreq + " MHz", "Load", cpuUsage + "%")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ClusterChip(
                                title = "Little",
                                current = cpuState.currentFreq,
                                max = cpuState.maxFreq,
                                governor = cpuState.gov,
                                modifier = Modifier.weight(1f),
                            )
                            if (hasBigCluster) {
                                ClusterChip(
                                    title = "Big",
                                    current = bigClusterState.currentFreq,
                                    max = bigClusterState.maxFreq,
                                    governor = bigClusterState.gov,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (hasPrimeCluster) {
                                ClusterChip(
                                    title = "Prime",
                                    current = primeClusterState.currentFreq,
                                    max = primeClusterState.maxFreq,
                                    governor = primeClusterState.gov,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiniUsageGraph(cpuHistory, Modifier.weight(1.55f).height(132.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                cpuCoreMetrics.take(8).forEach { core ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("CPU${core.cpu}", style = MaterialTheme.typography.labelMedium)
                                        Text("${core.load}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("${core.frequencyMHz} MHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        CompactRow("Little Max", cpuState.maxFreq + " MHz")
                        if (hasBigCluster) {
                            CompactRow("Big Max", bigClusterState.maxFreq + " MHz")
                        }
                        if (hasPrimeCluster) {
                            CompactRow("Prime Max", primeClusterState.maxFreq + " MHz")
                        }
                    }
                }
                item {
                    DashboardSection("GPU") {
                        MetricHeader("Current", gpuState.currentFreq + " MHz", "Load", gpuUsage + "%")
                        MiniUsageGraph(gpuHistory, Modifier.fillMaxWidth().height(96.dp))
                        CompactRow("Governor", gpuState.gov)
                        CompactRow("Max", gpuState.maxFreq + " MHz")
                    }
                }
                item {
                    DashboardSection("RAM & ZRAM") {
                        val ramUsed = formatBytes(ramState.usedBytes)
                        val ramTotal = formatBytes(ramState.totalBytes)
                        val zramUsed = formatBytes(zramMemory.usedBytes)
                        val zramTotal = formatBytes(zramMemory.totalBytes)
                        CompactRow("RAM", "$ramUsed / $ramTotal")
                        val ramRatio = if (ramState.totalBytes > 0) (ramState.usedBytes.toFloat() / ramState.totalBytes).coerceIn(0f, 1f) else 0f
                        MiniBar(ramRatio)
                        CompactRow("ZRAM", "$zramUsed / $zramTotal")
                        val zramRatio = if (zramMemory.totalBytes > 0) (zramMemory.usedBytes.toFloat() / zramMemory.totalBytes).coerceIn(0f, 1f) else 0f
                        MiniBar(zramRatio)
                    }
                }
                item {
                    DashboardBatterySection(batteryInfo)
                }
            }
        }
    }

@Composable
private fun ClusterChip(title: String, current: String, max: String, governor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                Text("$current MHz", style = MaterialTheme.typography.bodyMedium)
                Text("/ $max", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(governor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

@Composable
private fun DashboardBatterySection(batteryInfo: BatteryViewModel.BatteryInfo) {
    val level = batteryInfo.level.removeSuffix("%").toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val capacity = batteryInfo.designCapacity.ifBlank { "N/A" }
    val discharged = if (capacity.endsWith("mAh")) {
        val mAh = capacity.removeSuffix(" mAh").toFloatOrNull()
        if (mAh != null) "${(mAh * (100f - level) / 100f).toInt()} mAh discharged" else "N/A"
    } else "N/A"

    DashboardSection("BATTERY") {
        MetricHeader("Capacity", capacity, "Charge", batteryInfo.level)
        CompactRow("Status", batteryInfo.status + if (batteryInfo.current != "N/A") " • ${batteryInfo.current}" else "")
        CompactRow("Voltage", batteryInfo.voltage)
        CompactRow("Power", batteryInfo.power)
        CompactRow("Health", batteryInfo.health)
        CompactRow("Temperature", batteryInfo.temp)
        CompactRow("Level", discharged)
        MiniBar(level / 100f)
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
    val graphColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(6.dp)) {
        if (values.size < 2) return@Canvas
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).coerceAtLeast(1)
            val y = size.height - (value / 100f * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, graphColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
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
    if (bytes >= 1073741824L) {
        val tenths = bytes / 107374182L
        return "${tenths / 10}.${tenths % 10} GB"
    }
    return "${bytes / 1048576L} MB"
}
