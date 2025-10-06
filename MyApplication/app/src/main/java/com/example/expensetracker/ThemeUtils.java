package com.example.expensetracker;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
public class ThemeUtils {
    private static final String PREFS = "theme_prefs";
    private static final String KEY_MODE = "mode";
    public static void applySavedTheme(Context ctx) {
        int mode = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    public static void setTheme(Context ctx, int mode) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
