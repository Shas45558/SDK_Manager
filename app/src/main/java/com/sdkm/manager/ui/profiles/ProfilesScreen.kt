package com.sdkm.manager.ui.profiles

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sdkm.manager.ui.components.SimpleTopAppBar
import com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfilesScreen() {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(ProfileManager.list(context)) }
    var bootProfile by remember { mutableStateOf<String?>(null) }
    var showName by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf<KernelProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedForExport by remember { mutableStateOf<KernelProfile?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val profile = selectedForExport
        if (uri != null && profile != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(ProfileManager.export(profile).toByteArray()) } }
                .onSuccess { message = "Profile exported" }
                .onFailure { message = "Export failed: ${it.message}" }
        }
        selectedForExport = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Unable to read file") }
                .mapCatching { ProfileManager.import(context, it) ?: error("Invalid SDKM profile") }
                .onSuccess { profiles = ProfileManager.list(context); message = "Imported: ${it.name}" }
                .onFailure { message = "Import failed: ${it.message}" }
        }
    }

    LaunchedEffect(Unit) {
        bootProfile = withContext(Dispatchers.IO) { ProfileManager.bootProfileName() }
    }

    SDKMStandaloneDrawerHost {
        Column(Modifier.fillMaxSize()) {
            SimpleTopAppBar("Backup / Profiles")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Kernel settings profiles", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Save and restore only CPU, GPU, and Memory page settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showName = true }, modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.Icon(Icons.Rounded.Backup, null)
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            Text("Save current")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.Icon(Icons.Rounded.FileUpload, null)
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            Text("Import")
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                if (profiles.isEmpty()) {
                    item { Text("No saved profiles yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(profiles, key = { "${it.name}-${it.createdAt}" }) { profile ->
                    ProfileCard(
                        profile = profile,
                        bootEnabled = bootProfile == profile.name,
                        onRestore = {
                            val result = ProfileManager.restore(profile)
                            message = "Restored ${result.success} settings${if (result.failed > 0) "; ${result.failed} failed" else ""}"
                        },
                        onExport = { selectedForExport = profile; exportLauncher.launch("${profile.name}.json") },
                        onDelete = { showDelete = profile },
                        onBootChange = { enabled ->
                            if (enabled) {
                                val ok = ProfileManager.setBootProfile(profile)
                                if (ok) bootProfile = profile.name
                                message = if (ok) "${profile.name} will apply at boot" else "Could not install KernelSU boot profile"
                            } else {
                                val ok = ProfileManager.disableBootProfile()
                                if (ok) bootProfile = null
                                message = if (ok) "Apply at boot disabled" else "Could not disable boot profile"
                            }
                        },
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Boot profiles use KernelSU /data/adb/service.d and run as root after boot. ZRAM disk size is intentionally not changed by profiles because resizing active ZRAM requires a separate reset/swap sequence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showName) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showName = false },
            title = { Text("Save current kernel settings") },
            text = {
                androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = name.trim().ifBlank { "Profile ${profiles.size + 1}" }
                    val profile = ProfileManager.captureCurrent(clean)
                    message = if (profile != null && ProfileManager.save(context, profile)) "Saved ${profile.settings.size} settings" else "Could not read kernel settings (root required)"
                    profiles = ProfileManager.list(context)
                    showName = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showName = false }) { Text("Cancel") } },
        )
    }

    showDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDelete = null },
            title = { Text("Delete profile?") },
            text = { Text(profile.name) },
            confirmButton = { TextButton(onClick = { ProfileManager.delete(context, profile); profiles = ProfileManager.list(context); showDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = null }) { Text("Cancel") } },
        )
    }

    message?.let { msg ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Backup / Profiles") }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}

@Composable
private fun ProfileCard(
    profile: KernelProfile,
    bootEnabled: Boolean,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onBootChange: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            Text("${profile.settings.size} settings • ${DateFormat.getDateTimeInstance().format(Date(profile.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Apply settings at boot", modifier = Modifier.weight(1f))
                Switch(checked = bootEnabled, onCheckedChange = onBootChange)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { androidx.compose.material3.Icon(Icons.Rounded.Restore, null); Spacer(Modifier.padding(horizontal = 2.dp)); Text("Restore") }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { androidx.compose.material3.Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.padding(horizontal = 2.dp)); Text("Export") }
                OutlinedButton(onClick = onDelete) { androidx.compose.material3.Icon(Icons.Rounded.Delete, null) }
            }
        }
    }
}
