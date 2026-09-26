@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sdkm.manager.ui.monitor

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sdkm.manager.ui.theme.SDKMTheme
import com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost
import com.sdkm.manager.ui.components.SimpleTopAppBar
import com.sdkm.manager.utils.KernelUtils
import com.sdkm.manager.utils.SoCUtils
import com.sdkm.manager.utils.Utils
import kotlinx.coroutines.delay
import java.util.Locale

class MonitorActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startNotificationService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SDKMTheme {
                SDKMStandaloneDrawerHost(selectedItem = "Monitor") {
                    MonitorScreen(onNotificationRequest = { requestNotificationPermission() })
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startNotificationService()
        }
    }

    private fun startNotificationService() {
        ContextCompat.startForegroundService(this, Intent(this, GameMonitorService::class.java).setAction(GameMonitorService.ACTION_START_NOTIFICATION))
    }
}

private data class LiveStats(
    val cpu: Int, val gpu: Int, val ram: Int, val zram: Int,
    val cpuFreq: Long, val gpuFreq: Int, val batteryTemp: Float,
    val voltage: Float, val current: Float, val watts: Float,
    val charging: String, val temperatures: List<Pair<String, Float>>,
)

@androidx.compose.runtime.Composable
fun MonitorScreen(onNotificationRequest: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refreshMs by remember { mutableStateOf(1000L) }
    var stats by remember { mutableStateOf(readLiveStats(context)) }
    val cpuHistory = remember { mutableStateOf(List(60) { 0f }) }
    val gpuHistory = remember { mutableStateOf(List(60) { 0f }) }
    val ramHistory = remember { mutableStateOf(List(60) { 0f }) }

    LaunchedEffect(refreshMs) {
        while (true) {
            stats = readLiveStats(context)
            cpuHistory.value = (cpuHistory.value.drop(1) + stats.cpu.toFloat()).takeLast(60)
            gpuHistory.value = (gpuHistory.value.drop(1) + stats.gpu.toFloat()).takeLast(60)
            ramHistory.value = (ramHistory.value.drop(1) + stats.ram.toFloat()).takeLast(60)
            delay(refreshMs)
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = { SimpleTopAppBar(title = "Monitor") },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MonitorCard("CPU", "${stats.cpu}% • ${stats.cpuFreq} MHz", cpuHistory.value) }
            item { MonitorCard("GPU", "${stats.gpu}% • ${stats.gpuFreq} MHz", gpuHistory.value) }
            item { MonitorCard("RAM", "${stats.ram}% used", ramHistory.value) }
            item { MonitorCard("Battery", "${stats.batteryTemp} °C • ${stats.watts} W • ${stats.charging}", null) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Battery electrical", style = MaterialTheme.typography.titleMedium)
                        Text("Voltage ${stats.voltage} V   Current ${stats.current} mA   Power ${stats.watts} W")
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Temperature sensors", style = MaterialTheme.typography.titleMedium)
                        if (stats.temperatures.isEmpty()) Text("No readable kernel thermal sensors")
                        stats.temperatures.take(12).forEach { (name, temp) -> Text("$name  %.1f °C".format(Locale.US, temp)) }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Refresh interval: ${refreshMs} ms", style = MaterialTheme.typography.titleMedium)
                        Slider(value = refreshMs.toFloat(), onValueChange = { refreshMs = it.toLong() }, valueRange = 250f..5000f, steps = 18)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { refreshMs = 250 }) { Text("250ms") }
                            OutlinedButton(onClick = { refreshMs = 1000 }) { Text("1s") }
                            OutlinedButton(onClick = { refreshMs = 2000 }) { Text("2s") }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Monitor output", style = MaterialTheme.typography.titleMedium)
                        Text("The monitor page stays live while you browse the detailed readings. You can also start a floating overlay or an ongoing notification.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (Settings.canDrawOverlays(context)) {
                                    ContextCompat.startForegroundService(context, Intent(context, GameMonitorService::class.java).setAction(GameMonitorService.ACTION_START_OVERLAY))
                                } else context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                            }) { Icon(Icons.Filled.Visibility, null); Spacer(Modifier.height(1.dp)); Text("Overlay") }
                            Button(onClick = onNotificationRequest) {
                                Icon(Icons.Filled.Notifications, null)
                                Text("Notification")
                            }
                            OutlinedButton(onClick = { context.startService(Intent(context, GameMonitorService::class.java).setAction(GameMonitorService.ACTION_STOP)) }) { Icon(Icons.Filled.Stop, null); Text("Stop") }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MonitorCard(title: String, subtitle: String, history: List<Float>?) {
    val graphColor = MaterialTheme.colorScheme.primary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle)
            if (history != null) {
                Canvas(Modifier.fillMaxWidth().height(70.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (history.size > 1) {
                        val path = Path()
                        history.forEachIndexed { i, v ->
                            val x = size.width * i / (history.size - 1)
                            val y = size.height * (1f - v.coerceIn(0f, 100f) / 100f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, graphColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }
                }
            }
        }
    }
}

