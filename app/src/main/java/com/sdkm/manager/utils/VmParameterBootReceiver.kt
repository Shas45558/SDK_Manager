/*
 * Copyright (c) 2025 Rve <rve27github@gmail.com>
 *
 * GNU General Public License v3.0
 */
package com.sdkm.manager.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Applies the saved VM parameters after boot when Self-booting is enabled. */
class VmParameterBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("vm_parameters", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                prefs.getString("swappiness", null)?.let {
                    Utils.writeFile(KernelUtils.SWAPPINESS, it)
                }
                prefs.getString("extra_free_kbytes", null)?.let {
                    Utils.writeFile(KernelUtils.EXTRA_FREE_KBYTES, it)
                }
                prefs.getString("watermark_scale_factor", null)?.let {
                    Utils.writeFile(KernelUtils.WATERMARK_SCALE_FACTOR, it)
                }
            } catch (t: Throwable) {
                Log.e(KernelUtils.TAG, "Failed to apply saved VM parameters", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
