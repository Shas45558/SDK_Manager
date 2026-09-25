from pathlib import Path
p=Path('/mnt/data/fixwork/app/src/main/java/com/sdkm/manager/ui/components/AppBar.kt')
s=p.read_text()
s=s.replace('import androidx.compose.runtime.Composable\n', 'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.CompositionLocalProvider\nimport androidx.compose.runtime.staticCompositionLocalOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.rememberCoroutineScope\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.setValue\n')
s=s.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.Row\nimport androidx.compose.foundation.layout.Spacer\nimport androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.clickable\nimport androidx.compose.material.icons.rounded.DeleteSweep\nimport androidx.compose.material.icons.rounded.DeveloperBoard\nimport androidx.compose.material.icons.rounded.ExitToApp\nimport androidx.compose.material.icons.rounded.Home\nimport androidx.compose.material.icons.rounded.Info\nimport androidx.compose.material.icons.rounded.Memory\nimport androidx.compose.material.icons.rounded.MonitorHeart\nimport androidx.compose.material.icons.rounded.RestartAlt\nimport androidx.compose.material.icons.rounded.Settings\nimport androidx.compose.material.icons.rounded.Tune\nimport androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.ButtonDefaults\nimport androidx.compose.material3.Card\nimport androidx.compose.material3.CardDefaults\nimport androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.ModalDrawerSheet\nimport androidx.compose.material3.ModalNavigationDrawer\nimport androidx.compose.material3.Surface\nimport androidx.compose.material3.TextButton\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.ui.graphics.vector.ImageVector\n')
s=s.replace('import com.sdkm.manager.ui.navigation.LocalSDKMDrawer\n', 'import com.sdkm.manager.ui.navigation.LocalSDKMDrawer\nimport com.sdkm.manager.ui.navigation.HomeRoute\nimport com.sdkm.manager.ui.navigation.CpuRoute\nimport com.sdkm.manager.ui.navigation.GpuRoute\nimport com.sdkm.manager.ui.navigation.BatteryRoute\nimport com.sdkm.manager.ui.navigation.KernelSettingsRoute\nimport com.sdkm.manager.ui.home.HomeViewModel\nimport com.sdkm.manager.ui.monitor.GameMonitorService\nimport com.sdkm.manager.ui.settings.SettingsActivity\nimport com.sdkm.manager.ui.taskKiller.TaskKillerActivity\nimport com.sdkm.manager.utils.Utils\nimport androidx.core.content.ContextCompat\nimport android.net.Uri\nimport android.provider.Settings\nimport kotlinx.coroutines.launch\n')
# Replace standalone function
start=s.index('@Composable\nfun SDKMStandaloneHamburgerMenu()')
end=s.index('\n@Composable\nfun TopAppBarWithBackButton', start)
new=r'''val LocalStandaloneDrawer = staticCompositionLocalOf<() -> Unit> { { } }

@Composable
fun SDKMStandaloneDrawerHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val deviceInfo by homeViewModel.deviceInfo.collectAsStateWithLifecycle()
    val appVersion by homeViewModel.appVersion.collectAsStateWithLifecycle()
    var showReboot by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadDeviceInfo(context)
        homeViewModel.loadAppVersion(context)
    }

    fun openMain(route: String) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_START_ROUTE, route)
        )
        (context as? android.app.Activity)?.finish()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
            ) {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("SDKM", style = MaterialTheme.typography.titleLarge)
                        Text("Kernel & System Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                StandaloneDrawerItem("Home", Icons.Rounded.Home) { scope.launch { drawerState.close(); openMain(HomeRoute) } }
                StandaloneDrawerItem("CPU", Icons.Rounded.Memory) { scope.launch { drawerState.close(); openMain(CpuRoute) } }
                StandaloneDrawerItem("GPU", Icons.Rounded.DeveloperBoard) { scope.launch { drawerState.close(); openMain(GpuRoute) } }
                StandaloneDrawerItem("Battery", Icons.Rounded.Memory) { scope.launch { drawerState.close(); openMain(BatteryRoute) } }
                StandaloneDrawerItem("Monitor", Icons.Rounded.MonitorHeart) {
                    scope.launch { drawerState.close() }
                    if (Settings.canDrawOverlays(context)) {
                        ContextCompat.startForegroundService(context, Intent(context, GameMonitorService::class.java).setAction(GameMonitorService.ACTION_START))
                    } else {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    }
                }
                StandaloneDrawerItem("Task Killer", Icons.Rounded.DeleteSweep) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, TaskKillerActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                }
                StandaloneDrawerItem("Kernel Settings", Icons.Rounded.Memory) { scope.launch { drawerState.close(); openMain(KernelSettingsRoute) } }
                StandaloneDrawerItem("Settings", Icons.Rounded.Settings) {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                }
                StandaloneDrawerItem("Reboot", Icons.Rounded.RestartAlt) { scope.launch { drawerState.close(); showReboot = true } }
                StandaloneDrawerItem("About", Icons.Rounded.Info) { scope.launch { drawerState.close(); showAbout = true } }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                StandaloneDrawerItem("Exit", Icons.Rounded.ExitToApp) { (context as? android.app.Activity)?.finish() }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        CompositionLocalProvider(LocalStandaloneDrawer provides { scope.launch { drawerState.open() } }) {
            content()
        }
    }

    if (showReboot) {
        AlertDialog(
            onDismissRequest = { showReboot = false },
            title = { Text("Reboot") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(Modifier.fillMaxWidth(), onClick = { showReboot = false; Utils.reboot("") }) { Text("Normal") }
                Button(Modifier.fillMaxWidth(), onClick = { showReboot = false; Utils.reboot("recovery") }) { Text("Recovery") }
                Button(Modifier.fillMaxWidth(), onClick = { showReboot = false; Utils.reboot("bootloader") }) { Text("Bootloader") }
            } },
            confirmButton = { TextButton(onClick = { showReboot = false }) { Text("Cancel") } },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About") },
            text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SDKM About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Version: $appVersion")
                Button(Modifier.fillMaxWidth(), onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Shas45558/SDK_Manager"))) } }) { Text("Source") }
                Button(Modifier.fillMaxWidth(), onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ocmt6768"))) } }) { Text("Telegram") }
                Text("System About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Device: ${deviceInfo.manufacturer} ${deviceInfo.deviceName}")
                Text("Android: ${deviceInfo.androidVersion}")
                Text("Kernel: ${deviceInfo.fullKernelVersion.ifBlank { deviceInfo.kernelVersion }}")
            } },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun StandaloneDrawerItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SDKMStandaloneHamburgerMenu() {
    IconButton(onClick = LocalStandaloneDrawer.current) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
    }
}
'''
s=s[:start]+new+s[end:]
# add imports for lifecycle collect
s=s.replace('import androidx.compose.runtime.setValue\n', 'import androidx.compose.runtime.setValue\nimport androidx.compose.runtime.LaunchedEffect\n')
s=s.replace('import androidx.lifecycle.viewmodel.compose.viewModel\n', 'import androidx.lifecycle.viewmodel.compose.viewModel\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\n') if 'import androidx.lifecycle.viewmodel.compose.viewModel\n' in s else s
p.write_text(s)

