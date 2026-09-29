@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sdkm.manager.ui.display

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sdkm.manager.ui.components.SimpleTopAppBar
import com.sdkm.manager.utils.DisplayState
import com.sdkm.manager.utils.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DisplayScreen(navController: NavController, viewModel: DisplayViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var resolutionPercent by remember { mutableFloatStateOf(100f) }
    var sizePercent by remember { mutableFloatStateOf(100f) }
    var width by remember { mutableStateOf("1080") }
    var height by remember { mutableStateOf("2340") }
    var density by remember { mutableStateOf("440") }
    var brightness by remember { mutableFloatStateOf(0f) }
    var selectedRefresh by remember { mutableFloatStateOf(60f) }
    var hbm by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        width = state.width.toString(); height = state.height.toString(); density = state.density.toString()
        resolutionPercent = (state.width * 100f / DisplayUtils.BASE_WIDTH).coerceIn(50f, 100f)
        sizePercent = (state.density * 100f / DisplayUtils.BASE_DENSITY).coerceIn(50f, 150f)
        brightness = if (state.maxBrightness > 0) state.brightness * 100f / state.maxBrightness else 0f
        selectedRefresh = state.refreshRate; hbm = state.hbmMode
    }
    fun apply(action: () -> Boolean) = scope.launch(Dispatchers.IO) {
        val ok = action(); viewModel.refresh()
        message = if (ok) "Applied successfully" else "Apply failed — previous value kept"
    }
    fun refresh() = scope.launch(Dispatchers.IO) { viewModel.refresh() }

    Column(Modifier.fillMaxSize()) {
        SimpleTopAppBar(title = "Display")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { StatusCard(state) }
            item {
                ControlCard("Resolution", "Change the logical resolution used by apps. Lower values can reduce GPU workload.") {
                    ValueRow("Scale", "${resolutionPercent.toInt()}%")
                    Slider(value = resolutionPercent, onValueChange = { resolutionPercent = it }, valueRange = 50f..100f)
                    val preview = DisplayUtils.resolutionForPercent(resolutionPercent.toInt())
                    Text("Calculated: ${preview.first} × ${preview.second}", color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(width, { width = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Width") }, singleLine = true)
                        OutlinedTextField(height, { height = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Height") }, singleLine = true)
                    }
                    Button(Modifier.fillMaxWidth(), onClick = {
                        val w = width.toIntOrNull(); val h = height.toIntOrNull()
                        if (w != null && h != null) apply { DisplayUtils.setResolution(w, h) }
                    }) { Text("Apply Resolution") }
                    TextButton(Modifier.fillMaxWidth(), onClick = {
                        val p = resolutionPercent.toInt(); val (w, h) = DisplayUtils.resolutionForPercent(p); width = w.toString(); height = h.toString()
                    }) { Text("Use ${resolutionPercent.toInt()}% values") }
                }
            }
            item {
                ControlCard("Display Size / Density", "System-wide UI scaling. This is independent from rendering resolution.") {
                    ValueRow("Scale", "${sizePercent.toInt()}%")
                    Slider(value = sizePercent, onValueChange = { sizePercent = it }, valueRange = 50f..150f)
                    Text("Calculated DPI: ${DisplayUtils.densityForPercent(sizePercent.toInt())}", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(density, { density = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Manual DPI") }, singleLine = true)
                    Button(Modifier.fillMaxWidth(), onClick = { density.toIntOrNull()?.let { d -> apply { DisplayUtils.setDensity(d) } } }) { Text("Apply Density") }
                    TextButton(Modifier.fillMaxWidth(), onClick = { density = DisplayUtils.densityForPercent(sizePercent.toInt()).toString() }) { Text("Use percentage DPI") }
                }
            }
            item {
                ControlCard("Refresh Rate", "Only refresh rates detected by the device are offered.") {
                    Text("Supported: ${state.supportedRefreshRates.joinToString(", ") { "${it.toInt()} Hz" }}")
                    val min = state.supportedRefreshRates.minOrNull() ?: 60f
                    val max = state.supportedRefreshRates.maxOrNull() ?: 60f
                    Slider(value = selectedRefresh, onValueChange = { selectedRefresh = it }, valueRange = min..max, enabled = state.supportedRefreshRates.size > 1)
                    Button(Modifier.fillMaxWidth(), enabled = state.supportedRefreshRates.size > 1, onClick = { apply { DisplayUtils.setRefreshRate(selectedRefresh) } }) { Text("Apply Refresh Rate") }
                }
            }
            item {
                ControlCard("Brightness", "Hardware backlight percentage.") {
                    ValueRow("Brightness", "${brightness.toInt()}%")
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0f..100f)
                    Button(Modifier.fillMaxWidth(), onClick = { apply { DisplayUtils.setBrightness(brightness.toInt()) } }) { Text("Apply Brightness") }
                }
            }
            item {
                ControlCard("HBM", "Hardware Brightness Mode exposed by this MTK display driver.") {
                    Text("Current mode: $hbm")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..2).forEach { mode ->
                            Button(Modifier.weight(1f), onClick = { hbm = mode; apply { DisplayUtils.setHbm(mode) } }) { Text("Mode $mode") }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reset Display", style = MaterialTheme.typography.titleMedium)
                        Text("Restore 1080 × 2340 / 440 DPI.")
                        Button(Modifier.fillMaxWidth(), onClick = { apply { DisplayUtils.resetDisplay() } }) { Text("Reset Display") }
                        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
                        TextButton(Modifier.fillMaxWidth(), onClick = { refresh() }) { Text("Refresh Values") }
                    }
                }
            }
        }
    }
}

@Composable private fun StatusCard(state: DisplayState) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Current Display", style = MaterialTheme.typography.titleMedium)
        Text("Resolution: ${state.width} × ${state.height}")
        Text("Density: ${state.density} DPI")
        Text("Refresh: ${state.refreshRate.toInt()} Hz")
        Text("Brightness: ${if (state.maxBrightness > 0) state.brightness * 100 / state.maxBrightness else 0}%")
        Text("HBM mode: ${state.hbmMode}")
    }
}

@Composable private fun ControlCard(title: String, description: String, content: @Composable () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        content()
    }
}
@Composable private fun ValueRow(label: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, color = MaterialTheme.colorScheme.primary) }