private fun readLiveStats(context: Context): LiveStats {
    val cpu = SoCUtils.getCpuUsage(context).toIntOrNull()?.coerceIn(0, 100) ?: 0
    val gpu = SoCUtils.getGpuUsage(context).toIntOrNull()?.coerceIn(0, 100) ?: 0
    val ram = SoCUtils.getRamMemoryInfo(context)
    val zram = KernelUtils.getZramMemoryInfo()
    val ramPct = if (ram.totalBytes > 0) (ram.usedBytes * 100 / ram.totalBytes).toInt().coerceIn(0, 100) else 0
    val zramPct = if (zram.totalBytes > 0) (zram.usedBytes * 100 / zram.totalBytes).toInt().coerceIn(0, 100) else 0
    val cpuFreq = listOf(SoCUtils.CURRENT_FREQ_CPU0, SoCUtils.CURRENT_FREQ_CPU3, SoCUtils.CURRENT_FREQ_CPU4, SoCUtils.CURRENT_FREQ_CPU6, SoCUtils.CURRENT_FREQ_CPU7).mapNotNull { Utils.readFile(it).toLongOrNull() }.maxOrNull()?.div(1000) ?: 0
    val gpuFreq = if (SoCUtils.isMtkGpu()) SoCUtils.readMtkGpuCurrentFreq().toIntOrNull() ?: 0 else Utils.readFile(SoCUtils.CURRENT_FREQ_GPU).toLongOrNull()?.div(1000)?.toInt() ?: 0
    val bm = context.getSystemService(BatteryManager::class.java)
    val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val voltage = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
    val current = (bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0) / 1000f
    val watts = voltage * kotlin.math.abs(current) / 1000f
    val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) { BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"; BatteryManager.BATTERY_STATUS_FULL -> "Full"; else -> "Discharging" }
    val sensors = readThermalSensors()
    return LiveStats(cpu, gpu, ramPct, zramPct, cpuFreq, gpuFreq, temp, voltage, current, watts, status, sensors)
}

private fun readThermalSensors(): List<Pair<String, Float>> {
    val out = mutableListOf<Pair<String, Float>>()
    val base = java.io.File("/sys/class/thermal")
    base.listFiles()?.filter { it.name.startsWith("thermal_zone") }?.forEach { zone ->
        val raw = runCatching { java.io.File(zone, "temp").readText().trim().toFloat() }.getOrNull() ?: return@forEach
        val temp = if (kotlin.math.abs(raw) > 1000) raw / 1000f else raw
        val type = runCatching { java.io.File(zone, "type").readText().trim() }.getOrDefault(zone.name)
        if (temp in -20f..150f) out += type to temp
    }
    return out.distinctBy { it.first }
}
