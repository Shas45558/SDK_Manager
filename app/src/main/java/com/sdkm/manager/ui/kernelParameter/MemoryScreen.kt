@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sdkm.manager.ui.kernelParameter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sdkm.manager.utils.KernelUtils
import com.sdkm.manager.ui.components.SimpleTopAppBar
import com.sdkm.manager.utils.SoCUtils

@Composable
fun MemoryScreen(
    viewModel: KernelParameterViewModel = viewModel(),
    navController: NavController,
) {
    val memory by viewModel.memory.collectAsStateWithLifecycle()
    var swappiness by remember { mutableFloatStateOf(0f) }
    var extraFreeKbytes by remember { mutableFloatStateOf(0f) }
    var pageCluster by remember { mutableStateOf("0") }
    var vfsCachePressure by remember { mutableStateOf("200") }
    var dirtyRatio by remember { mutableStateOf("10") }
    var dirtyBackgroundRatio by remember { mutableStateOf("5") }

    LaunchedEffect(memory.swappiness, memory.extraFreeKbytes, memory.pageCluster, memory.vfsCachePressure, memory.dirtyRatio, memory.dirtyBackgroundRatio) {
        swappiness = memory.swappiness.toFloatOrNull()?.coerceIn(0f, 200f) ?: 0f
        extraFreeKbytes = memory.extraFreeKbytes.toFloatOrNull()?.coerceIn(0f, 131072f) ?: 0f
        if (memory.pageCluster.isNotBlank() && memory.pageCluster != "N/A") pageCluster = memory.pageCluster
        if (memory.vfsCachePressure.isNotBlank() && memory.vfsCachePressure != "N/A") vfsCachePressure = memory.vfsCachePressure
        if (memory.dirtyRatio.isNotBlank() && memory.dirtyRatio != "N/A") dirtyRatio = memory.dirtyRatio
        if (memory.dirtyBackgroundRatio.isNotBlank() && memory.dirtyBackgroundRatio != "N/A") dirtyBackgroundRatio = memory.dirtyBackgroundRatio
    }

    Column(Modifier.fillMaxSize()) {
        SimpleTopAppBar(title = "Memory")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RamInfoCard()
            }
            item {
                ZramInfoCard(memory)
            }
            item {
                KernelSliderCard(
                    title = "Swappiness",
                    valueText = swappiness.toInt().toString(),
                    value = swappiness,
                    range = 0f..200f,
                    onValueChange = { swappiness = it },
                    onApply = {
                        val value = swappiness.toInt()
                        viewModel.setValue(KernelUtils.SWAPPINESS, value.toString())
                    },
                )
            }
            item {
                KernelSliderCard(
                    title = "Extra free kbytes",
                    valueText = "${extraFreeKbytes.toInt()} KB",
                    value = extraFreeKbytes,
                    range = 0f..131072f,
                    onValueChange = { extraFreeKbytes = it },
                    onApply = { viewModel.setValue(KernelUtils.EXTRA_FREE_KBYTES, extraFreeKbytes.toInt().toString()) },
                )
            }
            item {
                KernelInputCard("page-cluster", pageCluster, "0", { pageCluster = it }) {
                    viewModel.setValue(KernelUtils.PAGE_CLUSTER, pageCluster)
                }
            }
            item {
                KernelInputCard("vfs_cache_pressure", vfsCachePressure, "200", { vfsCachePressure = it }) {
                    viewModel.setValue(KernelUtils.VFS_CACHE_PRESSURE, vfsCachePressure)
                }
            }
            item {
                KernelInputCard("dirty_ratio", dirtyRatio, "10", { dirtyRatio = it }) {
                    viewModel.setValue(KernelUtils.DIRTY_RATIO, dirtyRatio)
                }
            }
            item {
                KernelInputCard("dirty_background_ratio", dirtyBackgroundRatio, "5", { dirtyBackgroundRatio = it }) {
                    viewModel.setValue(KernelUtils.DIRTY_BACKGROUND_RATIO, dirtyBackgroundRatio)
                }
            }
        }
    }
}

@Composable
private fun RamInfoCard() {
    val context = LocalContext.current
    val ram = remember { SoCUtils.getRamMemoryInfo(context) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("RAM", style = MaterialTheme.typography.titleMedium)
            Text("Total ${ram.total}", style = MaterialTheme.typography.bodyMedium)
            Text("Used ${ram.used}", style = MaterialTheme.typography.bodyMedium)
            Text("Free ${ram.free}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ZramInfoCard(memory: KernelParameterViewModel.Memory) {
    val progress = if (memory.zramTotalBytes > 0L) (memory.zramUsedBytes.toFloat() / memory.zramTotalBytes).coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ZRAM", style = MaterialTheme.typography.titleMedium)
                Text(memory.zramSize, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Used ${formatMemoryBytes(memory.zramUsedBytes)}")
                Text("Free ${formatMemoryBytes(memory.zramFreeBytes)}")
            }
            if (memory.zramCompAlgorithm != "N/A") Text("Compression: ${memory.zramCompAlgorithm}")
        }
    }
}

private fun formatMemoryBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mib = bytes / (1024.0 * 1024.0)
    return if (mib >= 1024.0) "%.1f GB".format(java.util.Locale.US, mib / 1024.0).replace(".0 GB", " GB") else "%.0f MB".format(java.util.Locale.US, mib)
}

@Composable
private fun KernelSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onApply: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(valueText, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range)
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
        }
    }
}

@Composable
private fun KernelInputCard(
    title: String,
    value: String,
    defaultValue: String,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit = {},
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("Default $defaultValue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.filter(Char::isDigit)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply")
            }
        }
    }
}
