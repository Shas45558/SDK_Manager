package com.sdkm.manager.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkm.manager.utils.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : ViewModel() {
    data class Policy(val index: Int, val name: String, val enabled: Boolean)
    data class State(
        val cpuTemp: String = "N/A",
        val tripPoints: List<Pair<String, String>> = emptyList(),
        val thermalCurrentPower: String = "N/A",
        val thermalPowerRange: String = "N/A",
        val thermalLimit: String = "N/A",
        val thermalActive: Boolean = false,
        val cpuAdaptive: String = "N/A",
        val systemReset: String = "N/A",
        val ppmAvailable: Boolean = false,
        val policies: List<Policy> = emptyList(),
        val writeError: String? = null,
    )

    companion object {
        private const val THERMAL_ROOT = "/sys/class/thermal"
        private const val PPM_POLICY_STATUS = "/proc/ppm/policy_status"
        private const val PPM_THERMAL_POWER = "/proc/ppm/policy/thermal_cur_power"
        private const val PPM_THERMAL_LIMIT = "/proc/ppm/policy/thermal_limit"
        private val POLICY_NAMES = mapOf(0 to "PTPOD", 1 to "SYS_BOOST", 2 to "PWR_THRO", 3 to "THERMAL", 4 to "DLPT", 5 to "LCM_OFF")
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun start() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(1000)
            }
        }
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun refresh() {
        val zone = findThermalZone("mtktscpu")
        val temp = zone?.let { readInt("$it/temp")?.let { value -> "%.1f°C".format(value / 1000f) } } ?: "N/A"
        val trips = zone?.let { readTripPoints(it) } ?: emptyList()

        val powerText = Utils.readFile(PPM_THERMAL_POWER)
        val powerCurrent = Regex("current power\\s*=\\s*(-?\\d+)").find(powerText)?.groupValues?.get(1)
        val powerMin = Regex("min power\\s*=\\s*(-?\\d+)").find(powerText)?.groupValues?.get(1)
        val powerMax = Regex("max power\\s*=\\s*(-?\\d+)").find(powerText)?.groupValues?.get(1)

        val limitText = Utils.readFile(PPM_THERMAL_LIMIT)
        val limitedPower = Regex("limited power\\s*=\\s*(-?\\d+)").find(limitText)?.groupValues?.get(1)
        val active = Regex("PPM thermal activate\\s*=\\s*(\\d+)").find(limitText)?.groupValues?.get(1) == "1"

        val adaptive = findCoolingDevice("cpu_adaptive_0")?.let { Utils.readFile("$it/cur_state") } ?: "N/A"
        val reset = findCoolingDevice("mtktscpu-sysrst")?.let { Utils.readFile("$it/cur_state") } ?: "N/A"
        val policies = parsePolicies(Utils.readFile(PPM_POLICY_STATUS))

        _state.value = State(
            cpuTemp = temp,
            tripPoints = trips,
            thermalCurrentPower = powerCurrent ?: "N/A",
            thermalPowerRange = if (powerMin != null && powerMax != null) "$powerMin – $powerMax" else "N/A",
            thermalLimit = limitedPower ?: "N/A",
            thermalActive = active,
            cpuAdaptive = adaptive,
            systemReset = reset,
            ppmAvailable = File("/proc/ppm").exists() || Utils.testFile(PPM_POLICY_STATUS),
            policies = policies,
            writeError = null,
        )
    }

    fun setPolicy(index: Int, enabled: Boolean) {
        val ok = Utils.writeFile(PPM_POLICY_STATUS, "$index ${if (enabled) 1 else 0}")
        if (ok) refresh() else _state.value = _state.value.copy(writeError = "Failed to change PPM policy $index. Root access may be required.")
    }

    private fun findThermalZone(type: String): String? = runCatching {
        File(THERMAL_ROOT).listFiles()?.firstOrNull { dir ->
            dir.name.startsWith("thermal_zone") && Utils.readFile("${dir.path}/type").trim() == type
        }?.path
    }.getOrNull()

    private fun findCoolingDevice(type: String): String? = runCatching {
        File(THERMAL_ROOT).listFiles()?.firstOrNull { dir ->
            dir.name.startsWith("cooling_device") && Utils.readFile("${dir.path}/type").trim() == type
        }?.path
    }.getOrNull()

    private fun readInt(path: String): Long? = Utils.readFile(path).trim().toLongOrNull()

    private fun readTripPoints(zone: String): List<Pair<String, String>> = buildList {
        for (i in 0..15) {
            val value = readInt("$zone/trip_point_${i}_temp") ?: continue
            add("Trip $i" to "%.1f°C".format(value / 1000f))
        }
    }

    private fun parsePolicies(text: String): List<Policy> = text.lineSequence().mapNotNull { line ->
        val match = Regex("\\[(\\d+)]\\s+PPM_POLICY_([A-Z_]+):\\s+(enabled|disabled)").find(line) ?: return@mapNotNull null
        val index = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        Policy(index, POLICY_NAMES[index] ?: match.groupValues[2], match.groupValues[3] == "enabled")
    }.sortedBy { it.index }.toList()

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
