package com.example.expensetracker;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Material You dynamic color (Monet) on supported devices
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
