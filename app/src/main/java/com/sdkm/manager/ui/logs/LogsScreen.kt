/*
 * Copyright (c) 2026 Rve <rve27github@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.sdkm.manager.ui.logs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost
import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu
import com.sdkm.manager.ui.theme.SDKMTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object AppContextHolder { lateinit var context: android.content.Context }

class LogsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.context = applicationContext
        setContent { SDKMTheme { LogsScreen() } }
    }
}

private enum class LogSource(val label: String) {
    KERNEL("Kernel / dmesg"),
    SYSTEM("Android logcat"),
    APP("App / package")
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun LogsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    AppContextHolder.context = context.applicationContext
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(LogSource.KERNEL) }
    var query by remember { mutableStateOf("") }
    var appPackage by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var paused by remember { mutableStateOf(false) }
    var refreshMs by remember { mutableStateOf(1000L) }
    var menuOpen by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var kernelSuppressed by remember { mutableStateOf(LogsUtils.isKernelSuppressed()) }
    var kernelStatus by remember { mutableStateOf(if (LogsUtils.isRootAvailable()) { if (kernelSuppressed) "Kernel printk suppressed" else "Kernel printk active" } else "Root access unavailable") }

    fun refresh() {
        if (paused) return
        scope.launch(Dispatchers.IO) {
            val result = when (source) {
                LogSource.KERNEL -> LogsUtils.kernelLog()
                LogSource.SYSTEM -> LogsUtils.logcat()
                LogSource.APP -> LogsUtils.appLog(appPackage)
            }
            val filtered = if (query.isBlank()) result else result.filter { it.contains(query, ignoreCase = true) }
            launch(Dispatchers.Main) { lines = filtered.takeLast(1200) }
        }
    }

    LaunchedEffect(source, appPackage, query, paused, refreshMs) {
        if (!paused) {
            refresh()
            while (!paused) {
                delay(refreshMs)
                refresh()
            }
        }
    }

    SDKMStandaloneDrawerHost {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Logs") },
                    navigationIcon = { SDKMStandaloneHamburgerMenu() },
                    actions = {
                        IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, "Refresh") }
                        IconButton(onClick = { paused = !paused }) {
                            Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (paused) "Resume" else "Pause")
                        }
                        IconButton(onClick = { lines = emptyList() }) { Icon(Icons.Filled.ClearAll, "Clear view") }
                        IconButton(onClick = {
                            val text = lines.joinToString("\n")
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_TITLE, "SDKM Logs")
                            }, "Export logs"))
                        }) { Icon(Icons.Filled.FileDownload, "Export") }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { menuOpen = true }) { Text(source.label) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        LogSource.values().forEach { item ->
                            DropdownMenuItem(text = { Text(item.label) }, onClick = { source = item; menuOpen = false })
                        }
                    }
                    Button(onClick = { clearDialog = true }) { Text("Clear buffers") }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val result = if (kernelSuppressed) LogsUtils.restoreKernelLogging() else LogsUtils.suppressKernelLogging()
                            launch(Dispatchers.Main) {
                                kernelSuppressed = result.first
                                kernelStatus = result.second
                            }
                        }
                    },
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, null)
                    Text(if (kernelSuppressed) "Turn Kernel Logging ON" else "Turn Kernel Logging OFF")
                }

                if (source == LogSource.APP) {
                    OutlinedTextField(
                        value = appPackage,
                        onValueChange = { appPackage = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Package name") },
                        placeholder = { Text("com.example.app") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search / filter") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (paused) "SDKM capture paused" else "SDKM capture active", color = MaterialTheme.colorScheme.primary)
                    Text("${lines.size} lines")
                }
                Text(
                    kernelStatus,
                    color = if (kernelSuppressed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(lines) { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Refresh:")
                    listOf(250L, 1000L, 2000L, 5000L).forEach { ms ->
                        TextButton(onClick = { refreshMs = ms }) { Text(if (ms < 1000) "${ms}ms" else "${ms / 1000}s") }
                    }
                }
                Text(
                    "Kernel log OFF suppresses kernel printk output at runtime using root (dmesg -D + printk level 0). Linux may still keep messages in the kernel ring buffer; fully removing printk requires kernel-level support. Android logcat is separate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Clear log buffers?") },
            text = { Text("This clears the Android logcat buffers and the kernel ring buffer using root. It does not permanently disable logging.") },
            confirmButton = {
                TextButton(onClick = {
                    clearDialog = false
                    scope.launch(Dispatchers.IO) {
                        val result = LogsUtils.clearBuffers()
                        launch(Dispatchers.Main) { lines = if (result.first) emptyList() else listOf(result.second) }
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancel") } },
        )
    }
}

private object LogsUtils {
    private const val PREFS = "sdkm_logs"
    private const val KEY_PRINTK = "saved_printk"
    private const val KEY_DEVKMSG = "saved_devkmsg"

    private fun exec(vararg commands: String): Shell.Result {
        return Shell.cmd(*commands).exec()
    }

    fun isRootAvailable(): Boolean {
        return exec("id").isSuccess && exec("id").out.joinToString(" ").contains("uid=0")
    }

    fun isKernelSuppressed(): Boolean {
        val printk = readFile("/proc/sys/kernel/printk")?.trim()?.split(Regex("\\s+"))?.firstOrNull()
        return printk == "0"
    }

    fun suppressKernelLogging(): Pair<Boolean, String> {
        if (!isRootAvailable()) return false to "Root access is required"
        val savedPrintk = readFile("/proc/sys/kernel/printk")?.trim()
        val savedDevKmsg = readFile("/proc/sys/kernel/printk_devkmsg")?.trim()
        val prefs = AppContextHolder.context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        if (!savedPrintk.isNullOrBlank()) prefs.edit().putString(KEY_PRINTK, savedPrintk).apply()
        if (!savedDevKmsg.isNullOrBlank()) prefs.edit().putString(KEY_DEVKMSG, savedDevKmsg).apply()

        val r = exec(
            "dmesg -D",
            "printf '0\\n' > /proc/sys/kernel/printk",
            "printf 'off\\n' > /proc/sys/kernel/printk_devkmsg",
        )
        return if (r.isSuccess) true to "Kernel printk suppressed (runtime)"
        else false to "Kernel suppression failed: ${r.err.joinToString(" ").ifBlank { "permission denied" }}"
    }

    fun restoreKernelLogging(): Pair<Boolean, String> {
        if (!isRootAvailable()) return false to "Root access is required"
        val prefs = AppContextHolder.context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val savedPrintk = prefs.getString(KEY_PRINTK, null)
        val savedDevKmsg = prefs.getString(KEY_DEVKMSG, null)
        val printk = savedPrintk ?: "4 4 1 7"
        val commands = mutableListOf("printf '${printk.replace("'", "'\\''")}\\n' > /proc/sys/kernel/printk", "dmesg -E")
        if (!savedDevKmsg.isNullOrBlank()) commands += "printf '${savedDevKmsg.replace("'", "'\\''")}\\n' > /proc/sys/kernel/printk_devkmsg"
        val r = exec(*commands.toTypedArray())
        return if (r.isSuccess) false to "Kernel printk restored"
        else false to "Kernel restore failed: ${r.err.joinToString(" ").ifBlank { "permission denied" }}"
    }

    private fun readFile(path: String): String? {
        val r = exec("cat ${path.replace(" ", "\\ ")}")
        return if (r.isSuccess && r.out.isNotEmpty()) r.out.joinToString(" ").trim() else null
    }

    fun kernelLog(): List<String> {
        if (!isRootAvailable()) return listOf("Root access is required to read the kernel log.")
        val commands = listOf(
            "dmesg --color=never 2>/dev/null",
            "toybox dmesg 2>/dev/null",
            "cat /dev/kmsg 2>/dev/null | tail -n 1200",
            "cat /proc/kmsg 2>/dev/null | tail -n 1200",
        )
        for (command in commands) {
            val r = exec(command)
            if (r.isSuccess && r.out.any { it.isNotBlank() }) return r.out.filter { it.isNotBlank() }.takeLast(1200)
        }
        val detail = exec("dmesg --color=never 2>&1").err.joinToString(" ").trim()
        return listOf(if (detail.isBlank()) "Kernel log could not be read even with root access." else "Kernel log read failed: $detail")
    }

    fun logcat(): List<String> {
        if (!isRootAvailable()) return listOf("Root access is required to read Android logcat.")
        val commands = listOf(
            "logcat -d -v threadtime -t 1200 2>/dev/null",
            "toybox logcat -d -v threadtime -t 1200 2>/dev/null",
        )
        for (command in commands) {
            val r = exec(command)
            if (r.isSuccess && r.out.any { it.isNotBlank() }) return r.out.filter { it.isNotBlank() }.takeLast(1200)
        }
        return listOf("Unable to read Android logcat with root access.")
    }

    fun appLog(packageName: String): List<String> {
        val pkg = packageName.trim()
        if (!Regex("^[A-Za-z0-9_.]+$").matches(pkg)) return listOf("Enter a valid package name")
        if (!isRootAvailable()) return listOf("Root access is required to read app logs.")

        val pidResult = exec("pidof $pkg 2>/dev/null")
        val pids = pidResult.out.flatMap { it.trim().split(Regex("\\s+")) }.filter { it.all(Char::isDigit) && it.isNotEmpty() }
        if (pids.isNotEmpty()) {
            val r = exec("logcat -d -v threadtime --pid=${pids.first()} -t 1200 2>/dev/null")
            if (r.isSuccess && r.out.any { it.isNotBlank() }) return r.out.filter { it.isNotBlank() }.takeLast(1200)
        }

        val r = exec("logcat -d -v threadtime -t 3000 2>/dev/null | grep -i -- '$pkg'")
        return if (r.isSuccess && r.out.isNotEmpty()) r.out.takeLast(1200)
        else listOf("No log lines found for $pkg. The app may not currently have a running process or matching log entries.")
    }

    fun clearBuffers(): Pair<Boolean, String> {
        if (!isRootAvailable()) return false to "Root access is required"
        val logcat = exec("logcat -c")
        val dmesg = exec("dmesg -c >/dev/null 2>&1")
        return if (logcat.isSuccess && dmesg.isSuccess) true to "Logcat and kernel buffers cleared"
        else false to "Clear failed: ${(logcat.err + dmesg.err).joinToString(" ").ifBlank { "permission denied or unsupported by kernel" }}"
    }
}
