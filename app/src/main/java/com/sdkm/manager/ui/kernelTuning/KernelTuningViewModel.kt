package com.sdkm.manager.ui.kernelTuning

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell

 data class KernelTuneParam(val path: String, val label: String, val value: String)
 data class BlockDeviceState(val name: String, val scheduler: String, val availableSchedulers: List<String>, val readAhead: String)
 data class KernelTuningState(
    val devices: List<BlockDeviceState> = emptyList(),
    val vm: List<KernelTuneParam> = emptyList(),
    val entropy: List<KernelTuneParam> = emptyList(),
    val network: List<KernelTuneParam> = emptyList(),
    val filesystem: List<KernelTuneParam> = emptyList(),
 )

class KernelTuningViewModel : ViewModel() {
    val state = mutableStateOf(KernelTuningState())
    val input = mutableStateMapOf<String, String>()

    fun refresh() {
        val devices = shell("for d in /sys/block/*; do n=\"${'$'}{d##*/}\"; [ -f ${'$'}d/queue/scheduler ] || continue; sched=${'$'}(cat ${'$'}d/queue/scheduler 2>/dev/null); ra=${'$'}(cat ${'$'}d/queue/read_ahead_kb 2>/dev/null); echo \"DEV|${'$'}n|${'$'}sched|${'$'}ra\"; done")
            .mapNotNull { line ->
                val p = line.split('|', limit = 4)
                if (p.size == 4) {
                    val sched = p[2].trim()
                    val available = sched.replace("[", "").replace("]", "").split(Regex("\\s+")).filter { it.isNotBlank() }
                    val current = available.firstOrNull { sched.contains("[$it]") } ?: available.firstOrNull().orEmpty()
                    BlockDeviceState(p[1], current, available, p[3].trim())
                } else null
            }
        state.value = KernelTuningState(
            devices = devices,
            vm = params(listOf(
                "/proc/sys/vm/swappiness" to "swappiness",
                "/proc/sys/vm/vfs_cache_pressure" to "vfs_cache_pressure",
                "/proc/sys/vm/page-cluster" to "page-cluster",
                "/proc/sys/vm/dirty_ratio" to "dirty_ratio",
                "/proc/sys/vm/dirty_background_ratio" to "dirty_background_ratio",
                "/proc/sys/vm/watermark_scale_factor" to "watermark_scale_factor",
            ), { it }),
            entropy = emptyList(),
            network = params(listOf(
                "/proc/sys/net/ipv4/tcp_congestion_control" to "tcp_congestion_control",
            ), { it }),
            filesystem = emptyList(),
        )
    }

    private fun params(items: List<Pair<String,String>>, transform: (String)->String): List<KernelTuneParam> = items.mapNotNull { (path,label) ->
        val v = shell("cat ${q(path)} 2>/dev/null").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        KernelTuneParam(path, label, transform(v))
    }

    fun write(path: String, value: String) {
        if (value.isBlank() || value.contains('\n') || value.contains(';')) return
        Shell.cmd("printf '%s' ${q(value)} > ${q(path)}").exec()
        refresh()
    }

    fun setScheduler(device: String, scheduler: String) {
        if (!device.matches(Regex("[A-Za-z0-9_.-]+")) || !scheduler.matches(Regex("[A-Za-z0-9_.-]+"))) return
        write("/sys/block/$device/queue/scheduler", scheduler)
    }

    private fun shell(command: String): List<String> = runCatching { Shell.cmd(command).exec().out.orEmpty() }.getOrDefault(emptyList())
    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"
}
