package com.example.expensetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private static final String CHANNEL_EXPORT = "export_status";

    // 🔄 Flag global pentru actualizarea totalurilor
    public static boolean shouldRefreshTotals = false;

    private TextView tvIncome, tvExpense, tvBalance;

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
                                ExportImportUtils.ExcelImportResult res =
                                        ExportImportUtils.importFromExcel(this, AppDatabase.getInstance(this), uri);
                                runOnUiThread(() -> {
                                    new AlertDialog.Builder(this)
                                            .setTitle("Import Excel")
                                            .setMessage("Import reușit.\n" + res.toString())
                                            .setPositiveButton("OK", null)
                                            .show();

                                    shouldRefreshTotals = true;
                                    updateTotals();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> new AlertDialog.Builder(this)
                                        .setTitle("Eroare la import")
                                        .setMessage(e.getMessage())
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
        Button btnExpense = findViewById(R.id.btn_add_expense);
        Button btnIncome = findViewById(R.id.btn_add_income);
        btnExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, AddExpenseActivity.class));
        });
        btnIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, IncomeActivity.class));
        });

        // Inițializează TextView-urile pentru totaluri
        tvIncome = findViewById(R.id.tv_total_income);
        tvExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);

        // Actualizează totalurile la pornirea aplicației
        updateTotals();

        // Auto-backup zilnic
        AutoBackup.scheduleDaily(this);
    }

    // ------------------------ MENIUL DE SUS ------------------------
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
                            case 0:
                                ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_NO);
                                break;
                            case 1:
                                ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_YES);
                                break;
                            case 2:
                                ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                break;
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

    // ------------------------ MENIUL LATERAL ------------------------
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
                    new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm",
                            java.util.Locale.ROOT).format(new java.util.Date()) + ".xlsx");
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

    // ------------------------ TOTALURI ------------------------
    private void updateTotals() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            double totalIncome = db.incomeDao().getTotalAmount();
            double totalExpense = db.expenseDao().getTotalAmount();
            double balance = totalIncome - totalExpense;

            runOnUiThread(() -> {
                animateText(tvIncome, String.format(Locale.getDefault(),
                        "Venituri totale: %.2f lei", totalIncome));
                animateText(tvExpense, String.format(Locale.getDefault(),
                        "Cheltuieli totale: %.2f lei", totalExpense));

                if (balance < 0)
                    tvBalance.setTextColor(getResources().getColor(R.color.Red));
                else
                    tvBalance.setTextColor(getResources().getColor(R.color.Green));

                animateText(tvBalance, String.format(Locale.getDefault(),
                        "Balanță: %.2f lei", balance));
            });
        }).start();
    }

    // 🔥 Animație fade-in pentru actualizare smooth
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

    // ------------------------ NOTIFICĂRI ------------------------
    private void createExportChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_EXPORT, "Export status", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void notifyExport(String title, String text) {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_EXPORT)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
