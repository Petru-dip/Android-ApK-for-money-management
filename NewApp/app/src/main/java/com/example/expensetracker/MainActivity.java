package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private static final String CHANNEL_EXPORT = "export_status";
    private static final int REQ_NOTIFICATION_PERMISSION = 100;

    public static boolean shouldRefreshTotals = false;

    private TextView tvIncome, tvExpense, tvBalance;
    private AutoCompleteTextView dropdownPeriod;
    private long customDateStart = -1, customDateEnd = -1;

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
                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Import anulat")
                                                            .setMessage("Nu au fost făcute modificări.")
                                                            .setPositiveButton("OK", null)
                                                            .show();
                                                })
                                                .show();
                                    } else {
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

        handleExpensePrefillIntent(getIntent());

        createExportChannel();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this::onDrawerItemClick);
        }

        MaterialButton btnExpense = findViewById(R.id.btn_add_expense);
        MaterialButton btnIncome = findViewById(R.id.btn_add_income);
        if (btnExpense != null) {
            btnExpense.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                startActivity(new Intent(this, AddExpenseActivity.class));
            });
        }
        if (btnIncome != null) {
            btnIncome.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                startActivity(new Intent(this, AddIncomeActivity.class));
            });
        }

        tvIncome = findViewById(R.id.tv_total_income);
        tvExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);

        btnAll = findViewById(R.id.btn_total_all);
        btnFirma = findViewById(R.id.btn_total_firma);
        btnPersonal = findViewById(R.id.btn_total_personal);

        if (btnAll != null) btnAll.setOnClickListener(v -> { currentFilter = FilterType.ALL;     highlightButton(btnAll);     updateTotals(); });
        if (btnFirma != null) btnFirma.setOnClickListener(v -> { currentFilter = FilterType.FIRMA;   highlightButton(btnFirma);   updateTotals(); });
        if (btnPersonal != null) btnPersonal.setOnClickListener(v -> { currentFilter = FilterType.PERSONAL; highlightButton(btnPersonal); updateTotals(); });

        setupPeriodFilter();

        highlightButton(btnAll);
        updateTotals();
        AutoBackup.scheduleDaily(this);
    }

    private void setupPeriodFilter() {
        dropdownPeriod = findViewById(R.id.dropdown_period_main);
        String[] periods = getResources().getStringArray(R.array.periods_main);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, periods);
        dropdownPeriod.setAdapter(adapter);
        dropdownPeriod.setText(periods[4], false); // "De la început" by default

        dropdownPeriod.setOnItemClickListener((parent, view, position, id) -> {
            if (periods[position].equals("Perioadă custom")) {
                showDateRangePicker();
            } else {
                customDateStart =-1;
                customDateEnd = -1;
                updateTotals();
            }
        });
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Selectează un interval de date");
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            customDateStart = selection.first;
            customDateEnd = selection.second;
            updateTotals();
        });
        picker.show(getSupportFragmentManager(), picker.toString());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleExpensePrefillIntent(intent);
    }

    private void handleExpensePrefillIntent(Intent intent) {
        if (intent == null) return;
        if (!PaymentNotificationService.ACTION_EXPENSE_FROM_NOTIFICATION.equals(intent.getAction()))
            return;

        Intent add = new Intent(this, AddExpenseActivity.class);
        add.setAction(intent.getAction());
        if (intent.getExtras() != null) {
            add.putExtras(intent.getExtras());
        }
        startActivity(add);
    }

    private boolean onDrawerItemClick(@NonNull MenuItem item) {
        if (drawerLayout != null) drawerLayout.closeDrawers();
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
//            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return false;
    }

    private void updateTotals() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long[] dateRange = getDateRange(dropdownPeriod.getText().toString());

            double totalIncome = 0.0;
            double totalExpense = 0.0;

            switch (currentFilter) {
                case ALL -> {
                    Double inc = db.incomeDao().getTotalAmountByDate(dateRange[0], dateRange[1]);
                    Double exp = db.expenseDao().getTotalAmountByDate(dateRange[0], dateRange[1]);
                    totalIncome  = (inc != null ? inc : 0.0);
                    totalExpense = (exp != null ? exp : 0.0);
                }
                case FIRMA -> {
                    Double inc = db.incomeDao().getTotalByTypeAndDate("FIRMA", dateRange[0], dateRange[1]);
                    Double exp = db.expenseDao().getTotalByTypeAndDate("FIRMA", dateRange[0], dateRange[1]);
                    totalIncome  = (inc != null ? inc : 0.0);
                    totalExpense = (exp != null ? exp : 0.0);
                }
                case PERSONAL -> {
                    Double inc = db.incomeDao().getTotalByTypeAndDate("PERSONAL", dateRange[0], dateRange[1]);
                    Double exp = db.expenseDao().getTotalByTypeAndDate("PERSONAL", dateRange[0], dateRange[1]);
                    totalIncome  = (inc != null ? inc : 0.0);
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

    private long[] getDateRange(String period) {
        if (period.equals("Perioadă custom") && customDateStart > 0 && customDateEnd > 0) {
            return new long[]{customDateStart, customDateEnd};
        }

        Calendar to = Calendar.getInstance();
        Calendar from = Calendar.getInstance();

        switch (period) {
            case "Azi":
                from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
                break;
            case "Anul curent":
                from.set(Calendar.DAY_OF_YEAR, 1);
                from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
                break;
            case "Luna Curenta":
                from.set(Calendar.MONTH, 2);
                from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
                break;
            case "De la început":
            default:
                return new long[]{0, Long.MAX_VALUE};
        }

        to.set(Calendar.HOUR_OF_DAY, 23); to.set(Calendar.MINUTE, 59); to.set(Calendar.SECOND, 59);

        return new long[]{from.getTimeInMillis(), to.getTimeInMillis()};
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        boolean on = Settings.isAutoSaveOn(this);
        MenuItem item = menu.findItem(R.id.action_auto_save);
        if (item != null) {
            item.setTitle(on ? R.string.menu_auto_save_on : R.string.menu_auto_save_off);
            item.setIcon(on ? R.drawable.ic_income : R.drawable.ic_expense);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_auto_save) {
            boolean on = Settings.toggleAutoSave(this);
            item.setTitle(on ? R.string.menu_auto_save_on : R.string.menu_auto_save_off);
            item.setIcon(on ? R.drawable.ic_income : R.drawable.ic_expense);
            android.widget.Toast.makeText(
                    this,
                    on ? getString(R.string.toast_auto_save_on) : getString(R.string.toast_auto_save_off),
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
