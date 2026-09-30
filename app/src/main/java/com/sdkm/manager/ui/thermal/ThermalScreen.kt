package com.sdkm.manager.ui.thermal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sdkm.manager.ui.components.SimpleTopAppBar

@Composable
fun ThermalScreen(
    navController: NavController,
    viewModel: ThermalViewModel = viewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.start()
                Lifecycle.Event.ON_PAUSE -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopAppBar(title = "Thermal")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ThermalStatusCard(state) }
            item { PpmPolicyCard(state, viewModel) }
            item { ThermalProtectionCard(state) }
            if (state.writeError != null) {
                item {
                    Text(
                        state.writeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalStatusCard(state: ThermalViewModel.State) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Thermal status", style = MaterialTheme.typography.titleMedium)
            InfoRow("CPU temperature", state.cpuTemp)
            InfoRow("PPM thermal", if (state.thermalActive) "Active" else "Inactive")
            InfoRow("Limited power", state.thermalLimit)
            InfoRow("Current thermal power", state.thermalCurrentPower)
            InfoRow("Power range", state.thermalPowerRange)
            if (state.tripPoints.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("CPU trip points", style = MaterialTheme.typography.titleSmall)
                state.tripPoints.forEach { (label, value) -> InfoRow(label, value) }
            }
        }
    }
}

@Composable
private fun PpmPolicyCard(state: ThermalViewModel.State, viewModel: ThermalViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PPM policies", style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.ppmAvailable) "Runtime policy controls" else "PPM interface not available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (state.policies.isEmpty()) {
                Text("No supported PPM policy nodes detected.", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.policies.forEach { policy ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(policy.name, style = MaterialTheme.typography.bodyLarge)
                            Text("Policy ${policy.index}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = policy.enabled,
                            onCheckedChange = { viewModel.setPolicy(policy.index, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalProtectionCard(state: ThermalViewModel.State) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Thermal protection", style = MaterialTheme.typography.titleMedium)
            Text(
                "Kernel safety mechanisms are display-only and are not exposed as manual controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoRow("cpu_adaptive_0", state.cpuAdaptive)
            InfoRow("mtktscpu-sysrst", state.systemReset)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
