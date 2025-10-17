package com.example.expensetracker;

import android.content.Context;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class AutoBackup {
    public static void scheduleDaily(Context ctx) {
        WorkRequest req = new PeriodicWorkRequest.Builder(BackupWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true).build())
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "auto_backup_json", ExistingPeriodicWorkPolicy.KEEP, (PeriodicWorkRequest) req);
    }
}
