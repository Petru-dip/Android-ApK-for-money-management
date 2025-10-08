package com.example.expensetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Locale;

public class MainActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private static final String CHANNEL_EXPORT = "export_status";
    private static final int REQ_NOTIFICATION_PERMISSION = 100;

    public static boolean shouldRefreshTotals = false;

    private TextView tvIncome, tvExpense, tvBalance;

    private enum FilterType {ALL, FIRMA, PERSONAL}
    private FilterType currentFilter = FilterType.ALL;

    private MaterialButton btnAll, btnFirma, btnPersonal;

    private final ActivityResultLauncher<String> exportExcelLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    uri -> {
                        if (uri == null) return;
                        new Thread(() -> {
                            try {
                                ExportImportUtils.exportToExcel(this, AppDatabase.getInstance(this), uri);
                                notifyExport("Export Excel", "Fișier XLSX salvat cu succes.");
                                shouldRefreshTotals = true;
                                runOnUiThread(this::updateTotals);
                            } catch (Exception e) {
                                runOnUiThread(() -> new AlertDialog.Builder(this)
                                        .setTitle("Eroare la export")
                                        .setMessage(e.getMessage())
                                        .setPositiveButton("OK", null).show());
                            }
                        }).start();
                    });

    private final ActivityResultLauncher<String[]> importExcelLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) return;
                        new Thread(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(this);
                                ExportImportUtils.PendingExcelImport pending =
                                        ExportImportUtils.prepareImportFromExcel(this, db, uri);

                                runOnUiThread(() -> {
                                    if (pending.hasDuplicates()) {
                                        String msg = "Am găsit " + pending.duplicateExpenses + " cheltuieli și "
                                                + pending.duplicateIncomes + " venituri care există deja în aplicație.\n\n"
                                                + "Cum vrei să continui?";
                                        new AlertDialog.Builder(this)
                                                .setTitle("Import Excel – duplicate detectate")
                                                .setMessage(msg)
                                                .setPositiveButton("Rescrie duplicatele", (d, w) -> {
                                                    // REPLACE_DUPLICATES
                                                    new Thread(() -> {
                                                        ExportImportUtils.ExcelImportResult res =
                                                                ExportImportUtils.commitImport(db, pending,
                                                                        ExportImportUtils.Resolution.REPLACE_DUPLICATES);
                                                        runOnUiThread(() -> {
                                                            new AlertDialog.Builder(this)
                                                                    .setTitle("Import finalizat")
                                                                    .setMessage(res.toString())
                                                                    .setPositiveButton("OK", null)
                                                                    .show();
                                                            MainActivity.shouldRefreshTotals = true;
                                                            updateTotals();
                                                        });
                                                    }).start();
                                                })
                                                .setNeutralButton("Omite duplicatele", (d, w) -> {
                                                    // EXCLUDE_DUPLICATES
                                                    new Thread(() -> {
                                                        ExportImportUtils.ExcelImportResult res =
                                                                ExportImportUtils.commitImport(db, pending,
                                                                        ExportImportUtils.Resolution.EXCLUDE_DUPLICATES);
                                                        runOnUiThread(() -> {
                                                            new AlertDialog.Builder(this)
                                                                    .setTitle("Import finalizat")
                                                                    .setMessage(res.toString())
                                                                    .setPositiveButton("OK", null)
                                                                    .show();
                                                            MainActivity.shouldRefreshTotals = true;
                                                            updateTotals();
                                                        });
                                                    }).start();
                                                })
                                                .setNegativeButton("Anulează", (d, w) -> {
                                                    // CANCEL
                                                    // Nu scriem nimic, doar informăm:
                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Import anulat")
                                                            .setMessage("Nu au fost făcute modificări.")
                                                            .setPositiveButton("OK", null)
                                                            .show();
                                                })
                                                .show();
                                    } else {
                                        // Fără duplicate -> import direct (EXCLUDE_DUPLICATES are același efect)
                                        new Thread(() -> {
                                            ExportImportUtils.ExcelImportResult res =
                                                    ExportImportUtils.commitImport(db, pending,
                                                            ExportImportUtils.Resolution.EXCLUDE_DUPLICATES);
                                            runOnUiThread(() -> {
                                                new AlertDialog.Builder(this)
                                                        .setTitle("Import Excel")
                                                        .setMessage(res.toString())
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                                MainActivity.shouldRefreshTotals = true;
                                                updateTotals();
                                            });
                                        }).start();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> new AlertDialog.Builder(this)
                                        .setTitle("Eroare la import")
                                        .setMessage(String.valueOf(e.getMessage()))
                                        .setPositiveButton("OK", null)
                                        .show());
                            }
                        }).start();
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);
        setupToolbar(R.string.app_name, false); // fără buton back pe ecranul principal

        createExportChannel();

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            drawerLayout.open();
        });
        toolbar.setOnMenuItemClickListener(this::onTopMenuClick);

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this::onDrawerItemClick);

        // Butoane principale
        MaterialButton btnExpense = findViewById(R.id.btn_add_expense);
        MaterialButton btnIncome = findViewById(R.id.btn_add_income);
        btnExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, AddExpenseActivity.class));
        });
        btnIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, IncomeActivity.class));
        });

        // TextView-uri totaluri
        tvIncome = findViewById(R.id.tv_total_income);
        tvExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);

        // Butoane filtrare
        btnAll = findViewById(R.id.btn_total_all);
        btnFirma = findViewById(R.id.btn_total_firma);
        btnPersonal = findViewById(R.id.btn_total_personal);

        btnAll.setOnClickListener(v -> {
            currentFilter = FilterType.ALL;
            highlightButton(btnAll);
            updateTotals();
        });

        btnFirma.setOnClickListener(v -> {
            currentFilter = FilterType.FIRMA;
            highlightButton(btnFirma);
            updateTotals();
        });

        btnPersonal.setOnClickListener(v -> {
            currentFilter = FilterType.PERSONAL;
            highlightButton(btnPersonal);
            updateTotals();
        });

        highlightButton(btnAll);
        updateTotals();
        AutoBackup.scheduleDaily(this);
    }

    private boolean onTopMenuClick(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_reports) {
            startActivity(new Intent(this, ExpenseReportActivity.class));
            return true;
        } else if (id == R.id.action_theme) {
            String[] opts = {"Luminoasă", "Întunecată", "Urmărește sistemul"};
            new AlertDialog.Builder(this)
                    .setTitle("Alege tema")
                    .setItems(opts, (d, which) -> {
                        switch (which) {
                            case 0 -> ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_NO);
                            case 1 -> ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_YES);
                            case 2 -> ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        }
                    }).show();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return false;
    }

    private boolean onDrawerItemClick(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.nav_expenses) {
            startActivity(new Intent(this, ExpenseListActivity.class));
            return true;
        } else if (id == R.id.nav_incomes) {
            startActivity(new Intent(this, IncomeListActivity.class));
            return true;
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, ExpenseReportActivity.class));
            return true;
        } else if (id == R.id.nav_export) {
            exportExcelLauncher.launch("finance-export-" +
                    new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm", java.util.Locale.ROOT)
                            .format(new java.util.Date()) + ".xlsx");
            return true;
        } else if (id == R.id.nav_import) {
            importExcelLauncher.launch(new String[]{
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
            });
            return true;
        } else if (id == R.id.nav_theme) {
            int mode = ThemeUtils.getMode(this) == AppCompatDelegate.MODE_NIGHT_YES
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES;
            ThemeUtils.setTheme(this, mode);
            return true;
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return false;
    }

    private void updateTotals() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            double totalIncome = 0.0;
            double totalExpense = 0.0;

            switch (currentFilter) {
                case ALL -> {
                    totalIncome = db.incomeDao().getTotalAmount();
                    totalExpense = db.expenseDao().getTotalAmount();
                }
                case FIRMA -> {
                    Double inc = db.incomeDao().getTotalBySourceType("FIRMA");
                    Double exp = db.expenseDao().getTotalByCategoryType("FIRMA");
                    totalIncome = (inc != null ? inc : 0.0);
                    totalExpense = (exp != null ? exp : 0.0);
                }
                case PERSONAL -> {
                    Double inc = db.incomeDao().getTotalBySourceType("PERSONAL");
                    Double exp = db.expenseDao().getTotalByCategoryType("PERSONAL");
                    totalIncome = (inc != null ? inc : 0.0);
                    totalExpense = (exp != null ? exp : 0.0);
                }
            }

            final double fIncome = totalIncome;
            final double fExpense = totalExpense;
            final double balance = fIncome - fExpense;

            runOnUiThread(() -> {
                animateText(tvIncome, String.format(Locale.getDefault(),
                        "Venituri totale: %.2f lei", fIncome));
                animateText(tvExpense, String.format(Locale.getDefault(),
                        "Cheltuieli totale: %.2f lei", fExpense));

                tvBalance.setTextColor(getResources().getColor(
                        balance < 0 ? R.color.Red : R.color.Green));

                animateText(tvBalance, String.format(Locale.getDefault(),
                        "Balanță: %.2f lei", balance));
            });
        }).start();
    }

    private void animateText(TextView view, String newText) {
        if (view == null) return;
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(150);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(150);

        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}
            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}
            @Override public void onAnimationEnd(android.view.animation.Animation animation) {
                view.setText(newText);
                view.startAnimation(fadeIn);
            }
        });
        view.startAnimation(fadeOut);
    }

    private void highlightButton(MaterialButton selected) {
        MaterialButton[] all = {btnAll, btnFirma, btnPersonal};
        for (MaterialButton b : all) {
            if (b == null) continue;
            b.setStrokeWidth(1);
            b.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
        if (selected != null) {
            selected.setStrokeWidth(2);
            selected.setBackgroundColor(getResources().getColor(R.color.teal_200));
        }
    }

    // ---------------- NOTIFICĂRI ----------------
    private void createExportChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_EXPORT, "Export status", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void notifyExport(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // ✅ verificare permisiune Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATION_PERMISSION);
                return;
            }
        }

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_EXPORT)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), nb.build());

        runOnUiThread(() ->
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(text)
                        .setPositiveButton("OK", null)
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldRefreshTotals) {
            updateTotals();
            shouldRefreshTotals = false;
        }
    }
}
