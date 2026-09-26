/*
 * Copyright (c) 2026 Rve <rve27github@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sdkm.manager.ui.taskKiller

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu
import com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.sdkm.manager.R
import com.sdkm.manager.ui.theme.SDKMTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class RunningProcess(
    val pid: Int,
    val uid: Int,
    val processName: String,
    val packageName: String?,
    val appName: String,
    val rssKb: Long,
    val cpuPercent: Float,
    val canStop: Boolean,
    val isSystemApp: Boolean,
    val frozen: Boolean,
)

class TaskKillerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Shell.getCachedShell() == null) {
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(20),
            )
        }

        setContent {
            SDKMTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides androidx.compose.ui.unit.Density(
                        density = density.density * 1.10f,
                        fontScale = density.fontScale * 1.10f,
                    ),
                ) {
                    TaskKillerScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun TaskKillerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processes by remember { mutableStateOf<List<RunningProcess>>(emptyList()) }
    var memory by remember { mutableStateOf(MemorySnapshot(0, 0, 0)) }
    var showSystem by remember { mutableStateOf(false) }
    var pendingFreeze by remember { mutableStateOf<RunningProcess?>(null) }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val newProcesses = TaskKillerUtils.getRunningProcesses(context, processes)
            val newMemory = TaskKillerUtils.getMemory(context)
            launch(Dispatchers.Main) {
                processes = newProcesses
                memory = newMemory
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            refresh()
        }
    }

    SDKMStandaloneDrawerHost {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Killer") },
                    navigationIcon = { SDKMStandaloneHamburgerMenu() },
                    actions = {
                        IconButton(onClick = { refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { MemoryCard(memory, processes.size) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Running processes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showSystem = !showSystem }) {
                            Text(if (showSystem) "Hide system" else "Show system")
                        }
                    }
                }

                val visible = processes.filter { showSystem || !it.isSystemApp }
                if (visible.isEmpty()) {
                    item { Text("No matching running processes", modifier = Modifier.padding(16.dp)) }
                }
                items(visible, key = { "${it.pid}:${it.packageName}:${it.processName}" }) { process ->
                    ProcessCard(
                        process = process,
                        onStop = {
                            scope.launch(Dispatchers.IO) {
                                process.packageName?.let { TaskKillerUtils.forceStop(it) }
                                refresh()
                            }
                        },
                        onFreeze = { pendingFreeze = process },
                    )
                }
            }
        }
    }

    pendingFreeze?.let { process ->
        AlertDialog(
            onDismissRequest = { pendingFreeze = null },
            title = { Text(if (process.frozen) "Unfreeze app?" else "Freeze app?") },
            text = {
                Text(
                    if (process.frozen)
                        "Enable ${process.packageName} again for user 0."
                    else
                        "Disable ${process.packageName} for user 0. System apps can be required by Android; freezing one may cause features or the device UI to stop working."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = process
                    pendingFreeze = null
                    scope.launch(Dispatchers.IO) {
                        target.packageName?.let { TaskKillerUtils.setFrozen(it, !target.frozen) }
                        refresh()
                    }
                }) { Text(if (process.frozen) "Unfreeze" else "Freeze") }
            },
            dismissButton = { TextButton(onClick = { pendingFreeze = null }) { Text("Cancel") } },
        )
    }
}

@androidx.compose.runtime.Composable
private fun MemoryCard(memory: MemorySnapshot, processCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("RAM", style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatBytes(memory.usedBytes)} / ${formatBytes(memory.totalBytes)}  •  ${formatBytes(memory.availableBytes)} available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("$processCount running process entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ProcessCard(
    process: RunningProcess,
    onStop: () -> Unit,
    onFreeze: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(process.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(process.processName, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "UID ${process.uid}  •  ${process.packageName ?: "unknown package"}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (process.isSystemApp) Text("SYSTEM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("RAM ${formatKb(process.rssKb)}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text("CPU ${String.format(Locale.US, "%.1f", process.cpuPercent)}%", style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (process.canStop) {
                    Button(onClick = onStop, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null)
                        Text("  Force stop")
                    }
                }
                if (process.packageName != null) {
                    Button(onClick = onFreeze, modifier = Modifier.weight(1f)) {
                        Icon(if (process.frozen) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = null)
                        Text(if (process.frozen) "  Unfreeze" else "  Freeze")
                    }
                }
            }
        }
    }
}

private data class MemorySnapshot(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
)

private object TaskKillerUtils {
    fun getMemory(context: Context): MemorySnapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val info = android.app.ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return MemorySnapshot(info.totalMem, (info.totalMem - info.availMem).coerceAtLeast(0L), info.availMem)
    }

    fun getRunningProcesses(context: Context, previous: List<RunningProcess>): List<RunningProcess> {
        val packageManager = context.packageManager
        val installed = packageManager.getInstalledApplications(0)
        val appByPackage = installed.associateBy { it.packageName }
        val packageOutput = Shell.cmd("cmd package list packages -U; pm list packages -d").exec()
        val uidToPackage = HashMap<Int, String>()
        val frozenPackages = HashSet<String>()
        if (packageOutput.isSuccess) {
            packageOutput.out.forEach { line ->
                val m = Regex("""package:([^\s]+)\s+uid:(\d+)""").find(line)
                if (m != null) uidToPackage.putIfAbsent(m.groupValues[2].toIntOrNull() ?: return@forEach, m.groupValues[1])
                if (line.startsWith("package:") && !line.contains(" uid:")) frozenPackages += line.removePrefix("package:").trim()
            }
        }
        val output = Shell.cmd("ps -A -o PID,UID,RSS,PCPU,NAME").exec()
        if (!output.isSuccess) return emptyList()
        return output.out.asSequence().drop(1).mapNotNull { line ->
            val m = Regex("""^\s*(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+(.+?)\s*$""").find(line) ?: return@mapNotNull null
            val pid = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val uid = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (pid <= 1) return@mapNotNull null
            val pkg = uidToPackage[uid] ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val app = appByPackage[pkg] ?: return@mapNotNull null
            val rss = m.groupValues[3].toLongOrNull() ?: 0L
            val cpu = m.groupValues[4].replace("%", "").toFloatOrNull()?.coerceIn(0f,100f) ?: 0f
            val name = m.groupValues[5].trim()
            val system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            RunningProcess(pid, uid, name, pkg, app.loadLabel(packageManager).toString(), rss, cpu, true, system, pkg in frozenPackages)
        }.sortedWith(compareByDescending<RunningProcess> { it.cpuPercent }.thenByDescending { it.rssKb }).toList()
    }

    fun forceStop(packageName: String) {
        if (!validPackage(packageName)) return
        Shell.cmd("am force-stop $packageName").exec()
    }

    fun setFrozen(packageName: String, frozen: Boolean) {
        if (!validPackage(packageName)) return
        if (frozen) Shell.cmd("pm disable-user --user 0 $packageName").exec()
        else Shell.cmd("pm enable $packageName").exec()
    }

    private fun isFrozen(packageName: String): Boolean {
        if (!validPackage(packageName)) return false
        val r = Shell.cmd("pm list packages -d $packageName").exec()
        return r.isSuccess && r.out.any { it.trim() == "package:$packageName" }
    }

    private fun validPackage(packageName: String): Boolean = Regex("""^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$""").matches(packageName)
}

private fun formatBytes(bytes: Long): String {
    val gib = 1024.0 * 1024.0 * 1024.0
    val mib = 1024.0 * 1024.0
    return when {
        bytes >= gib -> String.format(Locale.US, "%.1f GiB", bytes / gib)
        bytes >= mib -> String.format(Locale.US, "%.0f MiB", bytes / mib)
        else -> String.format(Locale.US, "%.0f KiB", bytes / 1024.0)
    }
}

private fun formatKb(kb: Long): String {
    return if (kb >= 1024) {
        String.format(Locale.US, "%.0f MB", kb / 1024.0)
    } else {
        "$kb KB"
    }    }

