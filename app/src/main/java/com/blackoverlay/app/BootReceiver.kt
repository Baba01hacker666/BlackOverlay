package com.blackoverlay.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.startOnBoot(context) || Prefs.isRunning(context)) {
                if (OverlayService.hasOverlayPermission(context)) {
                    OverlayService.start(context)
                }
            }
        }
    }
}
