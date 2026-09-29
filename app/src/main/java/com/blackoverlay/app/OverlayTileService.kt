package com.blackoverlay.app

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class OverlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state =
            if (Prefs.isRunning(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!OverlayService.hasOverlayPermission(this)) {
            // No collapse-with-Intent API on older Android, so use a PendingIntent.
            val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return
            val pi = android.app.PendingIntent.getActivity(
                this, 0, launch,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(launch)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(pi)
                }
            } catch (_: Exception) {}
            return
        }
        if (Prefs.isRunning(this)) {
            OverlayService.stop(this)
            qsTile?.state = Tile.STATE_INACTIVE
        } else {
            OverlayService.start(this)
            qsTile?.state = Tile.STATE_ACTIVE
        }
        qsTile?.updateTile()
    }
}
