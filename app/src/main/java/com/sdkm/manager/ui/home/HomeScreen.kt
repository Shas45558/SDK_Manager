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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Groups3
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.composables.icons.materialsymbols.roundedfilled.R.drawable.materialsymbols_ic_shield_rounded_filled
import com.sdkm.manager.R
import com.sdkm.manager.ui.contributor.ContributorActivity
import com.sdkm.manager.ui.navigation.BottomNavigationBar
import com.sdkm.manager.ui.components.SimpleTopAppBar
import androidx.core.net.toUri

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullKernelVersion by rememberSaveable { mutableStateOf(false) }

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

    Scaffold(
        topBar = { SimpleTopAppBar() },
        bottomBar = { BottomNavigationBar(navController) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RootStatusCard()
            }

            item {
                ExkmSection(title = "DEVICE") {
                    ExkmRow(
                        icon = painterResource(materialsymbols_ic_mobile_info_rounded_filled),
                        title = stringResource(R.string.device),
                        value = "${deviceInfo.manufacturer} ${deviceInfo.deviceName}",
                        summary = deviceInfo.deviceCodename,
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = painterResource(materialsymbols_ic_android_rounded_filled),
                        title = stringResource(R.string.android),
                        value = "Android ${deviceInfo.androidVersion}",
                        summary = "SDK ${deviceInfo.sdkVersion}",
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = painterResource(R.drawable.ic_linux),
                        title = stringResource(R.string.kernel),
                        value = if (isFullKernelVersion) deviceInfo.fullKernelVersion else deviceInfo.kernelVersion,
                        summary = "Tap to ${if (isFullKernelVersion) "shorten" else "show full"} version",
                        onClick = { isFullKernelVersion = !isFullKernelVersion },
                    )
                }
            }

            item {
                ExkmSection(title = "HARDWARE") {
                    ExkmRow(
                        icon = painterResource(materialsymbols_ic_memory_rounded_filled),
                        title = stringResource(R.string.cpu),
                        value = deviceInfo.cpu,
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = painterResource(materialsymbols_ic_memory_rounded_filled),
                        title = stringResource(R.string.gpu),
                        value = deviceInfo.gpuModel,
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = Icons.Rounded.Security,
                        title = stringResource(R.string.wireguard),
                        value = deviceInfo.wireGuard,
                        summary = if (deviceInfo.hasWireGuard) "Available" else "Not detected",
                    )
                }
            }

            item {
                ExkmSection(title = "MEMORY") {
                    ExkmRow(
                        icon = painterResource(materialsymbols_ic_memory_rounded_filled),
                        title = stringResource(R.string.ram),
                        value = deviceInfo.ramInfo,
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = Icons.Rounded.Memory,
                        title = "ZRAM",
                        value = deviceInfo.zram,
                        summary = "Compressed swap",
                    )
                }
            }

            item {
                ExkmSection(title = "ABOUT SDKM") {
                    ExkmRow(
                        icon = Icons.Rounded.Groups3,
                        title = stringResource(R.string.contributors),
                        summary = stringResource(R.string.contributors_desc),
                        onClick = { context.startActivity(Intent(context, ContributorActivity::class.java)) },
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(R.string.source_code),
                        summary = stringResource(R.string.source_code_desc),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Shas45558/SDK_Manager.git".toUri()))
                        },
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = painterResource(R.drawable.ic_telegram),
                        title = stringResource(R.string.telegram_group),
                        summary = stringResource(R.string.telegram_group_desc),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/ocmt6768".toUri()))
                        },
                    )
                    ExkmDivider()
                    ExkmRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.app_version),
                        value = appVersion,
                    )
                }
            }
        }
    }
}

@Composable
private fun RootStatusCard() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(9.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
            ) {}
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ROOT ACCESS",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "KernelSU / root features ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ExkmSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
        )
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ExkmDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

@Composable
private fun ExkmRow(
    icon: Any,
    title: String,
    value: String? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clip(RoundedCornerShape(10.dp)) else Modifier)
        .then(if (onClick != null) Modifier.background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 10.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (icon) {
            is ImageVector -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            is Painter -> Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