# wrap SettingsScreen
p=Path('/mnt/data/fixwork/app/src/main/java/com/sdkm/manager/ui/settings/SettingsScreen.kt')
s=p.read_text()
s=s.replace('import com.sdkm.manager.ui.components.ListItem\n', 'import com.sdkm.manager.ui.components.ListItem\nimport com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost\n')
# if import line absent, insert before SDKMStandalone
if 'SDKMStandaloneDrawerHost' not in s.split('import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu')[0]:
    s=s.replace('import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu\n', 'import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu\nimport com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost\n')
s=s.replace('    Scaffold(\n        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),', '    SDKMStandaloneDrawerHost {\n        Scaffold(\n        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),')
# close host after Scaffold block: locate final closing before function end. This is risky; add before last } of function by replace first occurrence near file end.
idx=s.rfind('\n    }\n}')
if idx!=-1: s=s[:idx]+'\n    }\n    }\n}'+s[idx+len('\n    }\n}'):] 
p.write_text(s)

# wrap TaskKillerScreen Scaffold content
p=Path('/mnt/data/fixwork/app/src/main/java/com/sdkm/manager/ui/taskKiller/TaskKillerActivity.kt')
s=p.read_text()
s=s.replace('import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu\n', 'import com.sdkm.manager.ui.components.SDKMStandaloneHamburgerMenu\nimport com.sdkm.manager.ui.components.SDKMStandaloneDrawerHost\n')
s=s.replace('    Scaffold(\n        topBar = {', '    SDKMStandaloneDrawerHost {\n    Scaffold(\n        topBar = {', 1)
# close wrapper after scaffold. Find last scaffold closing around function before file next function/data. Use known segment containerColor and closing.
needle='''        containerColor = MaterialTheme.colorScheme.surfaceContainer,\n    ) { padding ->'''
# no direct closing; need insert before function closing after LazyColumn. Find region from first SDKM host to next @Composable or data class.
start=s.index('    SDKMStandaloneDrawerHost {')
nextpos=s.find('\n}\n', start)
# first } is not function close likely inner lambda? find last occurrence before next top-level declaration
# use marker after LazyColumn end: locate '\n    }\n}\n' after start, choose last before EOF? 
end=s.find('\n}\n', start)
while end!=-1 and end < len(s):
    if s[end+3:end+4] in ['@','\n','']:
        # continue if likely function end; choose first pattern where following text starts top-level annotation
        rest=s[end+3:]
        if rest.startswith('@') or rest.startswith('data class') or rest.strip()=='' : break
    end=s.find('\n}\n', end+3)
