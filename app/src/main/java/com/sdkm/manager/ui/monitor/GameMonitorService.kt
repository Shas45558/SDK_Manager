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
    private var ramPercentText: TextView? = null
    private var zramText: TextView? = null
    private var zramPercentText: TextView? = null
    private var cpuBar: ProgressBar? = null
    private var gpuBar: ProgressBar? = null
    private var ramBar: ProgressBar? = null
    private var zramBar: ProgressBar? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) showOverlay()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlay == null) showOverlay()
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

        cpuFreqText?.text = "${stats.cpuFreq} MHz"
        gpuFreqText?.text = "${stats.gpuFreq} MHz"
        ramText?.text = "${formatBytes(stats.ramUsed)} / ${formatBytes(stats.ramTotal)}"
        ramPercentText?.text = "$ramUsedPercent%"
        zramText?.text = "${formatBytes(stats.zramUsed)} / ${formatBytes(stats.zramTotal)}"
        zramPercentText?.text = "$zramUsedPercent%"
        cpuBar?.progress = cpuUsage
        gpuBar?.progress = gpuUsage
        ramBar?.progress = ramUsedPercent
        zramBar?.progress = zramUsedPercent
    }


    private fun showOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.argb(238, 8, 20, 35))
                setStroke(dp(1), Color.rgb(92, 105, 255))
                cornerRadius = dp(16).toFloat()
            }
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = "🎮  Game Monitor"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(34), 1f))
        root.addView(header)

        cpuBar = addMetric(root, "CPU", "0 MHz") { cpuFreqText = it }
        gpuBar = addMetric(root, "GPU", "0 MHz") { gpuFreqText = it }
        ramBar = addMemoryMetric(root, "RAM") { ramText = it.first; ramPercentText = it.second }
        zramBar = addMemoryMetric(root, "ZRAM") { zramText = it.first; zramPercentText = it.second }

        val params = WindowManager.LayoutParams(
            dp(300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(10)
            y = dp(90)
        }

        makeDraggable(root, params)
        overlay = root
        windowManager?.addView(root, params)
    }

    private fun addMetric(root: LinearLayout, label: String, initialFreq: String, bind: (TextView) -> Unit): ProgressBar {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val name = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        val freq = TextView(this).apply {
            text = initialFreq
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
        }
        top.addView(name, LinearLayout.LayoutParams(0, dp(24), 1f))
        top.addView(freq, LinearLayout.LayoutParams(dp(78), dp(24)))
        val bar = progressBar()
        row.addView(top)
        row.addView(bar, LinearLayout.LayoutParams(-1, dp(7)))
        row.setPadding(0, dp(2), 0, dp(5))
        root.addView(row)
        bind(freq)
        return bar
    }

    private fun addMemoryMetric(root: LinearLayout, label: String, bind: (Pair<TextView, TextView>) -> Unit): ProgressBar {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val name = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        val value = TextView(this).apply {
            text = "0 / 0"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
        }
        val percent = TextView(this).apply {
            text = "0%"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.END
        }
        top.addView(name, LinearLayout.LayoutParams(0, dp(24), 1f))
        top.addView(value, LinearLayout.LayoutParams(dp(110), dp(24)))
        top.addView(percent, LinearLayout.LayoutParams(dp(40), dp(24)))
        val bar = progressBar()
        row.addView(top)
        row.addView(bar, LinearLayout.LayoutParams(-1, dp(7)))
        row.setPadding(0, dp(2), 0, dp(5))
        root.addView(row)
        bind(value to percent)
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
            .setContentText("CPU • GPU • RAM • ZRAM monitoring is active")
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
        const val ACTION_STOP = "com.sdkm.manager.monitor.STOP"
        private const val CHANNEL_ID = "game_monitor"
        private const val NOTIFICATION_ID = 7001
    }
}
