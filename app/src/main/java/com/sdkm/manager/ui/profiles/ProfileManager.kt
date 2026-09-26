package com.sdkm.manager.ui.profiles

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@Serializable
data class KernelProfile(
    val name: String,
    val createdAt: Long,
    val settings: Map<String, String>,
)

object ProfileManager {
    private const val BOOT_SCRIPT = "/data/adb/service.d/sdkm_profile.sh"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val staticPaths = listOf(
        "/proc/sys/kernel/sched_autogroup_enabled",
        "/proc/sys/kernel/sched_lib_name",
        "/proc/sys/kernel/sched_util_clamp_max",
        "/proc/sys/kernel/sched_util_clamp_min",
        "/proc/sys/kernel/sched_util_clamp_min_rt_default",
        "/proc/sys/kernel/random/read_wakeup_threshold",
        "/proc/sys/kernel/random/write_wakeup_threshold",
        "/proc/sys/kernel/printk",
        "/proc/sys/vm/swappiness",
        "/proc/sys/vm/page-cluster",
        "/proc/sys/vm/vfs_cache_pressure",
        "/proc/sys/vm/extra_free_kbytes",
        "/proc/sys/vm/watermark_scale_factor",
        "/proc/sys/vm/dirty_ratio",
        "/proc/sys/vm/dirty_background_ratio",
        "/proc/sys/net/ipv4/tcp_congestion_control",
        "/proc/sys/net/core/rmem_max",
        "/proc/sys/net/core/wmem_max",
        "/proc/sys/net/ipv4/tcp_rmem",
        "/proc/sys/net/ipv4/tcp_wmem",
        "/proc/sys/fs/file-max",
        "/proc/sys/fs/inotify/max_user_watches",
        "/proc/sys/fs/inotify/max_user_instances",
        "/proc/sys/fs/pipe-max-size",
        "/sys/module/ged/parameters/enable_cpu_boost",
        "/sys/module/ged/parameters/gx_force_cpu_boost",
        "/sys/module/ged/parameters/boost_upper_bound",
        "/sys/module/ged/parameters/deboost_reduce",
        "/sys/module/ged/parameters/enable_gpu_boost",
        "/sys/module/ged/parameters/boost_gpu_enable",
        "/sys/module/ged/parameters/ged_boost_enable",
        "/sys/module/ged/parameters/gpu_dvfs_enable",
        "/sys/kernel/ged/hal/custom_boost_gpu_freq",
        "/sys/kernel/ged/hal/custom_upbound_gpu_freq",
        "/sys/module/ged/parameters/gpu_cust_boost_freq",
        "/sys/module/ged/parameters/gpu_cust_upbound_freq",
        "/sys/module/ged/parameters/gpu_bottom_freq",
    )

    fun profilesDir(context: Context): File = File(context.filesDir, "kernel_profiles").apply { mkdirs() }

    fun list(context: Context): List<KernelProfile> = profilesDir(context).listFiles()
        ?.filter { it.extension == "json" }
        ?.mapNotNull { runCatching { json.decodeFromString<KernelProfile>(it.readText()) }.getOrNull() }
        ?.sortedByDescending { it.createdAt }
        ?: emptyList()

    fun fileFor(context: Context, profile: KernelProfile): File = File(profilesDir(context), "${safeName(profile.name)}.json")

    fun captureCurrent(name: String): KernelProfile? {
        val paths = LinkedHashSet(staticPaths)
        paths.addAll(dynamicCpuPaths())
        paths.addAll(dynamicBlockPaths())
        val settings = LinkedHashMap<String, String>()
        for (path in paths) {
            val value = read(path) ?: continue
            if (value.isNotBlank()) settings[path] = value.trim()
        }
        if (settings.isEmpty()) return null
        return KernelProfile(name.trim(), System.currentTimeMillis(), settings)
    }

    fun save(context: Context, profile: KernelProfile): Boolean = runCatching {
        fileFor(context, profile).writeText(json.encodeToString(profile))
        true
    }.getOrDefault(false)

    fun delete(context: Context, profile: KernelProfile): Boolean = fileFor(context, profile).delete()

    fun export(profile: KernelProfile): String = json.encodeToString(profile)

    fun import(context: Context, content: String): KernelProfile? = runCatching {
        json.decodeFromString<KernelProfile>(content).also { save(context, it) }
    }.getOrNull()

    fun restore(profile: KernelProfile): ApplyResult = apply(profile.settings)

    fun setBootProfile(profile: KernelProfile): Boolean {
        val script = buildBootScript(profile)
        val encoded = android.util.Base64.encodeToString(script.toByteArray(), android.util.Base64.NO_WRAP)
        val cmd = "mkdir -p /data/adb/service.d && echo '$encoded' | base64 -d > '$BOOT_SCRIPT' && chmod 755 '$BOOT_SCRIPT'"
        return Shell.cmd(cmd).exec().isSuccess
    }

    fun disableBootProfile(): Boolean = Shell.cmd("rm -f '$BOOT_SCRIPT'").exec().isSuccess

    fun isBootEnabled(): Boolean = Shell.cmd("[ -f '$BOOT_SCRIPT' ]").exec().isSuccess

    fun bootProfileName(): String? = runCatching {
        Shell.cmd("grep -m1 '^# SDKM kernel profile:' '$BOOT_SCRIPT' 2>/dev/null").exec().out.orEmpty()
            .firstOrNull()?.removePrefix("# SDKM kernel profile:")?.trim()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun apply(settings: Map<String, String>): ApplyResult {
        var success = 0
        var failed = 0
        settings.forEach { (path, value) ->
            val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' ${q(value)} > ${q(path)}").exec()
            if (result.isSuccess) success++ else failed++
        }
        return ApplyResult(success, failed)
    }

    private fun buildBootScript(profile: KernelProfile): String {
        val lines = buildString {
            appendLine("#!/system/bin/sh")
            appendLine("# SDKM kernel profile: ${profile.name.replace("\n", " ")}")
            appendLine("# Generated by SDKM. Runs as KernelSU service.d at boot.")
            appendLine("sleep 8")
            profile.settings.forEach { (path, value) ->
                val encoded = android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP)
                appendLine("if [ -e ${q(path)} ]; then echo '$encoded' | base64 -d | cat > ${q(path)} 2>/dev/null; fi")
            }
        }
        return lines
    }

    private fun dynamicCpuPaths(): List<String> {
        return Shell.cmd("for p in /sys/devices/system/cpu/cpufreq/policy*; do for f in scaling_min_freq scaling_max_freq scaling_governor; do [ -e \"${'$'}p/${'$'}f\" ] && echo \"${'$'}p/${'$'}f\"; done; done").exec().out.orEmpty()
    }

    private fun dynamicBlockPaths(): List<String> {
        return Shell.cmd("for d in /sys/block/*; do [ -e \"${'$'}d/queue/scheduler\" ] && echo \"${'$'}d/queue/scheduler\"; [ -e \"${'$'}d/queue/read_ahead_kb\" ] && echo \"${'$'}d/queue/read_ahead_kb\"; done").exec().out.orEmpty()
    }

    private fun read(path: String): String? = runCatching {
        Shell.cmd("cat ${q(path)} 2>/dev/null").exec().out.orEmpty().joinToString("\n").trim().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun safeName(name: String): String = name.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").take(64).ifBlank { "profile" }
    private fun q(value: String) = "'" + value.replace("'", "'\\''") + "'"

    data class ApplyResult(val success: Int, val failed: Int)
}
