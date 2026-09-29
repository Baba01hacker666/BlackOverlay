package com.blackoverlay.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    private const val NAME = "blackoverlay_prefs"
    const val KEY_OPACITY = "opacity" // 0..100
    const val KEY_RUNNING = "running"
    const val KEY_START_ON_BOOT = "start_on_boot"
    const val KEY_BLOCK_TOUCH = "block_touch"
    const val KEY_SHOW_NOTIFICATION = "show_notification"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getOpacity(context: Context): Int =
        prefs(context).getInt(KEY_OPACITY, 50).coerceIn(0, 100)

    fun setOpacity(context: Context, value: Int) =
        prefs(context).edit { putInt(KEY_OPACITY, value.coerceIn(0, 100)) }

    fun isRunning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) =
        prefs(context).edit { putBoolean(KEY_RUNNING, running) }

    fun startOnBoot(context: Context): Boolean =
        prefs(context).getBoolean(KEY_START_ON_BOOT, false)

    fun setStartOnBoot(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_START_ON_BOOT, value) }

    fun blockTouch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_TOUCH, false)

    fun setBlockTouch(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_BLOCK_TOUCH, value) }
}
