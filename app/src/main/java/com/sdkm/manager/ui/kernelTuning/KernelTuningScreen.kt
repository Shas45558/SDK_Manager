package com.sdkm.manager.ui.kernelTuning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sdkm.manager.ui.components.SimpleTopAppBar

@Composable
fun KernelTuningScreen(
    navController: NavController,
    viewModel: KernelTuningViewModel = viewModel(),
) {
    val state by viewModel.state
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column {
        SimpleTopAppBar(title = "Kernel Tuning")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TuningSection("I/O scheduler", "Select an available scheduler for block devices.") {
                state.devices.forEach { device ->
                    DeviceTuningCard(device, viewModel)
                }
            } }
            item { TuningSection("Read-ahead", "Block-device read-ahead in KB.") {
                state.devices.forEach { device ->
                    NumericCard(
                        title = "${device.name} read-ahead",
                        current = device.readAhead,
                        value = viewModel.input["ra:${device.name}"] ?: device.readAhead,
                        onValue = { viewModel.input["ra:${device.name}"] = it },
                        onApply = {
                            viewModel.write("/sys/block/${device.name}/queue/read_ahead_kb", viewModel.input["ra:${device.name}"] ?: device.readAhead)
                        },
                    )
                }
            } }
            item { TuningSection("Entropy", "Randomness-pool controls, only when the kernel exposes them.") {
                state.entropy.forEach { p ->
                    NumericCard(p.label, p.value, viewModel.input[p.path] ?: p.value,
                        onValue = { viewModel.input[p.path] = it },
                        onApply = { viewModel.write(p.path, viewModel.input[p.path] ?: p.value) },
                    )
                }
                if (state.entropy.isEmpty()) Text("No writable entropy controls exposed by this kernel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { TuningSection("TCP / network", "Network stack parameters exposed under /proc/sys/net.") {
                state.network.forEach { p ->
                    NumericCard(p.label, p.value, viewModel.input[p.path] ?: p.value,
                        onValue = { viewModel.input[p.path] = it },
                        onApply = { viewModel.write(p.path, viewModel.input[p.path] ?: p.value) },
                    )
                }
            } }
            item { TuningSection("Filesystem", "Kernel filesystem limits and cache controls.") {
                state.filesystem.forEach { p ->
                    NumericCard(p.label, p.value, viewModel.input[p.path] ?: p.value,
                        onValue = { viewModel.input[p.path] = it },
                        onApply = { viewModel.write(p.path, viewModel.input[p.path] ?: p.value) },
                    )
                }
            } }
        }
    }
}

@Composable
private fun TuningSection(title: String, description: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun DeviceTuningCard(device: BlockDeviceState, vm: KernelTuningViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(device.name, style = MaterialTheme.typography.labelLarge)
        Text("Current: ${device.scheduler}", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            device.availableSchedulers.forEach { scheduler ->
                Button(onClick = { vm.setScheduler(device.name, scheduler) }) { Text(scheduler) }
            }
        }
    }
}

@Composable
private fun NumericCard(title: String, current: String, value: String, onValue: (String) -> Unit, onApply: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.weight(1f), singleLine = true)
            Button(onClick = onApply) { Text("Apply") }
        }
        Text("Current: $current", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
