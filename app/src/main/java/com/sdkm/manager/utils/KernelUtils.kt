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

import android.content.Context
import android.system.Os
import android.util.Log
import com.sdkm.manager.R
import com.topjohnwu.superuser.Shell

object KernelUtils {
    const val TAG = "KernelUtils"


    const val FULL_KERNEL_VERSION = "/proc/version"

    const val SCHED_AUTO_GROUP = "/proc/sys/kernel/sched_autogroup_enabled"
    const val PRINTK = "/proc/sys/kernel/printk"
    const val SCHED_LIB_NAME = "/proc/sys/kernel/sched_lib_name"

    const val SCHED_UTIL_CLAMP_MAX = "/proc/sys/kernel/sched_util_clamp_max"
    const val SCHED_UTIL_CLAMP_MIN = "/proc/sys/kernel/sched_util_clamp_min"
    const val SCHED_UTIL_CLAMP_MIN_RT_DEFAULT = "/proc/sys/kernel/sched_util_clamp_min_rt_default"

    const val ZRAM = "/dev/block/zram0"
    const val ZRAM_RESET = "/sys/block/zram0/reset"
    const val ZRAM_SIZE = "/sys/block/zram0/disksize"
    const val ZRAM_COMP_ALGORITHM = "/sys/block/zram0/comp_algorithm"
    const val SWAPPINESS = "/proc/sys/vm/swappiness"
    const val EXTRA_FREE_KBYTES = "/proc/sys/vm/extra_free_kbytes"
    const val WATERMARK_SCALE_FACTOR = "/proc/sys/vm/watermark_scale_factor"
    const val DIRTY_RATIO = "/proc/sys/vm/dirty_ratio"

    const val TCP_CONGESTION_ALGORITHM = "/proc/sys/net/ipv4/tcp_congestion_control"
    const val TCP_AVAILABLE_CONGESTION_ALGORITHM = "/proc/sys/net/ipv4/tcp_available_congestion_control"

    const val WIREGUARD_VERSION = "/sys/module/wireguard/version"

    const val BORE = "/proc/sys/kernel/sched_bore"
    const val BURST_SMOOTHNESS_LONG = "/proc/sys/kernel/sched_burst_smoothness_long"
    const val BURST_SMOOTHNESS_SHORT = "/proc/sys/kernel/sched_burst_smoothness_short"
    const val BURST_FORK_ATAVISTIC = "/proc/sys/kernel/sched_burst_fork_atavistic"
    const val BURST_PENALTY_OFFSET = "/proc/sys/kernel/sched_burst_penalty_offset"
    const val BURST_PENALTY_SCALE = "/proc/sys/kernel/sched_burst_penalty_scale"
    const val BURST_CACHE_LIFETIME = "/proc/sys/kernel/sched_burst_cache_lifetime"