if end!=-1:
    # insert wrapper close before function close
    s=s[:end]+'    }\n'+s[end:]
p.write_text(s)

# swappiness persistence
p=Path('/mnt/data/fixwork/app/src/main/java/com/sdkm/manager/ui/kernelParameter/KernelSettingsScreen.kt')
s=p.read_text()
s=s.replace('import androidx.compose.runtime.Composable\n', 'import androidx.compose.runtime.Composable\nimport androidx.compose.ui.platform.LocalContext\n')
s=s.replace('    val memory by viewModel.memory.collectAsStateWithLifecycle()\n\n    var swappiness by remember { mutableFloatStateOf(0f) }', '    val memory by viewModel.memory.collectAsStateWithLifecycle()\n    val context = LocalContext.current\n    val prefs = remember { context.getSharedPreferences("sdkm_kernel_settings", android.content.Context.MODE_PRIVATE) }\n    val savedSwappiness = remember { prefs.getInt("swappiness", -1) }\n\n    var swappiness by remember { mutableFloatStateOf(savedSwappiness.takeIf { it in 0..200 }?.toFloat() ?: 0f) }')
old='''    LaunchedEffect(memory.swappiness, memory.extraFreeKbytes, memory.pageCluster, memory.vfsCachePressure, memory.dirtyRatio, memory.dirtyBackgroundRatio) {\n        swappiness = memory.swappiness.toFloatOrNull()?.coerceIn(0f, 200f) ?: 0f'''
new='''    LaunchedEffect(memory.swappiness, memory.extraFreeKbytes, memory.pageCluster, memory.vfsCachePressure, memory.dirtyRatio, memory.dirtyBackgroundRatio) {\n        if (savedSwappiness !in 0..200) {\n            swappiness = memory.swappiness.toFloatOrNull()?.coerceIn(0f, 200f) ?: 0f\n        }'''
s=s.replace(old,new)
s=s.replace('onApply = { viewModel.setValue(KernelUtils.SWAPPINESS, swappiness.toInt().toString()) },', 'onApply = {\n                        val value = swappiness.toInt()\n                        prefs.edit().putInt("swappiness", value).apply()\n                        viewModel.setValue(KernelUtils.SWAPPINESS, value.toString())\n                    },')
p.write_text(s)
