package com.sdkm.manager.utils

import com.topjohnwu.superuser.Shell
import java.util.Locale
import kotlin.math.roundToInt

data class DisplayState(
    val width: Int = 1080,
    val height: Int = 2340,
    val density: Int = 440,
    val refreshRate: Float = 60f,
    val supportedRefreshRates: List<Float> = listOf(60f),
    val brightness: Int = 0,
    val maxBrightness: Int = 2047,
    val hbmMode: Int = 0,
)

object DisplayUtils {
    const val BASE_WIDTH = 1080
    const val BASE_HEIGHT = 2340
    const val BASE_DENSITY = 440
    const val HBM_PATH = "/sys/devices/platform/mtkfb@0/graphics/fb0/mtk_fb_hbm"
    const val BACKLIGHT_PATH = "/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/brightness"
    const val BACKLIGHT_MAX_PATH = "/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/max_brightness"

    private fun shell(command: String): List<String> = Shell.cmd(command).exec().out

    fun readState(): DisplayState {
        val wm = shell("wm size; wm density").joinToString("\n")
        val size = Regex("(?:Override|Physical) size: (\\d+)x(\\d+)").findAll(wm).lastOrNull()
            ?: Regex("size: (\\d+)x(\\d+)").find(wm)
        val density = Regex("(?:Override|Physical) density: (\\d+)").findAll(wm).lastOrNull()
            ?: Regex("density: (\\d+)").find(wm)
        val display = shell("dumpsys display").joinToString("\n")
        val rates = Regex("supportedRefreshRates=\\[([^]]+)\\]").find(display)?.groupValues?.get(1)
            ?.split(',')?.mapNotNull { it.trim().toFloatOrNull() }?.distinct()?.sorted()
            ?: listOf(60f)
        val active = Regex("renderFrameRate=([0-9.]+)").find(display)?.groupValues?.get(1)?.toFloatOrNull()
            ?: rates.lastOrNull() ?: 60f
        val brightness = readInt(BACKLIGHT_PATH)
        val max = readInt(BACKLIGHT_MAX_PATH).takeIf { it > 0 } ?: 2047
        val hbm = Regex("hbm_mode:(\\d+)").find(read(HBM_PATH))?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return DisplayState(
            width = size?.groupValues?.get(1)?.toIntOrNull() ?: BASE_WIDTH,
            height = size?.groupValues?.get(2)?.toIntOrNull() ?: BASE_HEIGHT,
            density = density?.groupValues?.get(1)?.toIntOrNull() ?: BASE_DENSITY,
            refreshRate = active,
            supportedRefreshRates = rates,
            brightness = brightness,
            maxBrightness = max,
            hbmMode = hbm,
        )
    }

    fun setResolution(width: Int, height: Int): Boolean {
        if (width < 240 || height < 240 || width > 4320 || height > 4320) return false
        return Shell.cmd("wm size ${width}x${height}").exec().isSuccess
    }

    fun setDensity(density: Int): Boolean {
        if (density !in 120..800) return false
        return Shell.cmd("wm density $density").exec().isSuccess
    }

    fun resetDisplay(): Boolean {
        val result = Shell.cmd("wm size ${BASE_WIDTH}x${BASE_HEIGHT}; wm density $BASE_DENSITY").exec()
        return result.isSuccess
    }

    fun setRefreshRate(rate: Float): Boolean {
        if (rate <= 0f) return false
        val value = String.format(Locale.US, "%.0f", rate)
        return Shell.cmd("settings put system peak_refresh_rate $value; settings put system min_refresh_rate $value").exec().isSuccess
    }

    fun setBrightness(percent: Int): Boolean {
        val max = readInt(BACKLIGHT_MAX_PATH).takeIf { it > 0 } ?: 2047
        val value = (max * percent.coerceIn(0, 100) / 100f).roundToInt()
        return Shell.cmd("echo $value > $BACKLIGHT_PATH").exec().isSuccess
    }

    fun setHbm(mode: Int): Boolean {
        if (mode !in 0..2) return false
        return Shell.cmd("echo $mode > $HBM_PATH").exec().isSuccess
    }

    fun resolutionForPercent(percent: Int): Pair<Int, Int> {
        val p = percent.coerceIn(50, 100) / 100f
        return (BASE_WIDTH * p).roundToInt() to (BASE_HEIGHT * p).roundToInt()
    }

    fun densityForPercent(percent: Int): Int = (BASE_DENSITY * (percent.coerceIn(50, 150) / 100f)).roundToInt()

    private fun read(path: String): String = Shell.cmd("cat $path").exec().out.joinToString("\n").trim()
    private fun readInt(path: String): Int = read(path).trim().toIntOrNull() ?: 0
}
