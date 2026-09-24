/*
 * Copyright (c) 2025 Rve <rve27github@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.sdkm.manager.ui.home

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Groups3
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_android_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_info_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_memory_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_mobile_info_rounded_filled
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_shield_rounded_filled
import com.sdkm.manager.R
import com.sdkm.manager.ui.components.SimpleTopAppBar
import com.sdkm.manager.ui.contributor.ContributorActivity
import com.sdkm.manager.ui.navigation.BatteryRoute
import com.sdkm.manager.ui.navigation.BottomNavigationBar
import com.sdkm.manager.ui.navigation.KernelRoute
import com.sdkm.manager.ui.navigation.SoCRoute

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDeviceInfo(context)
                viewModel.loadAppVersion(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by viewModel.appVersion.collectAsStateWithLifecycle()
    var isFullKernelVersion by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { SimpleTopAppBar() },
        bottomBar = { BottomNavigationBar(navController) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DashboardHeader(
                    device = "${deviceInfo.manufacturer} ${deviceInfo.deviceName}",
                    codename = deviceInfo.deviceCodename,
                    kernel = if (isFullKernelVersion) deviceInfo.fullKernelVersion else deviceInfo.kernelVersion,
                    onKernelClick = { isFullKernelVersion = !isFullKernelVersion },
                )
            }

            item {
                Text(
                    text = "SYSTEM OVERVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            item {
                OverviewGrid(
                    android = "${deviceInfo.androidVersion} (${deviceInfo.sdkVersion})",
                    ram = "${deviceInfo.ramInfo} + ${deviceInfo.zram}",
                    cpu = deviceInfo.cpu,
                    gpu = deviceInfo.gpuModel,
                )
            }

            item {
                Text(
                    text = "QUICK CONTROLS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.nav_soc),
                        subtitle = "CPU / GPU",
                        onClick = { navController.navigate(SoCRoute) },
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.BatteryFull,
                        title = stringResource(R.string.nav_battery),
                        subtitle = "Power",
                        onClick = { navController.navigate(BatteryRoute) },
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.nav_kernel),
                        subtitle = "Kernel",
                        onClick = { navController.navigate(KernelRoute) },
                    )
                }
            }

            item {
                Text(
                    text = "DEVICE DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            items(
                listOf(
                    DetailItem(painterResource(materialsymbols_ic_mobile_info_rounded_filled), stringResource(R.string.device), "${deviceInfo.manufacturer} ${deviceInfo.deviceName} (${deviceInfo.deviceCodename})"),
                    DetailItem(painterResource(materialsymbols_ic_android_rounded_filled), stringResource(R.string.android), "${deviceInfo.androidVersion} (${deviceInfo.sdkVersion})"),
                    DetailItem(painterResource(materialsymbols_ic_memory_rounded_filled), stringResource(R.string.ram), "${deviceInfo.ramInfo} + ${deviceInfo.zram} (ZRAM)"),
                    DetailItem(painterResource(materialsymbols_ic_memory_rounded_filled), stringResource(R.string.cpu), deviceInfo.cpu),
                    DetailItem(painterResource(materialsymbols_ic_memory_rounded_filled), stringResource(R.string.gpu), deviceInfo.gpuModel),
                    DetailItem(painterResource(materialsymbols_ic_shield_rounded_filled), stringResource(R.string.wireguard), deviceInfo.wireGuard),
                ),
            ) { item ->
                DetailRow(item)
            }

            item {
                Text(
                    text = "ABOUT SDKM",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            item {
                AboutCard(
                    appVersion = appVersion,
                    onContributors = { context.startActivity(Intent(context, ContributorActivity::class.java)) },
                    onSource = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Shas45558/SDK_Manager.git".toUri())) },
                    onTelegram = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/ocmt6768".toUri())) },
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(device: String, codename: String, kernel: String, onKernelClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(materialsymbols_ic_info_rounded_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SDKM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(device, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("$codename", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(14.dp))
            Card(
                onClick = onKernelClick,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_linux), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.kernel), style = MaterialTheme.typography.labelMedium)
                        Text(kernel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("DETAILS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun OverviewGrid(android: String, ram: String, cpu: String, gpu: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(Modifier.weight(1f), "ANDROID", android, Icons.Filled.Memory)
            MetricCard(Modifier.weight(1f), "MEMORY", ram, Icons.Filled.Memory)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(Modifier.weight(1f), "CPU", cpu, Icons.Filled.Memory)
            MetricCard(Modifier.weight(1f), "GPU", gpu, Icons.Filled.Memory)
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class DetailItem(val icon: androidx.compose.ui.graphics.painter.Painter, val title: String, val value: String)

@Composable
private fun DetailRow(item: DetailItem) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AboutCard(appVersion: String, onContributors: () -> Unit, onSource: () -> Unit, onTelegram: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column {
            AboutRow(Icons.Rounded.Groups3, stringResource(R.string.contributors), stringResource(R.string.contributors_desc), onContributors)
            AboutRow(Icons.Rounded.Code, stringResource(R.string.source_code), stringResource(R.string.source_code_desc), onSource)
            AboutRow(painterResource(R.drawable.ic_telegram), stringResource(R.string.telegram_group), stringResource(R.string.telegram_group_desc), onTelegram)
            AboutRow(painterResource(materialsymbols_ic_info_rounded_filled), stringResource(R.string.app_version), appVersion, null)
        }
    }
}

@Composable
private fun AboutRow(icon: Any, title: String, subtitle: String, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            when (icon) {
                is ImageVector -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (onClick != null) Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent), modifier = Modifier.fillMaxWidth()) { content() } else content()
}
