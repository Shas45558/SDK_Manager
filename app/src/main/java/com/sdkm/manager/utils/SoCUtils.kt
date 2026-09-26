/*
 * Copyright (c) 2025 Rve <rve27github@gmail.com>
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

// Dear programmer:
// When I wrote this code, only god and
// I knew how it worked.
// Now, only god knows it!
//
// Therefore, if you are trying to optimize
// this routine and it fails (most surely),
// please increase this counter as a
// warning for the next person:
//
// total hours wasted here = 254
//
package com.sdkm.manager.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.sdkm.manager.R
import com.topjohnwu.superuser.Shell
import java.io.File

object SoCUtils {
    const val TAG = "SoCUtils"

    const val CPU_TEMP = "/sys/class/thermal/thermal_zone0/temp"

    const val MIN_FREQ_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq"
    const val MAX_FREQ_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"
    const val CURRENT_FREQ_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq"
    const val AVAILABLE_FREQ_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies"
    const val GOV_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor"
    const val AVAILABLE_GOV_CPU0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors"

    const val MIN_FREQ_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_min_freq"
    const val MAX_FREQ_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_max_freq"
    const val CURRENT_FREQ_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_cur_freq"
    const val AVAILABLE_FREQ_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_available_frequencies"
    const val AVAILABLE_BOOST_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_boost_frequencies"
    const val GOV_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_governor"
    const val AVAILABLE_GOV_CPU3 = "/sys/devices/system/cpu/cpufreq/policy3/scaling_available_governors"

    const val MIN_FREQ_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_min_freq"
    const val MAX_FREQ_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq"
    const val CURRENT_FREQ_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_cur_freq"
    const val AVAILABLE_FREQ_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_frequencies"
    const val AVAILABLE_BOOST_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_boost_frequencies"
    const val GOV_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor"
    const val AVAILABLE_GOV_CPU4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors"

    const val MIN_FREQ_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_min_freq"
    const val MAX_FREQ_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq"
    const val CURRENT_FREQ_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_cur_freq"
    const val AVAILABLE_FREQ_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_available_frequencies"
    const val AVAILABLE_BOOST_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_boost_frequencies"
    const val GOV_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_governor"
    const val AVAILABLE_GOV_CPU6 = "/sys/devices/system/cpu/cpufreq/policy6/scaling_available_governors"

    const val MIN_FREQ_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_min_freq"
    const val MAX_FREQ_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_max_freq"
    const val CURRENT_FREQ_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_cur_freq"
    const val AVAILABLE_FREQ_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_available_frequencies"
    const val GOV_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_governor"
    const val AVAILABLE_GOV_CPU7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_available_governors"

    const val CPU_INPUT_BOOST_MS = "/sys/devices/system/cpu/cpu_boost/input_boost_ms"

    data class CpuCoreState(val cpu: Int, val online: Boolean, val controllable: Boolean)

    data class CpuCoreMetric(
        val cpu: Int,
        val load: Int,
        val frequencyMHz: String,
    )

    private val previousCpuCoreStats = mutableMapOf<Int, Pair<Long, Long>>()

    fun readCpuCoreMetrics(): List<CpuCoreMetric> = runCatching {
        val statLines = Utils.readFile("/proc/stat").lineSequence()
            .filter { it.startsWith("cpu") && it.getOrNull(3)?.isDigit() == true }
            .toList()

        statLines.mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            val cpu = parts.firstOrNull()?.removePrefix("cpu")?.toIntOrNull() ?: return@mapNotNull null
            if (parts.size < 8) return@mapNotNull null
            val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
            if (values.size < 7) return@mapNotNull null
            val idle = values[3] + (values.getOrNull(4) ?: 0L)
            val total = values.sum()
            val previous = previousCpuCoreStats[cpu]
            val load = if (previous != null && total > previous.first) {
                val totalDelta = total - previous.first
                val idleDelta = (idle - previous.second).coerceAtLeast(0L)
                ((totalDelta - idleDelta).coerceAtLeast(0L) * 100L / totalDelta).toInt().coerceIn(0, 100)
            } else {
                0
            }
            previousCpuCoreStats[cpu] = total to idle

            val path = "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq"
            val raw = Shell.cmd("cat $path 2>/dev/null").exec().out.firstOrNull()?.trim()?.toLongOrNull()
            val mhz = when {
                raw == null || raw <= 0L -> "0"
                raw >= 10_000_000L -> raw / 1_000_000L
                else -> raw / 1_000L
            }
            CpuCoreMetric(cpu, load, mhz.toString())
        }.sortedBy { it.cpu }
    }.getOrElse {
        Log.e(TAG, "readCpuCoreMetrics: ${it.message}", it)
        emptyList()
    }

    fun readCpuCoreStates(): List<CpuCoreState> = runCatching {
        val cpuDirs = File("/sys/devices/system/cpu").listFiles()
            ?.mapNotNull { file ->
                val n = file.name.removePrefix("cpu").toIntOrNull()
                if (file.isDirectory && n != null) n else null
            }
            ?.sorted()
            ?: emptyList()

        cpuDirs.map { cpu ->
            val onlineFile = File("/sys/devices/system/cpu/cpu$cpu/online")
            val online = if (cpu == 0 && !onlineFile.exists()) {
                true
            } else {
                onlineFile.readText().trim() == "1"
            }
            CpuCoreState(cpu, online, cpu != 0 && isCpuCoreOnlineWritable(cpu))
        }
    }.getOrElse {
        Log.e(TAG, "readCpuCoreStates: ${it.message}", it)
        emptyList()
    }

    fun isCpuCoreOnlineWritable(cpu: Int): Boolean = runCatching {
        val path = "/sys/devices/system/cpu/cpu$cpu/online"
        File(path).exists() && Shell.cmd("test -w $path").exec().isSuccess
    }.getOrDefault(false)

    fun setCpuCoreOnline(cpu: Int, online: Boolean): Boolean = runCatching {
        if (cpu == 0) return false
        val path = "/sys/devices/system/cpu/cpu$cpu/online"
        if (!File(path).exists()) return false
        val result = Shell.cmd("echo ${if (online) 1 else 0} > $path").exec()
        result.isSuccess
    }.onFailure {
        Log.e(TAG, "setCpuCoreOnline cpu$cpu: ${it.message}", it)
    }.getOrDefault(false)
    const val CPU_SCHED_BOOST_ON_INPUT = "/sys/devices/system/cpu/cpu_boost/sched_boost_on_input"

    const val MIN_FREQ_GPU = "/sys/class/kgsl/kgsl-3d0/min_clock_mhz"
    const val MAX_FREQ_GPU = "/sys/class/kgsl/kgsl-3d0/max_clock_mhz"
    const val CURRENT_FREQ_GPU = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    const val AVAILABLE_FREQ_GPU = "/sys/class/kgsl/kgsl-3d0/freq_table_mhz"
    const val GOV_GPU = "/sys/class/kgsl/kgsl-3d0/devfreq/governor"
    const val AVAILABLE_GOV_GPU = "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors"
    const val MAX_PWRLEVEL = "/sys/class/kgsl/kgsl-3d0/max_pwrlevel"
    const val MIN_PWRLEVEL = "/sys/class/kgsl/kgsl-3d0/min_pwrlevel"
    const val DEFAULT_PWRLEVEL = "/sys/class/kgsl/kgsl-3d0/default_pwrlevel"
    const val ADRENO_BOOST = "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost"
    const val GPU_THROTTLING = "/sys/class/kgsl/kgsl-3d0/throttling"
    const val GPU_TEMP = "/sys/class/kgsl/kgsl-3d0/temp"

    // MediaTek GED/GPUFREQ (used by MT6768 and other MTK kernels).
    const val MTK_GPU_CURRENT_FREQ = "/sys/kernel/ged/hal/current_freqency"
    const val MTK_GPU_OPP_LOGS = "/sys/kernel/ged/hal/opp_logs"
    const val MTK_GPU_OPP_DUMP = "/proc/gpufreq/gpufreq_opp_dump"
    const val MTK_GPU_VAR_DUMP = "/proc/gpufreq/gpufreq_var_dump"
    const val MTK_GPU_UTILIZATION = "/sys/kernel/ged/hal/gpu_utilization"
    private val MTK_GPU_UPBOUND_PATHS = listOf(
        "/sys/kernel/ged/hal/custom_upbound_gpu_freq",
        "/sys/kernel/debug/ged/hal/custom_upbound_gpu_freq",
        "/d/ged/hal/custom_upbound_gpu_freq"
    )

    private val MTK_GPU_BOTTOM_PATHS = listOf(
        "/sys/kernel/ged/hal/custom_boost_gpu_freq",
        "/sys/kernel/debug/ged/hal/custom_boost_gpu_freq",
        "/d/ged/hal/custom_boost_gpu_freq"
    )

    /**
     * Detect MTK GED through the shell instead of File.exists().
     * GED/debugfs nodes can be inaccessible to the app's normal Java file API
     * while the root shell used by the manager can still read them.
     */
    fun isMtkGpu(): Boolean = runCatching {
        val current = Shell.cmd("cat $MTK_GPU_CURRENT_FREQ").exec()
        if (current.isSuccess) {
            val values = current.out
                .flatMap { it.trim().split("\\s+".toRegex()) }
                .mapNotNull { it.toLongOrNull() }
            if (values.size >= 2 && values.last() > 0) return true
        }

        val opp = Shell.cmd("cat $MTK_GPU_OPP_LOGS").exec()
        if (opp.isSuccess && opp.out.any {
                it.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull()?.let { hz -> hz > 0 } == true
            }) return true

        // Some MT6768 kernels expose gpufreq only through /proc. In that
        // case the GED sysfs nodes above may be absent even though the real
        // MediaTek DVFS driver is active. Recognize the proc interface so
        // the manager does not fall back to the unrelated KGSL 200 MHz path.
        Shell.cmd("test -r $MTK_GPU_VAR_DUMP || test -r $MTK_GPU_OPP_DUMP").exec().isSuccess
    }.getOrDefault(false)

    private var sPrevTotal: Long = -1
    private var sPrevIdle: Long = -1

    fun getCpuInfo(context: Context): String = runCatching {
        val hardware = Utils.getSystemProperty("ro.hardware")
        val manufacturer = Utils.getSystemProperty("ro.soc.manufacturer")
        val model = Utils.getSystemProperty("ro.soc.model")
        val platform = Utils.getSystemProperty("ro.board.platform")
        val cpuInfo = Shell.cmd("cat /proc/cpuinfo").exec().out.joinToString(" ")

        when {
            model.isNotBlank() && manufacturer.isNotBlank() -> "$manufacturer $model"
            hardware.contains("qcom", ignoreCase = true) && model.isNotBlank() ->
                "Qualcomm Technologies, Inc $model"
            manufacturer.contains("QTI", ignoreCase = true) && model.isNotBlank() ->
                "Qualcomm Technologies, Inc $model"
            platform.equals("mt6768", ignoreCase = true) ||
                hardware.contains("mt6768", ignoreCase = true) ||
                cpuInfo.contains("MT6768", ignoreCase = true) ->
                "MediaTek MT6768"
            hardware.contains("mt", ignoreCase = true) && platform.isNotBlank() ->
                "MediaTek $platform"
            platform.isNotBlank() -> platform.uppercase()
            else -> context.getString(R.string.unknown)
        }
    }.getOrElse {
        Log.e(TAG, "getCpuInfo: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun readFreqCPU(filePath: String): String = runCatching {
        val file = File(filePath)
        if (file.exists()) {
            val freq = file.readText().trim()
            (freq.toInt() / 1000).toString()
        } else {
            "0"
        }
    }.getOrElse {
        Log.e(TAG, "readFreqCPU: ${it.message}", it)
        "0"
    }

    fun writeFreqCPU(filePath: String, frequency: String) {
        runCatching {
            val freqInKHz = (frequency.toInt() * 1000).toString()
            val command = "echo $freqInKHz > $filePath"
            Shell.cmd(command).exec()
        }.onFailure {
            Log.e(TAG, "writeFreqCPU: ${it.message}", it)
        }
    }

    fun readAvailableFreqCPU(filePath: String): List<String> = runCatching {
        val file = File(filePath)
        if (file.exists()) {
            file.readText()
                .trim()
                .split(" ")
                .map { (it.toInt() / 1000).toString() }
        } else {
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "readAvailableFreqCPU: ${it.message}", it)
        emptyList()
    }

    fun readAvailableFreqBoost(freqPath: String, boostPath: String): List<String> = runCatching {
        val regularFreq = readAvailableFreqCPU(freqPath)
        val boostFreq = readAvailableFreqCPU(boostPath)
        (regularFreq + boostFreq)
            .distinct()
            .sortedBy { it.toIntOrNull() ?: 0 }
    }.getOrElse {
        Log.e(TAG, "readAvailableFreqBoost: ${it.message}", it)
        emptyList()
    }

    fun readAvailableGovCPU(filePath: String): List<String> = runCatching {
        val file = File(filePath)
        if (file.exists()) {
            file.readText()
                .trim()
                .split(" ")
        } else {
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "readAvailableGovCPU: ${it.message}", it)
        emptyList()
    }

    fun getOpenGL(context: Context): String = runCatching {
        val result = Shell.cmd("dumpsys SurfaceFlinger | grep \"GLES:\"").exec()
        if (result.isSuccess && result.out.isNotEmpty()) {
            val glesLine = result.out.firstOrNull()?.trim()
            if (!glesLine.isNullOrBlank()) {
                val regex = Regex("GLES:\\s*[^,]+,\\s*(.+)")
                val matchResult = regex.find(glesLine)
                if (matchResult != null) {
                    val gpuInfo = matchResult.groupValues[1].trim()
                    return gpuInfo
                } else {
                    val commaIndex = glesLine.indexOf(',')
                    if (commaIndex != -1 && commaIndex < glesLine.length - 1) {
                        return glesLine.substring(commaIndex + 1).trim()
                    }
                }
            }
        }
        context.getString(R.string.unknown)
    }.getOrElse {
        Log.e(TAG, "getOpenGL: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun writeFreqGPU(filePath: String, frequency: String) {
        runCatching {
            Shell.cmd("echo $frequency > $filePath").exec()
        }.onFailure {
            Log.e(TAG, "writeFreqGPU: ${it.message}", it)
        }
    }

    /**
     * MTK GED current_freqency contains two values: OPP index and frequency in kHz,
     * e.g. "29 299000". Return the frequency in MHz.
     */
    fun readMtkGpuCurrentFreq(): String = runCatching {
        // On MT6768 kernels, /sys/kernel/ged/hal/current_freqency can be
        // stale (often stuck at 299000). The gpufreq var dump reports the
        // actual DVFS OPP/frequency, so prefer its live values.
        val varDump = Shell.cmd("cat $MTK_GPU_VAR_DUMP").exec()
        if (varDump.isSuccess) {
            val realClock = varDump.out.asSequence()
                .mapNotNull { Regex("real clock freq\\s*=\\s*(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toLongOrNull() }
                .firstOrNull()
            if (realClock != null && realClock > 0) return (realClock / 1000).toString()

            val oppFreq = varDump.out.asSequence()
                .mapNotNull { Regex("g_cur_opp_freq\\s*=\\s*(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toLongOrNull() }
                .firstOrNull()
            if (oppFreq != null && oppFreq > 0) return (oppFreq / 1000).toString()
        }

        val result = Shell.cmd("cat $MTK_GPU_CURRENT_FREQ").exec()
        if (!result.isSuccess) return "0"
        val khz = result.out.asSequence()
            .flatMap { it.trim().split("\\s+".toRegex()).asSequence() }
            .mapNotNull { it.toLongOrNull() }
            .lastOrNull() ?: return "0"
        (khz / 1000).toString()
    }.getOrElse {
        Log.e(TAG, "readMtkGpuCurrentFreq: ${it.message}", it)
        "0"
    }

    /**
     * MTK GED opp_logs lists frequency in Hz in the first column. Convert to MHz,
     * remove the trailing time column and return a unique sorted OPP list.
     */
    /**
     * Read the real MediaTek GPU OPP table.
     *
     * This device exposes the authoritative table through
     * /proc/gpufreq/gpufreq_opp_dump. GED opp_logs is a statistics/logging
     * interface and on this kernel does not contain the complete 32-entry OPP
     * table (the lowest 299 MHz entry can be missing).
     */
    fun readMtkGpuAvailableFreq(): List<String> = runCatching {
        val dump = Shell.cmd("cat $MTK_GPU_OPP_DUMP").exec()
        if (dump.isSuccess) {
            val procFreqs = dump.out.asSequence()
                .mapNotNull { line ->
                    Regex("\\[\\d+\\]\\s+freq\\s*=\\s*(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
                }
                .filter { it > 0 }
                .map { (it / 1000L).toString() }
                .distinct()
                .sortedByDescending { it.toIntOrNull() ?: 0 }
                .toList()
            if (procFreqs.isNotEmpty()) return procFreqs
        }

        // Fallback for older GED trees.
        val result = Shell.cmd("cat $MTK_GPU_OPP_LOGS").exec()
        if (!result.isSuccess) return emptyList()
        result.out.asSequence()
            .mapNotNull { line ->
                line.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull()
            }
            .filter { it > 0 }
            .map { (it / 1_000_000L).toString() }
            .distinct()
            .sortedByDescending { it.toIntOrNull() ?: 0 }
            .toList()
    }.getOrElse {
        Log.e(TAG, "readMtkGpuAvailableFreq: ${it.message}", it)
        emptyList()
    }

    /**
     * MTK GED's custom_boost_gpu_freq is the GPU frequency floor.
     *
     * The node accepts a frequency LEVEL, not a MHz value. On the MTK table
     * used by this device, level 0 maps to the lowest OPP (299 MHz), while
     * level N raises the minimum floor to the corresponding higher OPP.
     */
    fun getMtkGpuBottomPath(): String? = MTK_GPU_BOTTOM_PATHS.firstOrNull {
        Shell.cmd("test -e $it").exec().isSuccess
    }

    fun readMtkGpuMinFreq(): String = runCatching {
        val freqs = readMtkGpuAvailableFreq()
        if (freqs.isEmpty()) return "0"

        val path = getMtkGpuBottomPath()
        if (path != null) {
            val level = Shell.cmd("cat $path").exec().out.firstOrNull()?.trim()?.toIntOrNull()
            if (level != null) {
                val index = (freqs.lastIndex - level).coerceIn(0, freqs.lastIndex)
                return freqs[index]
            }
        }

        // If the floor node is unavailable, report the physical lowest OPP.
        freqs.minByOrNull { it.toIntOrNull() ?: Int.MAX_VALUE } ?: "0"
    }.getOrElse {
        Log.e(TAG, "readMtkGpuMinFreq: ${it.message}", it)
        "0"
    }

    fun isMtkGpuMinFreqWritable(): Boolean = runCatching {
        getMtkGpuBottomPath()?.let { Shell.cmd("test -w $it").exec().isSuccess } == true
    }.getOrDefault(false)

    fun writeMtkGpuMinFreq(frequency: String) {
        runCatching {
            val freqs = readMtkGpuAvailableFreq()
            val selected = frequency.toIntOrNull() ?: return
            val index = freqs.indexOfFirst { it.toIntOrNull() == selected }
            if (index < 0) return
            val path = getMtkGpuBottomPath() ?: return

            // GED's custom_boost_gpu_freq uses the same inverted level mapping
            // as the kernel's bottom-frequency setter:
            //   level = (table_size - 1) - OPP_index
            // This is the actual minimum-frequency floor, not a fake boost.
            val level = freqs.lastIndex - index
            Shell.cmd("echo $level > $path").exec()
        }.onFailure {
            Log.e(TAG, "writeMtkGpuMinFreq: ${it.message}", it)
        }
    }

    fun readMtkGpuMaxFreq(): String = runCatching {
        val freqs = readMtkGpuAvailableFreq()
        if (freqs.isEmpty()) return "0"

        // GED stores the ceiling as a "frequency level" where 0 means the
        // lowest OPP and (table_size - 1) means the highest OPP. The actual
        // gpufreq table, however, is indexed in the opposite direction:
        // index 0 = highest frequency, last index = lowest frequency.
        val path = getMtkGpuUpboundPath()
        if (path != null) {
            val level = Shell.cmd("cat $path").exec().out.firstOrNull()?.trim()?.toIntOrNull()
            if (level != null) {
                val index = (freqs.lastIndex - level).coerceIn(0, freqs.lastIndex)
                return freqs[index]
            }
        }

        freqs.maxByOrNull { it.toIntOrNull() ?: 0 } ?: "0"
    }.getOrElse {
        Log.e(TAG, "readMtkGpuMaxFreq: ${it.message}", it)
        "0"
    }

    /**
     * GED's custom_upbound_gpu_freq is a frequency level, not the same index
     * used by the gpufreq OPP table. Different MTK trees expose it through
     * different debugfs aliases.
     */
    fun getMtkGpuUpboundPath(): String? = MTK_GPU_UPBOUND_PATHS.firstOrNull {
        Shell.cmd("test -e $it").exec().isSuccess
    }

    fun isMtkGpuMaxFreqWritable(): Boolean = runCatching {
        getMtkGpuUpboundPath()?.let { Shell.cmd("test -w $it").exec().isSuccess } == true
    }.getOrDefault(false)

    /**
     * Keep the two GED frequency limits in a valid order.
     *
     * On this MT6768 GED implementation the boost limit is the minimum
     * frequency floor and the upbound limit is the maximum ceiling. If a
     * previous build/user action left them crossed (min > max), GED clamps
     * DVFS to the lower ceiling. Repair the crossed state once so the UI and
     * kernel agree: min <= max.
     */
    fun normalizeMtkGpuLimits(): Boolean = runCatching {
        val min = readMtkGpuMinFreq().toIntOrNull() ?: return false
        val max = readMtkGpuMaxFreq().toIntOrNull() ?: return false
        if (min <= max) return true

        val minPath = getMtkGpuBottomPath() ?: return false
        val maxPath = getMtkGpuUpboundPath() ?: return false
        if (!Shell.cmd("test -w $minPath").exec().isSuccess ||
            !Shell.cmd("test -w $maxPath").exec().isSuccess
        ) return false

        // Move the lower value to the minimum-floor node and the higher value
        // to the maximum-ceiling node. The writers perform the level/index
        // conversion for the GED interface.
        writeMtkGpuMinFreq(max.toString())
        writeMtkGpuMaxFreq(min.toString())

        readMtkGpuMinFreq().toIntOrNull() ?: return false
        readMtkGpuMaxFreq().toIntOrNull() ?: return false
        true
    }.onFailure {
        Log.e(TAG, "normalizeMtkGpuLimits: ${it.message}", it)
    }.getOrDefault(false)

    fun writeMtkGpuMaxFreq(frequency: String) {
        runCatching {
            val freqs = readMtkGpuAvailableFreq()
            val selected = frequency.toIntOrNull() ?: return
            val index = freqs.indexOfFirst { it.toIntOrNull() == selected }
            if (index < 0) return
            val path = getMtkGpuUpboundPath() ?: return

            // GED's ceiling writer converts: frequency_level -> OPP index
            // with: opp_index = (table_size - 1) - frequency_level.
            // Therefore invert that mapping before writing. Without this
            // inversion, selecting 950 MHz writes level 0 and clamps the GPU
            // to the lowest OPP (299 MHz).
            val level = freqs.lastIndex - index
            Shell.cmd("echo $level > $path").exec()
        }.onFailure {
            Log.e(TAG, "writeMtkGpuMaxFreq: ${it.message}", it)
        }
    }

    /**
     * Find the AP/CPU thermal sensor used by MediaTek kernels. thermal_zone0
     * is not guaranteed to be the CPU sensor on MT6768, so scan the zone type
     * names first and keep the old path as a fallback.
     */
    fun getMtkCpuTemperature(context: Context): String = runCatching {
        val result = Shell.cmd(
            "for z in /sys/class/thermal/thermal_zone*; do " +
                "[ -r \$z/type ] || continue; " +
                "t=\$(cat \$z/type 2>/dev/null); " +
                "case \"\$t\" in *cpu*|*CPU*|*mtktscpu*|*ap*|*AP*) " +
                "v=\$(cat \$z/temp 2>/dev/null); " +
                "case \"\$v\" in ''|'0') continue;; esac; " +
                "echo \"\$v\"; break;; esac; " +
                "done"
        ).exec()
        val raw = result.out.firstOrNull()?.trim()?.toLongOrNull()
        if (result.isSuccess && raw != null) {
            return "%.1f".format(raw / 1000.0)
        }

        val fallback = Utils.readFile(CPU_TEMP).toLongOrNull()
        fallback?.let { return "%.1f".format(it / 1000.0) }
        context.getString(R.string.unknown)
    }.getOrElse {
        Log.e(TAG, "getMtkCpuTemperature: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getMtkGpuUsage(context: Context): String = runCatching {
        // gpufreq_var_dump exposes the same gpu_loading value used by the
        // MediaTek DVFS code. Prefer it over the GED aggregate line.
        val varDump = Shell.cmd("cat $MTK_GPU_VAR_DUMP").exec()
        if (varDump.isSuccess) {
            val loading = varDump.out.asSequence()
                .mapNotNull { Regex("gpu_loading\\s*=\\s*(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                .firstOrNull()
            if (loading != null) return loading.coerceIn(0, 100).toString()
        }

        val result = Shell.cmd("cat $MTK_GPU_UTILIZATION").exec()
        if (!result.isSuccess) return context.getString(R.string.unknown)
        val value = result.out.asSequence()
            .flatMap { it.trim().split("\\s+".toRegex()).asSequence() }
            .mapNotNull { it.toIntOrNull() }
            .firstOrNull() ?: return context.getString(R.string.unknown)
        value.coerceIn(0, 100).toString()
    }.getOrElse {
        Log.e(TAG, "getMtkGpuUsage: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    /**
     * GPU temperature is intentionally reported as unavailable on this
     * device. Its MediaTek thermal driver exposes CPU/AP, battery, PMIC,
     * PA, DCTM and image-sensor zones, but no live MFG/GPU temperature
     * sensor. Never substitute CPU/AP temperature for GPU temperature.
     */
    fun getMtkGpuTemperature(context: Context): String = "N/A"

    fun readAvailableFreqGPU(filePath: String): List<String> = runCatching {
        val result = Shell.cmd("cat $filePath").exec()
        if (result.isSuccess) {
            result.out
                .flatMap { it.trim().split("\\s+".toRegex()) }
                .filter { it.isNotEmpty() }
        } else {
            Log.e("readAvailableFreqGPU", "Command execution failed: ${result.err}")
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "readAvailableFreqGPU: ${it.message}", it)
        emptyList()
    }

    fun readAvailableGovGPU(filePath: String): List<String> = runCatching {
        val result = Shell.cmd("cat $filePath").exec()
        if (result.isSuccess) {
            result.out
                .flatMap { it.trim().split("\\s+".toRegex()) }
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "readAvailableGovGPU: ${it.message}", it)
        emptyList()
    }

    fun readFreqGPU(filePath: String): String = runCatching {
        val result = Shell.cmd("cat $filePath").exec()
        if (result.isSuccess) {
            result.out.firstOrNull()
                ?.trim()
                ?.let { (it.toLong() / 1000000).toString() }
                ?: "0"
        } else {
            "0"
        }
    }.getOrElse {
        Log.e(TAG, "readCurrentGPUFreq: ${it.message}", it)
        "0"
    }

    fun getCpuUsage(context: Context): String = runCatching {
        val stat = Utils.readFile("/proc/stat")
        val trimmedStat = stat.trim()

        if (!trimmedStat.startsWith("cpu")) return context.getString(R.string.unknown)

        val parts = trimmedStat.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.size < 8) return context.getString(R.string.unknown)

        val user = parts[1].toLong()
        val nice = parts[2].toLong()
        val system = parts[3].toLong()
        val idle = parts[4].toLong()
        val iowait = parts[5].toLong()
        val irq = parts[6].toLong()
        val softirq = parts[7].toLong()
        val steal = if (parts.size > 8) parts[8].toLong() else 0

        val total = user + nice + system + idle + iowait + irq + softirq + steal

        if (sPrevTotal != -1L && total > sPrevTotal) {
            val diffTotal = total - sPrevTotal
            val diffIdle = idle - sPrevIdle
            val usage = 100 * (diffTotal - diffIdle) / diffTotal
            sPrevTotal = total
            sPrevIdle = idle
            return usage.toString()
        } else {
            sPrevTotal = total
            sPrevIdle = idle
            return context.getString(R.string.unknown)
        }
    }.getOrElse {
        Log.e(TAG, "getCpuUsage: ${it.message}", it)
        return context.getString(R.string.unknown)
    }

    fun getGpuUsage(context: Context): String = runCatching {
        if (isMtkGpu()) {
            return getMtkGpuUsage(context)
        }
        val usage = Utils.readFile("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
        if (usage.isEmpty()) return context.getString(R.string.unknown)
        val cleanedUsage = usage.replace("%", "").trim()
        val value = cleanedUsage.toInt()
        value.toString()
    }.getOrElse {
        Log.e(TAG, "getGpuUsage: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getRamMemoryInfo(context: Context): RamMemoryInfo = runCatching {
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(this)
        }
        RamMemoryInfo(
            total = formatRamBytes(memoryInfo.totalMem),
            free = formatRamBytes(memoryInfo.availMem),
            used = formatRamBytes((memoryInfo.totalMem - memoryInfo.availMem).coerceAtLeast(0L)),
            totalBytes = memoryInfo.totalMem,
            freeBytes = memoryInfo.availMem,
            usedBytes = (memoryInfo.totalMem - memoryInfo.availMem).coerceAtLeast(0L),
        )
    }.getOrElse {
        Log.e(TAG, "getRamMemoryInfo: ${it.message}", it)
        RamMemoryInfo(
            total = context.getString(R.string.unknown),
            free = context.getString(R.string.unknown),
            used = context.getString(R.string.unknown),
            totalBytes = 0L,
            freeBytes = 0L,
            usedBytes = 0L,
        )
    }

    data class RamMemoryInfo(
        val total: String,
        val used: String,
        val free: String,
        val totalBytes: Long,
        val usedBytes: Long,
        val freeBytes: Long,
    )

    private fun formatRamBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 GB"
        val gib = 1024.0 * 1024.0 * 1024.0
        val value = bytes / gib
        return if (value >= 1.0) {
            "%.1f GB".format(java.util.Locale.US, value).replace(".0 GB", " GB")
        } else {
            "%.0f MB".format(java.util.Locale.US, bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Read a dedicated RAM/DRAM/DDR thermal sensor when the kernel exposes one.
     * This intentionally does not fall back to CPU/AP temperature, because that
     * would be misleading on MediaTek kernels where RAM has no separate sensor.
     */
    fun getTotalRam(context: Context): String = getRamMemoryInfo(context).total
}
