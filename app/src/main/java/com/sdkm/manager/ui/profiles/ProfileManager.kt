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
    private const val PREFS = "profile_state"
    private const val KEY_CURRENT = "current_profile"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val staticPaths = listOf(
        // Memory
        "/proc/sys/vm/swappiness",
        "/proc/sys/vm/page-cluster",
        "/proc/sys/vm/vfs_cache_pressure",
        "/proc/sys/vm/extra_free_kbytes",
        "/proc/sys/vm/watermark_scale_factor",
        "/proc/sys/vm/dirty_ratio",
        "/proc/sys/vm/dirty_background_ratio",
        // CPU boost/governor controls
        "/sys/module/ged/parameters/enable_cpu_boost",
        "/sys/module/ged/parameters/gx_force_cpu_boost",
        "/sys/module/ged/parameters/boost_upper_bound",
        "/sys/module/ged/parameters/deboost_reduce",
        // GPU controls
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

    private val builtinProfileFiles = listOf(
        "gaming.json" to "Gaming",
        "performance.json" to "Performance",
        "balance.json" to "Balance",
        "battery_saver.json" to "Battery Saver",
        "ultra_power_saver.json" to "Ultra Power Saver",
    )

    fun builtinProfiles(context: Context): List<KernelProfile> = builtinProfileFiles.mapNotNull { (fileName, _) ->
        runCatching {
            val content = context.assets.open("builtin_profiles/$fileName").bufferedReader().use { it.readText() }
            json.decodeFromString<KernelProfile>(content)
        }.getOrNull()
    }

    fun list(context: Context): List<KernelProfile> = profilesDir(context).listFiles()
        ?.filter { it.extension == "json" }
        ?.mapNotNull { runCatching { json.decodeFromString<KernelProfile>(it.readText()) }.getOrNull() }
        ?.sortedByDescending { it.createdAt }
        ?: emptyList()

    fun fileFor(context: Context, profile: KernelProfile): File = File(profilesDir(context), "${safeName(profile.name)}.json")

    fun captureCurrent(name: String): KernelProfile? {
        val paths = LinkedHashSet(staticPaths)
        paths.addAll(dynamicCpuPaths())
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

    fun restore(context: Context, profile: KernelProfile): ApplyResult {
        val result = apply(profile.settings)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CURRENT, profile.name).apply()
        return result
    }

    fun currentProfileName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CURRENT, null)

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

        // Always bring every CPU core online first. This is required so that
        // policy/governor settings can be applied even when a profile later
        // requests some cores to be disabled.
        val onlinePaths = settings.keys
            .filter { it.matches(Regex("/sys/devices/system/cpu/cpu[0-9]+/online")) }
        onlinePaths.forEach { path ->
            val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' '1' > ${q(path)}").exec()
            if (result.isSuccess) success++ else failed++
        }

        // Apply governor settings before the remaining settings. If any
        // requested governor cannot be written, fall back to schedutil for
        // every governor path present in this profile (little + big clusters).
        val governorEntries = settings.filterKeys { it.endsWith("/scaling_governor") }
        var governorFailed = false
        governorEntries.forEach { (path, value) ->
            val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' ${q(value)} > ${q(path)}").exec()
            if (result.isSuccess) success++ else { failed++; governorFailed = true }
        }
        if (governorFailed) {
            governorEntries.keys.forEach { path ->
                val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' 'schedutil' > ${q(path)}").exec()
                if (result.isSuccess) {
                    // A failed requested governor is recovered by schedutil;
                    // do not count the fallback as an additional failure.
                    if (failed > 0) failed--
                    success++
                }
            }
        }

        // Apply all non-governor, non-CPU-online settings next.
        settings.forEach { (path, value) ->
            if (path.endsWith("/scaling_governor") || path.matches(Regex("/sys/devices/system/cpu/cpu[0-9]+/online"))) return@forEach
            val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' ${q(value)} > ${q(path)}").exec()
            if (result.isSuccess) success++ else failed++
        }

        // Finally honor explicit CPU-offline requests from the JSON profile.
        settings.filterKeys { it.matches(Regex("/sys/devices/system/cpu/cpu[0-9]+/online")) }
            .filterValues { it.trim() == "0" }
            .forEach { (path, value) ->
                val result = Shell.cmd("[ -e ${q(path)} ] && printf '%s' '0' > ${q(path)}").exec()
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

            // Bring all CPUs online before applying policy/governor settings.
            appendLine("for c in /sys/devices/system/cpu/cpu[0-9]*/online; do [ -e \"${'$'}c\" ] && echo 1 > \"${'$'}c\" 2>/dev/null; done")

            val governorEntries = profile.settings.filterKeys { it.endsWith("/scaling_governor") }
            val otherEntries = profile.settings.filterKeys {
                !it.endsWith("/scaling_governor") && !it.matches(Regex("/sys/devices/system/cpu/cpu[0-9]+/online"))
            }
            appendLine("gov_failed=0")
            governorEntries.forEach { (path, value) ->
                val encoded = android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP)
                appendLine("if [ -e ${q(path)} ]; then if ! (echo '$encoded' | base64 -d | cat > ${q(path)} 2>/dev/null); then gov_failed=1; fi; fi")
            }
            // If any requested governor failed, fall back to schedutil for
            // every available CPU policy (little + big clusters).
            appendLine("if [ \"${'$'}gov_failed\" -eq 1 ]; then for g in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do [ -e \"${'$'}g\" ] && echo schedutil > \"${'$'}g\" 2>/dev/null; done; fi")
            otherEntries.forEach { (path, value) ->
                val encoded = android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP)
                appendLine("if [ -e ${q(path)} ]; then echo '$encoded' | base64 -d | cat > ${q(path)} 2>/dev/null; fi")
            }
            profile.settings.filterKeys { it.matches(Regex("/sys/devices/system/cpu/cpu[0-9]+/online")) }
                .filterValues { it.trim() == "0" }
                .forEach { (path, _) ->
                    appendLine("if [ -e ${q(path)} ]; then echo 0 > ${q(path)} 2>/dev/null; fi")
                }
        }
        return lines
    }

    private fun dynamicCpuPaths(): List<String> {
        return Shell.cmd(
            "for p in /sys/devices/system/cpu/cpufreq/policy*; do for f in scaling_min_freq scaling_max_freq scaling_governor; do [ -e \"${'$'}p/${'$'}f\" ] && echo \"${'$'}p/${'$'}f\"; done; done; " +
                "for c in /sys/devices/system/cpu/cpu[0-9]*; do [ -e \"${'$'}c/online\" ] && echo \"${'$'}c/online\"; done"
        ).exec().out.orEmpty()
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
