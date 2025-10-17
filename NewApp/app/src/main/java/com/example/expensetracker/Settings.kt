package com.example.expensetracker

import android.content.Context

object Settings {
    private const val PREFS = "settings"
    private const val KEY_AUTO = "auto_save_mode"

    @JvmStatic
    fun isAutoSaveOn(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO, false)

    @JvmStatic
    fun setAutoSave(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO, on).apply()
    }

    @JvmStatic
    fun toggleAutoSave(ctx: Context): Boolean {
        val new = !isAutoSaveOn(ctx)
        setAutoSave(ctx, new)
        return new
    }
}
