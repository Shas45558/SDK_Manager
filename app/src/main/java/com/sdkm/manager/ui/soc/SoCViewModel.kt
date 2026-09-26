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
package com.sdkm.manager.ui.soc

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sdkm.manager.ui.settings.SettingsPreference
import com.sdkm.manager.utils.KernelUtils
import com.sdkm.manager.utils.SoCUtils
import com.sdkm.manager.utils.Utils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SoCViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val CPU_BOOST = "/sys/module/ged/parameters/enable_cpu_boost"
        const val FORCE_CPU_BOOST = "/sys/module/ged/parameters/gx_force_cpu_boost"
        const val BOOST_UPPER_BOUND = "/sys/module/ged/parameters/boost_upper_bound"
        const val DEBOOST_REDUCE = "/sys/module/ged/parameters/deboost_reduce"
        const val GPU_BOOST = "/sys/module/ged/parameters/enable_gpu_boost"
        const val GPU_BOOST_ENABLE = "/sys/module/ged/parameters/boost_gpu_enable"
        const val GED_BOOST = "/sys/module/ged/parameters/ged_boost_enable"
        const val GPU_DVFS = "/sys/module/ged/parameters/gpu_dvfs_enable"
        const val GPU_BOOST_FREQ = "/sys/module/ged/parameters/gpu_cust_boost_freq"
        const val GPU_VAR_DUMP = "/proc/gpufreq/gpufreq_var_dump"
        const val GPU_OPP_DUMP = "/proc/gpufreq/gpufreq_opp_dump"
    }
    private val settingsPreference = SettingsPreference.getInstance(application)

    data class CPUState(
        val minFreq: String,
        val maxFreq: String,
        val currentFreq: String,
        val gov: String,
        val availableFreq: List<String>,
        val availableGov: List<String>,
    ) {
        companion object {
            val EMPTY = CPUState("N/A", "N/A", "N/A", "N/A", emptyList(), emptyList())
        }
    }

    data class RamState(
        val totalBytes: Long = 0L,
        val usedBytes: Long = 0L,
        val freeBytes: Long = 0L,
    )

    data class GPUState(
        val minFreq: String,
        val maxFreq: String,
        val currentFreq: String,
        val gov: String,
        val maxPwrlevel: String,
        val minPwrlevel: String,
        val defaultPwrlevel: String,
        val adrenoBoost: String,
        val gpuThrottling: String,
        val availableFreq: List<String>,
        val availableGov: List<String>,
    ) {
        companion object {
            val EMPTY = GPUState("N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "0", emptyList(), emptyList())
        }
    }

    sealed class ClusterConfig(
        val name: String,
        val minFreqPath: String,
        val maxFreqPath: String,
        val currentFreqPath: String,
        val govPath: String,
        val availableFreqPath: String,
        val availableGovPath: String,
        val availableBoostFreqPath: String? = null,
    ) {
        object Little : ClusterConfig(
            name = "little",
            minFreqPath = SoCUtils.MIN_FREQ_CPU0,
            maxFreqPath = SoCUtils.MAX_FREQ_CPU0,
            currentFreqPath = SoCUtils.CURRENT_FREQ_CPU0,
            govPath = SoCUtils.GOV_CPU0,
            availableFreqPath = SoCUtils.AVAILABLE_FREQ_CPU0,
            availableGovPath = SoCUtils.AVAILABLE_GOV_CPU0,
        )

        data class Big(val cpuIndex: Int) : ClusterConfig(
            name = "big",
            minFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_min_freq",
            maxFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_max_freq",
            currentFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_cur_freq",
            govPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_governor",
            availableFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_available_frequencies",
            availableGovPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_available_governors",
            availableBoostFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_boost_frequencies",
        )

        data class Prime(val cpuIndex: Int) : ClusterConfig(
            name = "prime",
            minFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_min_freq",
            maxFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_max_freq",
            currentFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_cur_freq",
            govPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_governor",
            availableFreqPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_available_frequencies",
            availableGovPath = "/sys/devices/system/cpu/cpufreq/policy$cpuIndex/scaling_available_governors",
        )
    }

    private val _cpu0State = MutableStateFlow(CPUState.EMPTY)
    val cpu0State: StateFlow<CPUState> = _cpu0State

    private val _cpuCoreStates = MutableStateFlow<List<SoCUtils.CpuCoreState>>(emptyList())
    val cpuCoreStates: StateFlow<List<SoCUtils.CpuCoreState>> = _cpuCoreStates

    private val _cpuCoreMetrics = MutableStateFlow<List<SoCUtils.CpuCoreMetric>>(emptyList())
    val cpuCoreMetrics: StateFlow<List<SoCUtils.CpuCoreMetric>> = _cpuCoreMetrics

    private val _cpuUsage = MutableStateFlow("N/A")
    val cpuUsage: StateFlow<String> = _cpuUsage

    private val _cpuTemp = MutableStateFlow("N/A")
    val cpuTemp: StateFlow<String> = _cpuTemp

    private val _hasCpuInputBoostMs = MutableStateFlow(false)
    val hasCpuInputBoostMs: StateFlow<Boolean> = _hasCpuInputBoostMs

    private val _cpuInputBoostMs = MutableStateFlow("N/A")
    val cpuInputBoostMs: StateFlow<String> = _cpuInputBoostMs

    private val _hasCpuSchedBoostOnInput = MutableStateFlow(false)
    val hasCpuSchedBoostOnInput: StateFlow<Boolean> = _hasCpuSchedBoostOnInput

    private val _cpuSchedBoostOnInput = MutableStateFlow("0")
    val cpuSchedBoostOnInput: StateFlow<String> = _cpuSchedBoostOnInput

    private val _bigClusterState = MutableStateFlow(CPUState.EMPTY)
    val bigClusterState: StateFlow<CPUState> = _bigClusterState

    private val _primeClusterState = MutableStateFlow(CPUState.EMPTY)
    val primeClusterState: StateFlow<CPUState> = _primeClusterState

    private val _gpuState = MutableStateFlow(GPUState.EMPTY)
    val gpuState: StateFlow<GPUState> = _gpuState

    private val _ramState = MutableStateFlow(RamState())
    val ramState: StateFlow<RamState> = _ramState

    private val _gpuTemp = MutableStateFlow("N/A")
    val gpuTemp: StateFlow<String> = _gpuTemp

    private val _gpuUsage = MutableStateFlow("N/A")
    val gpuUsage: StateFlow<String> = _gpuUsage

    private val _cpuBoostEnabled = MutableStateFlow(false)
    val cpuBoostEnabled: StateFlow<Boolean> = _cpuBoostEnabled
    private val _forceCpuBoostEnabled = MutableStateFlow(false)
    val forceCpuBoostEnabled: StateFlow<Boolean> = _forceCpuBoostEnabled
    private val _boostUpperBound = MutableStateFlow("100")
    val boostUpperBound: StateFlow<String> = _boostUpperBound
    private val _deboostReduce = MutableStateFlow("0")
    val deboostReduce: StateFlow<String> = _deboostReduce

    private val _gpuBoostEnabled = MutableStateFlow(false)
    val gpuBoostEnabled: StateFlow<Boolean> = _gpuBoostEnabled
    private val _gpuBoostEnable = MutableStateFlow(false)
    val gpuBoostEnable: StateFlow<Boolean> = _gpuBoostEnable
    private val _gedBoostEnabled = MutableStateFlow(false)
    val gedBoostEnabled: StateFlow<Boolean> = _gedBoostEnabled
    private val _gpuDvfsEnabled = MutableStateFlow(false)
    val gpuDvfsEnabled: StateFlow<Boolean> = _gpuDvfsEnabled
    private val _gpuEffectiveLimit = MutableStateFlow("N/A")
    val gpuEffectiveLimit: StateFlow<String> = _gpuEffectiveLimit
    private val _gpuBoostFrequency = MutableStateFlow("N/A")
    val gpuBoostFrequency: StateFlow<String> = _gpuBoostFrequency

    private val _hasBigCluster = MutableStateFlow(false)
    val hasBigCluster: StateFlow<Boolean> = _hasBigCluster

    private val _hasPrimeCluster = MutableStateFlow(false)
    val hasPrimeCluster: StateFlow<Boolean> = _hasPrimeCluster

    private val _hasDefaultPwrlevel = MutableStateFlow(false)
    val hasDefaultPwrlevel: StateFlow<Boolean> = _hasDefaultPwrlevel

    private val _hasAdrenoBoost = MutableStateFlow(false)
    val hasAdrenoBoost: StateFlow<Boolean> = _hasAdrenoBoost

    private val _hasGPUThrottling = MutableStateFlow(false)
    val hasGPUThrottling: StateFlow<Boolean> = _hasGPUThrottling

    private var job: Job? = null
    private var detectedBigClusterConfig: ClusterConfig.Big? = null
    private var detectedPrimeClusterConfig: ClusterConfig.Prime? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSoCData()
        }
    }

    fun startJob() {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            settingsPreference.pollingInterval.collect { interval ->
                while (true) {
                    loadSoCData()
                    delay(interval)
                }
            }
        }
    }

    fun stopJob() {
        job?.cancel()
        job = null
    }

    private fun loadSoCData() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            loadCPUData()
            loadGPUData()
            loadRamData(context)
            loadTemperatureAndUsageData(context)
        }
    }

    private fun loadCPUData() {
        _cpuCoreStates.value = SoCUtils.readCpuCoreStates()
        _cpuCoreMetrics.value = SoCUtils.readCpuCoreMetrics()
        _cpu0State.value = loadClusterState(ClusterConfig.Little)

        val detectedClusters = detectClusterConfigs()
        detectedBigClusterConfig = detectedClusters.first
        detectedPrimeClusterConfig = detectedClusters.second

        _hasBigCluster.value = detectedBigClusterConfig != null
        detectedBigClusterConfig?.let { config ->
            _bigClusterState.value = loadClusterStateWithBoost(config)
        }

        _hasPrimeCluster.value = detectedPrimeClusterConfig != null
        detectedPrimeClusterConfig?.let { config ->
            _primeClusterState.value = loadClusterState(config)
        }

        _hasCpuInputBoostMs.value = Utils.testFile(SoCUtils.CPU_INPUT_BOOST_MS)
        _cpuInputBoostMs.value = Utils.readFile(SoCUtils.CPU_INPUT_BOOST_MS)

        _hasCpuSchedBoostOnInput.value = Utils.testFile(SoCUtils.CPU_SCHED_BOOST_ON_INPUT)
        _cpuSchedBoostOnInput.value = Utils.readFile(SoCUtils.CPU_SCHED_BOOST_ON_INPUT)
        loadCpuGedBoost()
    }

    private fun loadGPUData() {
        val isMtk = SoCUtils.isMtkGpu()
        if (isMtk) {
            // Repair a previously crossed GED min/max state before displaying
            // the controls. This prevents "Min 1200 / Max 299" and the
            // resulting 299 MHz clamp on this MT6768 implementation.
            SoCUtils.normalizeMtkGpuLimits()
        }
        val gpuState = if (isMtk) {
            GPUState(
                minFreq = SoCUtils.readMtkGpuMinFreq(),
                maxFreq = SoCUtils.readMtkGpuMaxFreq(),
                currentFreq = SoCUtils.readMtkGpuCurrentFreq(),
                gov = "GED/DVFS",
                maxPwrlevel = "N/A",
                minPwrlevel = "N/A",
                defaultPwrlevel = "N/A",
                adrenoBoost = "0",
                gpuThrottling = "0",
                availableFreq = SoCUtils.readMtkGpuAvailableFreq(),
                availableGov = emptyList(),
            )
        } else {
            GPUState(
                minFreq = Utils.readFile(SoCUtils.MIN_FREQ_GPU),
                maxFreq = Utils.readFile(SoCUtils.MAX_FREQ_GPU),
                currentFreq = SoCUtils.readFreqGPU(SoCUtils.CURRENT_FREQ_GPU),
                gov = Utils.readFile(SoCUtils.GOV_GPU),
                maxPwrlevel = Utils.readFile(SoCUtils.MAX_PWRLEVEL),
                minPwrlevel = Utils.readFile(SoCUtils.MIN_PWRLEVEL),
                defaultPwrlevel = Utils.readFile(SoCUtils.DEFAULT_PWRLEVEL),
                adrenoBoost = Utils.readFile(SoCUtils.ADRENO_BOOST),
                gpuThrottling = Utils.readFile(SoCUtils.GPU_THROTTLING),
                availableFreq = SoCUtils.readAvailableFreqGPU(SoCUtils.AVAILABLE_FREQ_GPU),
                availableGov = SoCUtils.readAvailableGovGPU(SoCUtils.AVAILABLE_GOV_GPU),
            )
        }
        _gpuState.value = gpuState

        _hasDefaultPwrlevel.value = Utils.testFile(SoCUtils.DEFAULT_PWRLEVEL)
        _hasAdrenoBoost.value = Utils.testFile(SoCUtils.ADRENO_BOOST)
        _hasGPUThrottling.value = Utils.testFile(SoCUtils.GPU_THROTTLING)
    }

    private fun loadCpuGedBoost() {
        _cpuBoostEnabled.value = Utils.readFile(CPU_BOOST) == "1"
        _forceCpuBoostEnabled.value = Utils.readFile(FORCE_CPU_BOOST) == "1"
        _boostUpperBound.value = Utils.readFile(BOOST_UPPER_BOUND).ifBlank { "100" }
        _deboostReduce.value = Utils.readFile(DEBOOST_REDUCE).ifBlank { "0" }
    }

    private fun loadGpuGedBoost() {
        _gpuBoostEnabled.value = Utils.readFile(GPU_BOOST) == "1"
        _gpuBoostEnable.value = Utils.readFile(GPU_BOOST_ENABLE) == "1"
        _gedBoostEnabled.value = Utils.readFile(GED_BOOST) == "1"
        _gpuDvfsEnabled.value = Utils.readFile(GPU_DVFS) == "1"
        _gpuBoostFrequency.value = readGpuBoostFrequency()
        _gpuEffectiveLimit.value = readGpuEffectiveLimit()
    }

    fun setCpuBoostEnabled(enabled: Boolean) = writeGedBoolean(CPU_BOOST, enabled) { _cpuBoostEnabled.value = it }
    fun setForceCpuBoostEnabled(enabled: Boolean) = writeGedBoolean(FORCE_CPU_BOOST, enabled) { _forceCpuBoostEnabled.value = it }
    fun setGpuBoostEnabled(enabled: Boolean) = writeGedBoolean(GPU_BOOST, enabled) { _gpuBoostEnabled.value = it }
    fun setGpuBoostEnable(enabled: Boolean) = writeGedBoolean(GPU_BOOST_ENABLE, enabled) { _gpuBoostEnable.value = it }
    fun setGedBoostEnabled(enabled: Boolean) = writeGedBoolean(GED_BOOST, enabled) { _gedBoostEnabled.value = it }
    fun setGpuDvfsEnabled(enabled: Boolean) = writeGedBoolean(GPU_DVFS, enabled) { _gpuDvfsEnabled.value = it }

    private fun readGpuBoostFrequency(): String {
        val raw = Utils.readFile(GPU_BOOST_FREQ).trim()
        return raw.toLongOrNull()?.let { "${it / 1000} MHz" } ?: "N/A"
    }

    private fun readGpuEffectiveLimit(): String = runCatching {
        val dump = Shell.cmd("cat $GPU_VAR_DUMP").exec()
        if (!dump.isSuccess) return "N/A"
        val idx = dump.out.asSequence()
            .mapNotNull { Regex("g_max_limited_idx\\s*=\\s*(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .firstOrNull() ?: return "N/A"
        val line = Shell.cmd("sed -n '${idx + 1}p' $GPU_OPP_DUMP").exec().out.firstOrNull() ?: return "OPP $idx"
        val freq = Regex("freq\\s*=\\s*(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (freq != null) "${freq / 1000} MHz" else "OPP $idx"
    }.getOrDefault("N/A")

    private fun writeGedBoolean(path: String, enabled: Boolean, update: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(path, if (enabled) 1 else 0)
            update(Utils.readFile(path) == "1")
        }
    }

    fun setBoostUpperBound(value: String) = writeGedNumber(BOOST_UPPER_BOUND, value) { _boostUpperBound.value = it }
    fun setDeboostReduce(value: String) = writeGedNumber(DEBOOST_REDUCE, value) { _deboostReduce.value = it }

    private fun writeGedNumber(path: String, value: String, update: (String) -> Unit) {
        val clean = value.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(path, clean)
            update(Utils.readFile(path).ifBlank { clean.toString() })
        }
    }

    private fun loadRamData(context: Context) {
        val ram = SoCUtils.getRamMemoryInfo(context)
        _ramState.value = RamState(
            totalBytes = ram.totalBytes,
            usedBytes = ram.usedBytes,
            freeBytes = ram.freeBytes,
        )
    }

    fun freeRam() {
        viewModelScope.launch(Dispatchers.IO) {
            KernelUtils.freeRam()
            delay(300)
            loadRamData(getApplication<Application>())
        }
    }

    private fun loadTemperatureAndUsageData(context: Context) {
        _cpuUsage.value = SoCUtils.getCpuUsage(context)
        _cpuTemp.value = if (SoCUtils.isMtkGpu()) {
            SoCUtils.getMtkCpuTemperature(context)
        } else {
            Utils.getTemp(context, SoCUtils.CPU_TEMP)
        }
        _gpuTemp.value = if (SoCUtils.isMtkGpu()) {
            SoCUtils.getMtkGpuTemperature(context)
        } else {
            Utils.getTemp(context, SoCUtils.GPU_TEMP)
        }
        _gpuUsage.value = SoCUtils.getGpuUsage(context)
        loadGpuGedBoost()
    }

    private fun detectClusterConfigs(): Pair<ClusterConfig.Big?, ClusterConfig.Prime?> {
        val policies = File("/sys/devices/system/cpu/cpufreq").listFiles()
            ?.mapNotNull { dir ->
                val index = dir.name.removePrefix("policy").toIntOrNull() ?: return@mapNotNull null
                if (index == 0 || !dir.isDirectory) return@mapNotNull null
                val availablePath = File(dir, "scaling_available_frequencies").absolutePath
                if (!Utils.testFile(availablePath)) return@mapNotNull null
                val max = Utils.readFile(File(dir, "scaling_max_freq").absolutePath).trim().toLongOrNull() ?: 0L
                if (max <= 0L) return@mapNotNull null
                Triple(index, max, availablePath)
            }
            ?.sortedWith(compareBy<Triple<Int, Long, String>> { it.second }.thenBy { it.first })
            ?: emptyList()

        if (policies.isEmpty()) return null to null

        val big = ClusterConfig.Big(policies.first().first)
        val prime = if (policies.size >= 2) ClusterConfig.Prime(policies.last().first) else null
        return big to prime
    }

    private fun loadClusterState(config: ClusterConfig): CPUState {
        return CPUState(
            minFreq = SoCUtils.readFreqCPU(config.minFreqPath),
            maxFreq = SoCUtils.readFreqCPU(config.maxFreqPath),
            currentFreq = SoCUtils.readFreqCPU(config.currentFreqPath),
            gov = Utils.readFile(config.govPath),
            availableFreq = SoCUtils.readAvailableFreqCPU(config.availableFreqPath),
            availableGov = SoCUtils.readAvailableGovCPU(config.availableGovPath),
        )
    }

    private fun loadClusterStateWithBoost(config: ClusterConfig.Big): CPUState {
        val availableBoostPath = config.availableBoostFreqPath
        val availableFreq = if (availableBoostPath != null) {
            SoCUtils.readAvailableFreqBoost(config.availableFreqPath, availableBoostPath)
        } else {
            SoCUtils.readAvailableFreqCPU(config.availableFreqPath)
        }

        return CPUState(
            minFreq = SoCUtils.readFreqCPU(config.minFreqPath),
            maxFreq = SoCUtils.readFreqCPU(config.maxFreqPath),
            currentFreq = SoCUtils.readFreqCPU(config.currentFreqPath),
            gov = Utils.readFile(config.govPath),
            availableFreq = availableFreq,
            availableGov = SoCUtils.readAvailableGovCPU(config.availableGovPath),
        )
    }

    fun setCpuCoreOnline(cpu: Int, online: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            SoCUtils.setCpuCoreOnline(cpu, online)
            _cpuCoreStates.value = SoCUtils.readCpuCoreStates()
        }
    }

    fun updateFreq(target: String, selectedFreq: String, cluster: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (cluster) {
                ClusterConfig.Little.name -> updateLittleClusterFreq(target, selectedFreq)
                "big" -> updateBigClusterFreq(target, selectedFreq)
                "prime" -> updatePrimeClusterFreq(target, selectedFreq)
                "gpu" -> updateGPUFreq(target, selectedFreq)
            }
        }
    }

    private fun updateLittleClusterFreq(target: String, selectedFreq: String) {
        val config = ClusterConfig.Little
        val path = if (target == "min") config.minFreqPath else config.maxFreqPath
        SoCUtils.writeFreqCPU(path, selectedFreq)

        _cpu0State.value = _cpu0State.value.copy(
            minFreq = SoCUtils.readFreqCPU(config.minFreqPath),
            maxFreq = SoCUtils.readFreqCPU(config.maxFreqPath),
        )
    }

    private fun updateBigClusterFreq(target: String, selectedFreq: String) {
        val config = detectedBigClusterConfig ?: return
        val path = if (target == "min") config.minFreqPath else config.maxFreqPath
        SoCUtils.writeFreqCPU(path, selectedFreq)

        _bigClusterState.value = _bigClusterState.value.copy(
            minFreq = SoCUtils.readFreqCPU(config.minFreqPath),
            maxFreq = SoCUtils.readFreqCPU(config.maxFreqPath),
        )
    }

    private fun updatePrimeClusterFreq(target: String, selectedFreq: String) {
        val config = detectedPrimeClusterConfig ?: return
        val path = if (target == "min") config.minFreqPath else config.maxFreqPath
        SoCUtils.writeFreqCPU(path, selectedFreq)

        _primeClusterState.value = _primeClusterState.value.copy(
            minFreq = SoCUtils.readFreqCPU(config.minFreqPath),
            maxFreq = SoCUtils.readFreqCPU(config.maxFreqPath),
        )
    }

    private fun updateGPUFreq(target: String, selectedFreq: String) {
        if (SoCUtils.isMtkGpu()) {
            // This device exposes both real GED controls:
            // custom_boost_gpu_freq = minimum-frequency floor
            // custom_upbound_gpu_freq = maximum-frequency ceiling
            if (target == "min") {
                SoCUtils.writeMtkGpuMinFreq(selectedFreq)
            } else {
                SoCUtils.writeMtkGpuMaxFreq(selectedFreq)
            }
            _gpuState.value = _gpuState.value.copy(
                minFreq = SoCUtils.readMtkGpuMinFreq(),
                maxFreq = SoCUtils.readMtkGpuMaxFreq(),
            )
            return
        }

        val path = if (target == "min") SoCUtils.MIN_FREQ_GPU else SoCUtils.MAX_FREQ_GPU
        SoCUtils.writeFreqGPU(path, selectedFreq)

        _gpuState.value = _gpuState.value.copy(
            minFreq = Utils.readFile(SoCUtils.MIN_FREQ_GPU),
            maxFreq = Utils.readFile(SoCUtils.MAX_FREQ_GPU),
        )
    }

    fun updateGov(selectedGov: String, cluster: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val governorPath = getGovernorPath(cluster) ?: return@launch
            Utils.writeFile(governorPath, selectedGov)
            updateClusterGovernorState(cluster, governorPath)
        }
    }

    private fun getGovernorPath(cluster: String): String? {
        return when (cluster) {
            ClusterConfig.Little.name -> ClusterConfig.Little.govPath
            "big" -> detectedBigClusterConfig?.govPath
            "prime" -> detectedPrimeClusterConfig?.govPath
            "gpu" -> if (SoCUtils.isMtkGpu()) null else SoCUtils.GOV_GPU
            else -> null
        }
    }

    private fun updateClusterGovernorState(cluster: String, governorPath: String) {
        val newGovernor = Utils.readFile(governorPath)
        when (cluster) {
            ClusterConfig.Little.name -> {
                _cpu0State.value = _cpu0State.value.copy(gov = newGovernor)
            }

            "big" -> {
                _bigClusterState.value = _bigClusterState.value.copy(gov = newGovernor)
            }

            "prime" -> {
                _primeClusterState.value = _primeClusterState.value.copy(gov = newGovernor)
            }

            "gpu" -> {
                _gpuState.value = _gpuState.value.copy(gov = newGovernor)
            }
        }
    }

    fun updateCpuInputBoostMs(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(SoCUtils.CPU_INPUT_BOOST_MS, value)
            _cpuInputBoostMs.value = Utils.readFile(SoCUtils.CPU_INPUT_BOOST_MS)
        }
    }

    fun updateCpuSchedBoostOnInput(isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = if (isEnabled) "1" else "0"

            Utils.writeFile(SoCUtils.CPU_SCHED_BOOST_ON_INPUT, value)
            _cpuSchedBoostOnInput.value = Utils.readFile(SoCUtils.CPU_SCHED_BOOST_ON_INPUT)
        }
    }

    fun updateDefaultPwrlevel(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(SoCUtils.DEFAULT_PWRLEVEL, value)
            _gpuState.value = _gpuState.value.copy(
                defaultPwrlevel = Utils.readFile(SoCUtils.DEFAULT_PWRLEVEL),
            )
        }
    }

    fun updateAdrenoBoost(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Utils.writeFile(SoCUtils.ADRENO_BOOST, value)
            _gpuState.value = _gpuState.value.copy(
                adrenoBoost = Utils.readFile(SoCUtils.ADRENO_BOOST),
            )
        }
    }

    fun updateGPUThrottling(isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = if (isChecked) "1" else "0"
            Utils.writeFile(SoCUtils.GPU_THROTTLING, newValue)
            _gpuState.value = _gpuState.value.copy(
                gpuThrottling = Utils.readFile(SoCUtils.GPU_THROTTLING),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        stopJob()
    }
}
