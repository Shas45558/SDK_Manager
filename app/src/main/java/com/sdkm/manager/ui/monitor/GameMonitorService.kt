package com.sdkm.manager.ui.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.sdkm.manager.R
import com.sdkm.manager.ui.MainActivity
import com.sdkm.manager.utils.KernelUtils
import com.sdkm.manager.utils.SoCUtils
import com.sdkm.manager.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GameMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var cpuFreqText: TextView? = null
    private var gpuFreqText: TextView? = null
    private var ramText: TextView? = null
    private var zramText: TextView? = null
    private var cpuBar: ProgressBar? = null
    private var gpuBar: ProgressBar? = null
    private var ramBar: ProgressBar? = null
    private var zramBar: ProgressBar? = null

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private var showCpu = true
    private var showGpu = true
    private var showRam = true
    private var showZram = true
    private var lastCpu = 0
    private var lastGpu = 0
    private var lastRam = 0
    private var overlayEnabled = true

    override fun onCreate() {
        super.onCreate()
        loadMetricPreferences()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (overlayEnabled && Settings.canDrawOverlays(this)) showOverlay()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_START_NOTIFICATION -> overlayEnabled = false
            ACTION_START_OVERLAY, ACTION_START -> overlayEnabled = true
        }
        if (overlayEnabled && !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (overlayEnabled && overlay == null) showOverlay()
        if (!overlayEnabled && overlay != null) { runCatching { windowManager?.removeView(overlay) }; overlay = null }
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val stats = readStats()
                withContext(Dispatchers.Main.immediate) {
                    applyStats(stats)
                }
                delay(1000)
            }
        }
    }

    private data class MonitorStats(
        val cpuUsage: Int,
        val gpuUsage: Int,
        val cpuFreq: Long,
        val gpuFreq: Int,
        val ramUsed: Long,
        val ramTotal: Long,
        val zramUsed: Long,
        val zramTotal: Long,
    )

    private fun readStats(): MonitorStats {
        val cpuUsage = SoCUtils.getCpuUsage(this).toIntOrNull()?.coerceIn(0, 100) ?: 0
        val gpuUsage = SoCUtils.getGpuUsage(this).toIntOrNull()?.coerceIn(0, 100) ?: 0
        val ram = SoCUtils.getRamMemoryInfo(this)
        val zram = KernelUtils.getZramMemoryInfo()

        val cpuFreq = listOf(
            SoCUtils.CURRENT_FREQ_CPU0,
            SoCUtils.CURRENT_FREQ_CPU3,
            SoCUtils.CURRENT_FREQ_CPU4,
            SoCUtils.CURRENT_FREQ_CPU6,
            SoCUtils.CURRENT_FREQ_CPU7,
        ).mapNotNull { Utils.readFile(it).toLongOrNull() }.maxOrNull()?.let { it / 1000 } ?: 0

        val gpuFreq = if (SoCUtils.isMtkGpu()) {
            SoCUtils.readMtkGpuCurrentFreq().toIntOrNull() ?: 0
        } else {
            Utils.readFile(SoCUtils.CURRENT_FREQ_GPU).toLongOrNull()?.div(1000)?.toInt() ?: 0
        }

        return MonitorStats(cpuUsage, gpuUsage, cpuFreq, gpuFreq, ram.usedBytes, ram.totalBytes, zram.usedBytes, zram.totalBytes)
    }

    private fun applyStats(stats: MonitorStats) {
        val cpuUsage = stats.cpuUsage
        val gpuUsage = stats.gpuUsage
        val ramUsedPercent = if (stats.ramTotal > 0) (stats.ramUsed * 100 / stats.ramTotal).toInt().coerceIn(0, 100) else 0
        val zramUsedPercent = if (stats.zramTotal > 0) (stats.zramUsed * 100 / stats.zramTotal).toInt().coerceIn(0, 100) else 0
        lastCpu = cpuUsage; lastGpu = gpuUsage; lastRam = ramUsedPercent

        cpuFreqText?.text = "CPU ${cpuUsage}%"
        gpuFreqText?.text = "GPU ${gpuUsage}%"
        ramText?.text = "RAM ${ramUsedPercent}%"
        zramText?.text = "ZRAM ${zramUsedPercent}%"
    }

    private fun showOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(3), dp(4), dp(3))
            background = GradientDrawable().apply {
                setColor(Color.argb(150, 8, 16, 28))
                setStroke(dp(1), Color.argb(120, 92, 105, 255))
                cornerRadius = dp(9).toFloat()
            }
        }

        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(3), 0, dp(2), 0)
            setOnClickListener { showMonitorOptions() }
        }

        if (showCpu) addCompactMetric(root, "CPU") { cpuFreqText = it }
        if (showGpu) addCompactMetric(root, "GPU") { gpuFreqText = it }
        if (showRam) addCompactMetric(root, "RAM") { ramText = it }
        if (showZram) addCompactMetric(root, "ZRAM") { zramText = it }
        root.addView(settings, LinearLayout.LayoutParams(dp(22), dp(24)))

        val params = WindowManager.LayoutParams(
            dp(250),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(5)
            y = dp(70)
        }

        makeDraggable(root, params)
        overlay = root
        windowManager?.addView(root, params)
    }

    private fun addCompactMetric(root: LinearLayout, label: String, bind: (TextView) -> Unit) {
        val text = TextView(this).apply {
            text = "$label 0%"
            textSize = 9.5f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setSingleLine(true)
            setPadding(dp(2), 0, dp(2), 0)
        }
        root.addView(text, LinearLayout.LayoutParams(0, dp(24), 1f))
        bind(text)
    }

    private fun loadMetricPreferences() {
        showCpu = prefs.getBoolean(KEY_CPU, true)
        showGpu = prefs.getBoolean(KEY_GPU, true)
        showRam = prefs.getBoolean(KEY_RAM, true)
        showZram = prefs.getBoolean(KEY_ZRAM, true)
        if (!showCpu && !showGpu && !showRam && !showZram) {
            showCpu = true
            prefs.edit().putBoolean(KEY_CPU, true).apply()
        }
    }

    private fun showMonitorOptions() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(2))
        }
        val cpu = CheckBox(this).apply { text = "CPU"; isChecked = showCpu }
        val gpu = CheckBox(this).apply { text = "GPU"; isChecked = showGpu }
        val ram = CheckBox(this).apply { text = "RAM"; isChecked = showRam }
        val zram = CheckBox(this).apply { text = "ZRAM"; isChecked = showZram }
        box.addView(cpu); box.addView(gpu); box.addView(ram); box.addView(zram)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Monitor items")
            .setView(box)
            .setNegativeButton("Stop") { _, _ ->
                stopSelf()
                prefs.edit().putBoolean("enabled", false).apply()
            }
            .setPositiveButton("Apply", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!cpu.isChecked && !gpu.isChecked && !ram.isChecked && !zram.isChecked) return@setOnClickListener
                showCpu = cpu.isChecked
                showGpu = gpu.isChecked
                showRam = ram.isChecked
                showZram = zram.isChecked
                prefs.edit()
                    .putBoolean(KEY_CPU, showCpu)
                    .putBoolean(KEY_GPU, showGpu)
                    .putBoolean(KEY_RAM, showRam)
                    .putBoolean(KEY_ZRAM, showZram)
                    .apply()
                overlay?.let { view -> runCatching { windowManager?.removeView(view) } }
                overlay = null
                cpuFreqText = null; gpuFreqText = null; ramText = null; zramText = null
                showOverlay()
                dialog.dismiss()
            }
        }
        dialog.window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE)
        dialog.show()
    }

    private fun addMetric(root: LinearLayout, label: String, initialFreq: String, bind: (TextView) -> Unit): ProgressBar {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val name = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
        }
        val freq = TextView(this).apply {
            text = initialFreq
            textSize = 10f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
        }
        top.addView(name, LinearLayout.LayoutParams(0, dp(18), 1f))
        top.addView(freq, LinearLayout.LayoutParams(dp(65), dp(18)))
        val bar = progressBar()
        row.addView(top)
        row.addView(bar, LinearLayout.LayoutParams(-1, dp(4)))
        row.setPadding(0, dp(1), 0, dp(2))
        root.addView(row)
        bind(freq)
        return bar
    }

    private fun addMemoryMetric(root: LinearLayout, label: String, bind: (TextView) -> Unit): ProgressBar {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val name = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
        }
        val value = TextView(this).apply {
            text = "0 / 0"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
        }
        top.addView(name, LinearLayout.LayoutParams(0, dp(18), 1f))
        top.addView(value, LinearLayout.LayoutParams(dp(105), dp(18)))
        val bar = progressBar()
        row.addView(top)
        row.addView(bar, LinearLayout.LayoutParams(-1, dp(4)))
        row.setPadding(0, dp(1), 0, dp(2))
        root.addView(row)
        bind(value)
        return bar
    }

    private fun progressBar() = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progress = 0
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> true
            }
        }
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Game Monitor")
            .setContentText("CPU ${lastCpu}% • GPU ${lastGpu}% • RAM ${lastRam}%")
            .setContentIntent(intent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Game Monitor", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val gb = 1024.0 * 1024.0 * 1024.0
        val mb = 1024.0 * 1024.0
        return if (bytes >= gb) String.format(Locale.US, "%.1f GB", bytes / gb) else String.format(Locale.US, "%.0f MB", bytes / mb)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        monitorJob?.cancel()
        overlay?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlay = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.sdkm.manager.monitor.START"
        const val ACTION_START_OVERLAY = "com.sdkm.manager.monitor.START_OVERLAY"
        const val ACTION_START_NOTIFICATION = "com.sdkm.manager.monitor.START_NOTIFICATION"
        const val ACTION_STOP = "com.sdkm.manager.monitor.STOP"
        private const val CHANNEL_ID = "game_monitor"
        private const val NOTIFICATION_ID = 7001
        private const val PREFS_NAME = "monitor_prefs"
        private const val KEY_CPU = "show_cpu"
        private const val KEY_GPU = "show_gpu"
        private const val KEY_RAM = "show_ram"
        private const val KEY_ZRAM = "show_zram"
    }
}
