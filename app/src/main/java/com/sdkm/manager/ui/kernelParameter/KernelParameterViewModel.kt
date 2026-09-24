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
package com.sdkm.manager.ui.kernelParameter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sdkm.manager.utils.KernelUtils
import com.sdkm.manager.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KernelParameterViewModel(application: Application) : AndroidViewModel(application) {
    data class KernelParameters(
        val schedAutogroup: Int? = 0,
        val hasSchedAutogroup: Boolean = false,
        val printk: String = "unknown",
        val hasPrintk: Boolean = false,
        val tcpCongestionAlgorithm: String = "unknown",
        val hasTcpCongestionAlgorithm: Boolean = false,
        val availableTcpCongestionAlgorithm: List<String> = emptyList(),
        val hasSchedLibName: Boolean = false,
        val schedLibName: String = "unknown",
    )

    data class Uclamp(
        val hasUclampMax: Boolean = false,
        val uclampMax: String = "N/A",
        val hasUclampMin: Boolean = false,
        val uclampMin: String = "N/A",
        val hasUclampMinRt: Boolean = false,
        val uclampMinRt: String = "N/A",
    )

    data class Memory(
        val zramSize: String = "N/A",
        val zramUsedBytes: Long = 0L,
        val zramFreeBytes: Long = 0L,
        val zramTotalBytes: Long = 0L,
        val zramCompAlgorithm: String = "N/A",
        val hasZramCompAlgorithm: Boolean = false,
        val availableZramCompAlgorithms: List<String> = emptyList(),
        val swappiness: String = "N/A",
        val hasSwappiness: Boolean = false,
        val pageCluster: String = "N/A",
        val hasPageCluster: Boolean = false,
        val vfsCachePressure: String = "N/A",
        val hasVfsCachePressure: Boolean = false,
        val dirtyBackgroundRatio: String = "N/A",
        val hasDirtyBackgroundRatio: Boolean = false,
        val extraFreeKbytes: String = "N/A",
        val hasExtraFreeKbytes: Boolean = false,
        val watermarkScaleFactor: String = "N/A",
        val hasWatermarkScaleFactor: Boolean = false,
        val hasDirtyRatio: Boolean = false,
        val dirtyRatio: String = "N/A",
    )

    data class BoreScheduler(
        val hasBore: Boolean = false,
        val bore: String = "",
        val hasBurstSmoothnessLong: Boolean = false,
        val burstSmoothnessLong: String = "",
        val hasBurstSmoothnessShort: Boolean = false,
        val burstSmoothnessShort: String = "",
        val hasBurstForkAtavistic: Boolean = false,
        val burstForkAtavistic: String = "",
        val hasBurstPenaltyOffset: Boolean = false,
        val burstPenaltyOffset: String = "",
        val hasBurstPenaltyScale: Boolean = false,
        val burstPenaltyScale: String = "",
        val hasBurstCacheLifetime: Boolean = false,
        val burstCacheLifetime: String = "",
    )

    private val _kernelParameters = MutableStateFlow(KernelParameters())
    val kernelParameters: StateFlow<KernelParameters> = _kernelParameters

    private val _uclamp = MutableStateFlow(Uclamp())
    val uclamp: StateFlow<Uclamp> = _uclamp

    private val _memory = MutableStateFlow(Memory())
    val memory: StateFlow<Memory> = _memory

    private val _boreScheduler = MutableStateFlow(BoreScheduler())
    val boreScheduler: StateFlow<BoreScheduler> = _boreScheduler

    private val refreshRequests = Channel<Unit>(1)
    var isRefreshing by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadKernelParameter()
            loadUclamp()
            loadMemory()
            loadBoreScheduler()

            for (r in refreshRequests) {
                isRefreshing = true
                try {
                    delay(1000)
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
        loadKernelParameter()
        loadUclamp()
        loadMemory()
        loadBoreScheduler()
    }

    fun loadKernelParameter() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            _kernelParameters.value = KernelParameters(
                schedAutogroup = Utils.readFile(KernelUtils.SCHED_AUTO_GROUP).toIntOrNull(),
                hasSchedAutogroup = Utils.testFile(KernelUtils.SCHED_AUTO_GROUP),
                printk = Utils.readFile(KernelUtils.PRINTK),
                hasPrintk = Utils.testFile(KernelUtils.PRINTK),
                tcpCongestionAlgorithm = KernelUtils.getTcpCongestionAlgorithm(context),
                hasTcpCongestionAlgorithm = Utils.testFile(KernelUtils.TCP_CONGESTION_ALGORITHM),
                availableTcpCongestionAlgorithm = KernelUtils.getAvailableTcpCongestionAlgorithm(),
                hasSchedLibName = Utils.testFile(KernelUtils.SCHED_LIB_NAME),
                schedLibName = Utils.readFile(KernelUtils.SCHED_LIB_NAME),
            )
        }
    }

    fun loadUclamp() {
        viewModelScope.launch(Dispatchers.IO) {
            _uclamp.value = Uclamp(
                hasUclampMax = Utils.testFile(KernelUtils.SCHED_UTIL_CLAMP_MAX),
                uclampMax = Utils.readFile(KernelUtils.SCHED_UTIL_CLAMP_MAX),
                hasUclampMin = Utils.testFile(KernelUtils.SCHED_UTIL_CLAMP_MIN),
                uclampMin = Utils.readFile(KernelUtils.SCHED_UTIL_CLAMP_MIN),
                hasUclampMinRt = Utils.testFile(KernelUtils.SCHED_UTIL_CLAMP_MIN_RT_DEFAULT),
                uclampMinRt = Utils.readFile(KernelUtils.SCHED_UTIL_CLAMP_MIN_RT_DEFAULT),
            )
        }
    }

    fun loadMemory() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val zram = KernelUtils.getZramMemoryInfo()
            _memory.value = Memory(
                zramSize = KernelUtils.getZramSize(context),
                zramTotalBytes = zram.totalBytes,
                zramUsedBytes = zram.usedBytes,
                zramFreeBytes = zram.freeBytes,
                zramCompAlgorithm = KernelUtils.getZramCompAlgorithm(context),
                hasZramCompAlgorithm = Utils.testFile(KernelUtils.ZRAM_COMP_ALGORITHM),
                availableZramCompAlgorithms = KernelUtils.getAvailableZramCompAlgorithms(),
                swappiness = Utils.readFile(KernelUtils.SWAPPINESS),
                hasSwappiness = Utils.testFile(KernelUtils.SWAPPINESS),
                pageCluster = Utils.readFile(KernelUtils.PAGE_CLUSTER),
                hasPageCluster = Utils.testFile(KernelUtils.PAGE_CLUSTER),
                vfsCachePressure = Utils.readFile(KernelUtils.VFS_CACHE_PRESSURE),
                hasVfsCachePressure = Utils.testFile(KernelUtils.VFS_CACHE_PRESSURE),
                dirtyBackgroundRatio = Utils.readFile(KernelUtils.DIRTY_BACKGROUND_RATIO),
                hasDirtyBackgroundRatio = Utils.testFile(KernelUtils.DIRTY_BACKGROUND_RATIO),
                extraFreeKbytes = Utils.readFile(KernelUtils.EXTRA_FREE_KBYTES),
                hasExtraFreeKbytes = Utils.testFile(KernelUtils.EXTRA_FREE_KBYTES),
                watermarkScaleFactor = Utils.readFile(KernelUtils.WATERMARK_SCALE_FACTOR),
                hasWatermarkScaleFactor = Utils.testFile(KernelUtils.WATERMARK_SCALE_FACTOR),
                hasDirtyRatio = Utils.testFile(KernelUtils.DIRTY_RATIO),
                dirtyRatio = Utils.readFile(KernelUtils.DIRTY_RATIO),
            )
        }
    }

    fun loadBoreScheduler() {
        viewModelScope.launch(Dispatchers.IO) {
            _boreScheduler.value = BoreScheduler(
                hasBore = Utils.testFile(KernelUtils.BORE),
                bore = Utils.readFile(KernelUtils.BORE),
                hasBurstSmoothnessLong = Utils.testFile(KernelUtils.BURST_SMOOTHNESS_LONG),
                burstSmoothnessLong = Utils.readFile(KernelUtils.BURST_SMOOTHNESS_LONG),
                hasBurstSmoothnessShort = Utils.testFile(KernelUtils.BURST_SMOOTHNESS_SHORT),
                burstSmoothnessShort = Utils.readFile(KernelUtils.BURST_SMOOTHNESS_SHORT),
                hasBurstForkAtavistic = Utils.testFile(KernelUtils.BURST_FORK_ATAVISTIC),
                burstForkAtavistic = Utils.readFile(KernelUtils.BURST_FORK_ATAVISTIC),
                hasBurstPenaltyOffset = Utils.testFile(KernelUtils.BURST_PENALTY_OFFSET),
                burstPenaltyOffset = Utils.readFile(KernelUtils.BURST_PENALTY_OFFSET),
                hasBurstPenaltyScale = Utils.testFile(KernelUtils.BURST_PENALTY_SCALE),
                burstPenaltyScale = Utils.readFile(KernelUtils.BURST_PENALTY_SCALE),
                hasBurstCacheLifetime = Utils.testFile(KernelUtils.BURST_CACHE_LIFETIME),
                burstCacheLifetime = Utils.readFile(KernelUtils.BURST_CACHE_LIFETIME),
            )
        }
    }

    fun setSchedAutogroup(isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = if (isChecked) 1 else 0
            Utils.writeFile(KernelUtils.SCHED_AUTO_GROUP, value.toString())
            _kernelParameters.value = _kernelParameters.value.copy(
                schedAutogroup = value,
            )
        }
    }

    fun setValue(filePath: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(filePath, value)
            when (filePath) {
                KernelUtils.PRINTK -> _kernelParameters.value.copy(printk = value)
                KernelUtils.SCHED_LIB_NAME -> _kernelParameters.value.copy(schedLibName = value)
                KernelUtils.TCP_CONGESTION_ALGORITHM -> _kernelParameters.value.copy(tcpCongestionAlgorithm = value)
                KernelUtils.SWAPPINESS -> _memory.value = _memory.value.copy(swappiness = value)
                KernelUtils.PAGE_CLUSTER -> _memory.value = _memory.value.copy(pageCluster = value)
                KernelUtils.VFS_CACHE_PRESSURE -> _memory.value = _memory.value.copy(vfsCachePressure = value)
                KernelUtils.DIRTY_BACKGROUND_RATIO -> _memory.value = _memory.value.copy(dirtyBackgroundRatio = value)
                KernelUtils.EXTRA_FREE_KBYTES -> _memory.value = _memory.value.copy(extraFreeKbytes = value)
                KernelUtils.WATERMARK_SCALE_FACTOR -> _memory.value = _memory.value.copy(watermarkScaleFactor = value)
                KernelUtils.DIRTY_RATIO -> _memory.value = _memory.value.copy(dirtyRatio = value)
                KernelUtils.BURST_SMOOTHNESS_LONG -> _boreScheduler.value.copy(burstSmoothnessLong = value)
                KernelUtils.BURST_SMOOTHNESS_SHORT -> _boreScheduler.value.copy(burstSmoothnessShort = value)
                KernelUtils.BURST_CACHE_LIFETIME -> _boreScheduler.value.copy(burstCacheLifetime = value)
                KernelUtils.BURST_FORK_ATAVISTIC -> _boreScheduler.value.copy(burstForkAtavistic = value)
                KernelUtils.BURST_PENALTY_OFFSET -> _boreScheduler.value.copy(burstPenaltyOffset = value)
                KernelUtils.BURST_PENALTY_SCALE -> _boreScheduler.value.copy(burstPenaltyScale = value)
                KernelUtils.SCHED_UTIL_CLAMP_MAX -> _uclamp.value.copy(uclampMax = value)
                KernelUtils.SCHED_UTIL_CLAMP_MIN -> _uclamp.value.copy(uclampMin = value)
                KernelUtils.SCHED_UTIL_CLAMP_MIN_RT_DEFAULT -> _uclamp.value.copy(uclampMinRt = value)
                else -> {}
            }
        }
    }

    fun updateZramCompAlgorithm(algorithm: String) {
        val currentSize = Utils.readFile(KernelUtils.ZRAM_SIZE)
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            KernelUtils.swapoffZram()
            KernelUtils.resetZram()
            KernelUtils.setZramCompAlgorithm(algorithm)
            Utils.writeFile(KernelUtils.ZRAM_SIZE, currentSize)
            KernelUtils.mkswapZram()
            KernelUtils.swaponZram()
            val zram = KernelUtils.getZramMemoryInfo()
            _memory.value = _memory.value.copy(
                zramCompAlgorithm = KernelUtils.getZramCompAlgorithm(context),
                zramTotalBytes = zram.totalBytes,
                zramUsedBytes = zram.usedBytes,
                zramFreeBytes = zram.freeBytes,
                zramSize = KernelUtils.getZramSize(context),
            )
        }
    }

    fun updateBoreStatus(isEnabled: Boolean) {
        val value = if (isEnabled) "1" else "0"
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(KernelUtils.BORE, value)
            _boreScheduler.value = _boreScheduler.value.copy(
                bore = value,
            )
        }
    }
}
