package com.example.expensetracker;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupWorker extends Worker {
    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull
    @Override public Result doWork() {
        try {
            // Salvează un backup JSON în stocarea internă a aplicației (folderul files/)
            Context ctx = getApplicationContext();
            String name = "backup-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()) + ".json";
            Uri uri = Uri.fromFile(new java.io.File(ctx.getFilesDir(), name));
            AppDatabase db = AppDatabase.getInstance(ctx);
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                // fallback: folosim un stream normal dacă SAF nu poate deschide content:// pentru file://
                if (os == null) try (OutputStream fos = new java.io.FileOutputStream(uri.getPath())) {
                    ExportImportUtils.exportJsonToStream(ctx, db, fos);
                } else {
                    ExportImportUtils.exportJsonToStream(ctx, db, os);
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