    fun getKernelVersion(context: Context): String = runCatching {
        Os.uname().release
    }.getOrElse {
        Log.e(TAG, "getKernelVersion: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getFullKernelVersion(context: Context): String = runCatching {
        Utils.readFile(FULL_KERNEL_VERSION)
    }.getOrElse {
        Log.e(TAG, "getFullKernelVersion: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getZramSize(context: Context): String = runCatching {
        val sizeInBytes = Utils.readFile(ZRAM_SIZE).trim().toLongOrNull() ?: 0L
        if (sizeInBytes <= 0L) return "0 GB"
        val gib = 1024L * 1024L * 1024L
        if (sizeInBytes % gib == 0L) {
            "${sizeInBytes / gib} GB"
        } else {
            "%.1f GB".format(java.util.Locale.US, sizeInBytes / gib.toDouble())
        }
    }.getOrElse {
        Log.e(TAG, "getZramSize: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    data class ZramMemoryInfo(
        val totalBytes: Long,
        val usedBytes: Long,
        val freeBytes: Long,
    )

    fun getZramMemoryInfo(): ZramMemoryInfo = runCatching {
        val totalBytes = Utils.readFile(ZRAM_SIZE).trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val swapLine = Utils.readFile("/proc/swaps")
            .lineSequence()
            .firstOrNull { it.trim().startsWith(ZRAM) }
        val parts = swapLine?.trim()?.split(Regex("\\s+")) ?: emptyList()
        val usedBytes = parts.getOrNull(3)?.toLongOrNull()?.times(1024L)?.coerceAtMost(totalBytes) ?: 0L
        ZramMemoryInfo(totalBytes, usedBytes, (totalBytes - usedBytes).coerceAtLeast(0L))
    }.getOrElse {
        Log.e(TAG, "getZramMemoryInfo: ${it.message}", it)
        ZramMemoryInfo(0L, 0L, 0L)
    }

    fun freeRam() {
        runCatching {
            Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec()
        }.onFailure {
            Log.e(TAG, "freeRam: ${it.message}", it)
        }
    }

    fun getZramCompAlgorithm(context: Context): String = runCatching {
        val algorithms = Utils.readFile(ZRAM_COMP_ALGORITHM)
        if (algorithms.isNotEmpty()) {
            val regex = "\\[([^\\]]+)\\]".toRegex()
            val match = regex.find(algorithms)
            match?.groupValues?.get(1) ?: context.getString(R.string.unknown)
        } else {
            context.getString(R.string.unknown)
        }
    }.getOrElse {
        Log.e(TAG, "getZramCompAlgorithm: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getAvailableZramCompAlgorithms(): List<String> = runCatching {
        val algorithms = Utils.readFile(ZRAM_COMP_ALGORITHM)
        return if (algorithms.isNotEmpty()) {
            algorithms.replace("[", "").replace("]", "")
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "getAvailableZramCompAlgorithms: ${it.message}", it)
        emptyList()
    }

    fun swapoffZram() {
        runCatching {
            Shell.cmd("swapoff $ZRAM").exec()
        }.onFailure {
            Log.e(TAG, "swapoffZram: ${it.message}", it)
        }
    }

    fun swaponZram() {
        runCatching {
            Shell.cmd("swapon $ZRAM").exec()
        }.onFailure {
            Log.e(TAG, "swaponZram: ${it.message}", it)
        }
    }

    fun mkswapZram() {
        runCatching {
            Shell.cmd("mkswap $ZRAM").exec()
        }.onFailure {
            Log.e(TAG, "mkswapZram: ${it.message}", it)
        }
    }

    fun resetZram() {
        runCatching {
            Shell.cmd("echo 1 > $ZRAM_RESET").exec()
        }.onFailure {
            Log.e(TAG, "resetZram: ${it.message}", it)
        }
    }

    fun setZramCompAlgorithm(algorithm: String) {
        runCatching {
            Shell.cmd("echo $algorithm > $ZRAM_COMP_ALGORITHM").exec()
        }.onFailure {
            Log.e(TAG, "setZramCompAlgorithm: ${it.message}", it)
        }
    }

    fun getTcpCongestionAlgorithm(context: Context): String = runCatching {
        Utils.readFile(TCP_CONGESTION_ALGORITHM)
    }.getOrElse {
        Log.e(TAG, "getTcpCongestionAlgorithm: ${it.message}", it)
        context.getString(R.string.unknown)
    }

    fun getAvailableTcpCongestionAlgorithm(): List<String> = runCatching {
        val algorithms = Utils.readFile(TCP_AVAILABLE_CONGESTION_ALGORITHM)
        if (algorithms.isNotEmpty()) {
            algorithms.trim()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }.getOrElse {
        Log.e(TAG, "getAvailableTcpCongestionAlgorithm: ${it.message}", it)
        emptyList()
    }

    fun setTcpCongestionAlgorithm(algorithm: String) {
        runCatching {
            Utils.writeFile(TCP_CONGESTION_ALGORITHM, algorithm)
        }.onFailure {
            Log.e(TAG, "setTcpCongestionAlgorithm: ${it.message}", it)
        }
    }

    fun getWireGuardVersion(context: Context): String = runCatching {
        Utils.readFile(WIREGUARD_VERSION)
    }.getOrElse {
        Log.e(TAG, "getWireGuardVersion: ${it.message}", it)
        context.getString(R.string.unknown)
    }

}
