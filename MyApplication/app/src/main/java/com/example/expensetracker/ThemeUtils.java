package com.example.expensetracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {

    private static final String PREFS = "theme_prefs";
    private static final String KEY_MODE = "theme_mode";

    /** 🔹 Aplică tema salvată din preferințe */
    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /** 🔹 Setează, salvează și aplică imediat tema nouă */
    public static void setTheme(Context context, int mode) {
        // Salvează noua temă
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MODE, mode)
                .apply();

        // Aplică tema
        AppCompatDelegate.setDefaultNightMode(mode);

        // Reîncarcă ecranul pentru a actualiza UI-ul
        if (context instanceof Activity) {
            ((Activity) context).recreate();
        }
    }

    /** 🔹 Returnează tema curentă (pentru toggle rapid) */
    public static int getMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
