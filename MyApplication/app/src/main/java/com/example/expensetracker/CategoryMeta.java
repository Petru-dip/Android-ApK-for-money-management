package com.example.expensetracker;

import android.content.Context;

import androidx.annotation.ColorInt;

import java.util.Locale;

/** Helper pentru alegea culorilor/iconițelor pentru categorii/tipuri. */
public final class CategoryMeta {

    private CategoryMeta() {}

    @ColorInt
    public static int pickColor(Context ctx, String key) {
        if (key == null) return 0xFF90CAF9; // default blue
        String k = key.toLowerCase(Locale.getDefault());
        if (k.contains("mâncare") || k.contains("mancare")) return 0xFFFFCDD2; // red100
        if (k.contains("sănătate") || k.contains("sanatate")) return 0xFFC8E6C9; // green100
        if (k.contains("transport")) return 0xFFB3E5FC; // light blue100
        if (k.contains("casă") || k.contains("casa")) return 0xFFFFF9C4; // yellow100
        if (k.contains("salariu")) return 0xFFA5D6A7; // green200
        if (k.contains("bonus")) return 0xFFFFE082; // amber200
        if (k.contains("familie")) return 0xFFCE93D8; // purple200
        if (k.contains("primit") || k.contains("cadou") || k.contains("transfer")) return 0xFF80DEEA; // cyan200
        return 0xFFCFD8DC; // blue grey100
    }
}


