package com.blackoverlay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "overlay_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.blackoverlay.app.START"
        const val ACTION_STOP = "com.blackoverlay.app.STOP"
        const val ACTION_UPDATE = "com.blackoverlay.app.UPDATE"
        const val EXTRA_OPACITY = "extra_opacity"

        fun start(context: Context, opacity: Int = Prefs.getOpacity(context)) {
            val i = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OPACITY, opacity)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            val i = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun hasOverlayPermission(context: Context): Boolean =
            Settings.canDrawOverlays(context)
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_OPACITY || key == Prefs.KEY_BLOCK_TOUCH) {
                updateOverlayView()
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Prefs.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        removeOverlay()
        Prefs.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        Prefs.setRunning(this, false)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Prefs.setRunning(this, false)
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE, null -> {
                if (intent?.hasExtra(EXTRA_OPACITY) == true) {
                    Prefs.setOpacity(this, intent.getIntExtra(EXTRA_OPACITY, Prefs.getOpacity(this)))
                }
                if (!hasOverlayPermission(this)) {
                    // Can't draw — stop cleanly. MainActivity will prompt for permission.
                    Prefs.setRunning(this, false)
                    stopSelf()
                    return START_NOT_STICKY
                }
                Prefs.setRunning(this, true)
                startForeground(NOTIF_ID, buildNotification())
                showOrUpdateOverlay()
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams {
        val touchFlags = if (Prefs.blockTouch(this)) {
            0 // touchable: consume touches (blocks interaction below)
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            touchFlags or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun opacityToAlpha(opacityPercent: Int): Float =
        (opacityPercent.coerceIn(0, 100) / 100f).coerceIn(0f, 1f)

    @Synchronized
    private fun showOrUpdateOverlay() {
        val wm = windowManager ?: return
        val opacity = Prefs.getOpacity(this)
        if (overlayView == null) {
            val v = View(this).apply {
                setBackgroundColor(Color.BLACK)
                alpha = opacityToAlpha(opacity)
            }
            try {
                wm.addView(v, overlayLayoutParams())
                overlayView = v
            } catch (e: Exception) {
                e.printStackTrace()
                Prefs.setRunning(this, false)
                stopSelf()
            }
        } else {
            updateOverlayView()
        }
        // Refresh notification so it shows current opacity
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification())
    }

    @Synchronized
    private fun updateOverlayView() {
        val v = overlayView ?: return
        val wm = windowManager ?: return
        v.alpha = opacityToAlpha(Prefs.getOpacity(this))
        try {
            wm.updateViewLayout(v, overlayLayoutParams())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun removeOverlay() {
        val v = overlayView ?: return
        try {
            windowManager?.removeViewImmediate(v)
        } catch (e: Exception) {
            try { windowManager?.removeView(v) } catch (_: Exception) {}
        }
        overlayView = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_MIN
                    ).apply {
                        description = getString(R.string.notif_channel_desc)
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        createChannel()
        val opacity = Prefs.getOpacity(this)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, opacity))
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }
}
