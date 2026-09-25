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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val processName: String,
    val packageName: String?,
    val appName: String,
    val rssKb: Long,
    val canStop: Boolean,
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

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val newProcesses = TaskKillerUtils.getRunningProcesses(context)
            val newMemory = TaskKillerUtils.getMemory(context)
            launch(Dispatchers.Main) {
                processes = newProcesses
                memory = newMemory
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            refresh()
        }
    }

    SDKMStandaloneDrawerHost {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.task_killer)) },
                navigationIcon = { SDKMStandaloneHamburgerMenu() },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = context.getString(R.string.refresh))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MemoryCard(memory)
            }

            if (processes.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.no_running_processes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(processes, key = { "${it.pid}:${it.processName}" }) { process ->
                ProcessCard(
                    process = process,
                    onStop = {
                        scope.launch(Dispatchers.IO) {
                            if (!process.packageName.isNullOrBlank()) {
                                TaskKillerUtils.forceStop(process.packageName)
                            }
                            refresh()
                        }
                    },
                )
            }
        }
    }
    }
}

@androidx.compose.runtime.Composable
private fun MemoryCard(memory: MemorySnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("RAM", style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatBytes(memory.usedBytes)} / ${formatBytes(memory.totalBytes)}  •  ${formatBytes(memory.availableBytes)} available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun ProcessCard(process: RunningProcess, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    process.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    "${process.processName}  •  PID ${process.pid}  •  ${formatKb(process.rssKb)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            if (process.canStop) {
                Button(onClick = onStop) {
                    Icon(
                        Icons.Filled.StopCircle,
                        contentDescription = null,
                    )
                    Text(
                        text = "  ${"Stop"}",
                    )
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
        return MemorySnapshot(
            totalBytes = info.totalMem,
            usedBytes = (info.totalMem - info.availMem).coerceAtLeast(0L),
            availableBytes = info.availMem,
        )
    }

    fun getRunningProcesses(context: Context): List<RunningProcess> {
        val packageManager = context.packageManager
        val installed = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        // Root-only process discovery: Android's ActivityManager process list is heavily
        // restricted on modern Android, so use the kernel-visible process table and map
        // each UID back to a real installed user package.
        val uidToPackage = HashMap<Int, String>()
        val packageOutput = Shell.cmd("cmd package list packages -3 -U").exec()
        if (packageOutput.isSuccess) {
            packageOutput.out.forEach { line ->
                val match = Regex("""package:([^\s]+)\s+uid:(\d+)""").find(line)
                if (match != null) {
                    uidToPackage[match.groupValues[2].toIntOrNull() ?: return@forEach] = match.groupValues[1]
                }
            }
        }

        val appByPackage = installed.associateBy { it.packageName }
        val output = Shell.cmd("ps -A -o PID,UID,RSS,NAME").exec()
        if (!output.isSuccess) return emptyList()

        return output.out.drop(1).mapNotNull { line ->
            val match = Regex("""^\s*(\d+)\s+(\d+)\s+(\d+)\s+(.+?)\s*$""").find(line)
                ?: return@mapNotNull null
            val pid = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val uid = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val rssKb = match.groupValues[3].toLongOrNull() ?: 0L
            val processName = match.groupValues[4].trim()
            if (pid <= 1 || processName.isBlank()) return@mapNotNull null

            val packageName = uidToPackage[uid] ?: return@mapNotNull null
            if (packageName == context.packageName) return@mapNotNull null
            val appInfo = appByPackage[packageName] ?: return@mapNotNull null
            val appName = appInfo.loadLabel(packageManager).toString()

            RunningProcess(
                pid = pid,
                processName = processName,
                packageName = packageName,
                appName = appName,
                rssKb = rssKb,
                canStop = true,
            )
        }
            .groupBy { it.packageName }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.rssKb } }
            .sortedByDescending { it.rssKb }
    }

    fun forceStop(packageName: String) {
        if (!Regex("""^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$""").matches(packageName)) return
        Shell.cmd("am force-stop $packageName").exec()
    }
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

