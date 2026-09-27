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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var swappiness by remember { mutableFloatStateOf(0f) }
    var extraFreeKbytes by remember { mutableFloatStateOf(0f) }
    var pageCluster by remember { mutableFloatStateOf(0f) }
    var vfsCachePressure by remember { mutableFloatStateOf(200f) }
    var dirtyRatio by remember { mutableFloatStateOf(10f) }
    var dirtyBackgroundRatio by remember { mutableFloatStateOf(5f) }

    LaunchedEffect(memory.swappiness, memory.extraFreeKbytes, memory.pageCluster, memory.vfsCachePressure, memory.dirtyRatio, memory.dirtyBackgroundRatio) {
        swappiness = memory.swappiness.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
        extraFreeKbytes = memory.extraFreeKbytes.toFloatOrNull()?.coerceIn(0f, 131072f) ?: 0f
        memory.pageCluster.toFloatOrNull()?.let { pageCluster = it.coerceIn(0f, 8f) }
        memory.vfsCachePressure.toFloatOrNull()?.let { vfsCachePressure = it.coerceIn(0f, 500f) }
        memory.dirtyRatio.toFloatOrNull()?.let { dirtyRatio = it.coerceIn(0f, 100f) }
        memory.dirtyBackgroundRatio.toFloatOrNull()?.let { dirtyBackgroundRatio = it.coerceIn(0f, 100f) }
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
                SwappinessCard(
                    value = swappiness,
                    onValueChange = { swappiness = it },
                    onApply = {
                        val value = swappiness.toInt().coerceIn(0, 100)
                        scope.launch { viewModel.setValue(KernelUtils.SWAPPINESS, value.toString()).join() }
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
                KernelSliderCard(
                    title = "page-cluster",
                    valueText = pageCluster.toInt().toString(),
                    value = pageCluster,
                    range = 0f..8f,
                    steps = 7,
                    onValueChange = { pageCluster = it },
                    onApply = { viewModel.setValue(KernelUtils.PAGE_CLUSTER, pageCluster.toInt().toString()) },
                )
            }
            item {
                KernelSliderCard(
                    title = "vfs_cache_pressure",
                    valueText = vfsCachePressure.toInt().toString(),
                    value = vfsCachePressure,
                    range = 0f..500f,
                    steps = 49,
                    onValueChange = { vfsCachePressure = it },
                    onApply = { viewModel.setValue(KernelUtils.VFS_CACHE_PRESSURE, vfsCachePressure.toInt().toString()) },
                )
            }
            item {
                KernelSliderCard(
                    title = "dirty_ratio",
                    valueText = dirtyRatio.toInt().toString(),
                    value = dirtyRatio,
                    range = 0f..100f,
                    steps = 99,
                    onValueChange = { dirtyRatio = it },
                    onApply = { viewModel.setValue(KernelUtils.DIRTY_RATIO, dirtyRatio.toInt().toString()) },
                )
            }
            item {
                KernelSliderCard(
                    title = "dirty_background_ratio",
                    valueText = dirtyBackgroundRatio.toInt().toString(),
                    value = dirtyBackgroundRatio,
                    range = 0f..100f,
                    steps = 99,
                    onValueChange = { dirtyBackgroundRatio = it },
                    onApply = { viewModel.setValue(KernelUtils.DIRTY_BACKGROUND_RATIO, dirtyBackgroundRatio.toInt().toString()) },
                )
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
private fun SwappinessCard(
    value: Float,
    onValueChange: (Float) -> Unit,
    onApply: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Swappiness", style = MaterialTheme.typography.titleMedium)
                Text(value.toInt().toString(), color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
            )
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
        }
    }
}

@Composable
private fun KernelSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    onApply: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(valueText, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
        }
    }
}
