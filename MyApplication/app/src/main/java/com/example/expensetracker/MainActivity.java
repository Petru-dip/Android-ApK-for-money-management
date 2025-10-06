package com.example.expensetracker;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;



public class MainActivity extends AppCompatActivity {

//    private final ActivityResultLauncher<String> exportLauncher =
//            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
//                if (uri != null) doExport(uri);
//            });
//
//    private final ActivityResultLauncher<String[]> importLauncher =
//            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
//                if (uri != null) doImport(uri);
//            });
//

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnExport = findViewById(R.id.btn_export_excel);
        Button btnImport = findViewById(R.id.btn_import_excel);
        btnExport.setOnClickListener(v ->
                exportExcelLauncher.launch("finance-export-" + new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm", java.util.Locale.ROOT).format(new java.util.Date()) + ".xlsx"));

        btnImport.setOnClickListener(v ->
                importExcelLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel" // fallback
                }));
        Button addExpense = findViewById(R.id.btn_add_expense);
        Button addIncome = findViewById(R.id.btn_add_income);
        Button listExpenses = findViewById(R.id.btn_list_expenses);
        Button reports = findViewById(R.id.btn_reports);
        Button listIncomes = findViewById(R.id.btn_list_incomes);
        addExpense.setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));
        addIncome.setOnClickListener(v -> startActivity(new Intent(this, IncomeActivity.class)));
        listExpenses.setOnClickListener(v -> startActivity(new Intent(this, ExpenseListActivity.class)));
        reports.setOnClickListener(v -> startActivity(new Intent(this, ExpenseReportActivity.class)));
        if (listIncomes != null) {
            listIncomes.setOnClickListener(v -> startActivity(new Intent(this, IncomeListActivity.class)));
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<String> exportExcelLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    uri -> {
                        if (uri == null) return;
                        new Thread(() -> {
                            try {
                                ExportImportUtils.exportToExcel(this, AppDatabase.getInstance(this), uri);
                                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Export Excel")
                                        .setMessage("Fișier XLSX salvat cu succes.")
                                        .setPositiveButton("OK", null).show());
                            } catch (Exception e) {
                                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Eroare la export")
                                        .setMessage(e.getMessage())
                                        .setPositiveButton("OK", null).show());
                            }
                        }).start();
                    });

    private final androidx.activity.result.ActivityResultLauncher<String[]> importExcelLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) return;
                        new Thread(() -> {
                            try {
                                ExportImportUtils.ExcelImportResult res =
                                        ExportImportUtils.importFromExcel(this, AppDatabase.getInstance(this), uri);
                                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Import Excel")
                                        .setMessage("Import reușit.\n" + res.toString())
                                        .setPositiveButton("OK", null).show());
                            } catch (Exception e) {
                                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Eroare la import")
                                        .setMessage(e.getMessage())
                                        .setPositiveButton("OK", null).show());
                            }
                        }).start();
                    });
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            String[] opts = {"Luminoasă", "Întunecată", "Urmărește sistemul"};
            new AlertDialog.Builder(this)
                    .setTitle("Alege tema")
                    .setItems(opts, (d, which) -> {
                        switch (which) {
                            case 0: ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_NO); break;
                            case 1: ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_YES); break;
                            case 2: ThemeUtils.setTheme(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
                        }
                    }).show();
            return true;
        }
//        else if (item.getItemId() == R.id.action_export) {
//            String name = "finance-export-" +
//                    new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.getDefault())
//                            .format(new java.util.Date()) + ".json";
//            exportLauncher.launch(name);
//            return true;
//        } else if (item.getItemId() == R.id.action_import) {
//            importLauncher.launch(new String[]{"application/json"});
//            return true;
//        }


        return super.onOptionsItemSelected(item);
    }
//
//    private void doExport(Uri uri) {
//        new Thread(() -> {
//            try {
//                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
//                ExportImportUtils.exportJson(this, uri, db);
//                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                        .setMessage("Export reușit.").setPositiveButton("OK", null).show());
//            } catch (Exception e) {
//                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                        .setTitle("Eroare la export").setMessage(e.getMessage()).setPositiveButton("OK", null).show());
//            }
//        }).start();
//    }

//    private void doImport(Uri uri) {
//        new Thread(() -> {
//            try {
//                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
//                BackupModels.ExportContainer c = ExportImportUtils.readJson(this, uri);
//
//                int dups = 0;
//                if (c != null && c.expenses != null) {
//                    for (Expense e : c.expenses) if (e.uid != null && !e.uid.isEmpty() && db.expenseDao().getByUid(e.uid) != null) dups++;
//                }
//                if (c != null && c.incomes != null) {
//                    for (Income i : c.incomes) if (i.uid != null && !i.uid.isEmpty() && db.incomeDao().getByUid(i.uid) != null) dups++;
//                }
//                final int duplicates = dups;
//
//                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                        .setTitle("Import date")
//                        .setMessage(duplicates > 0
//                                ? ("Există " + duplicates + " înregistrări deja existente.\nRescrii dublurile sau imporți doar cele noi?")
//                                : "Nu s-au detectat dubluri. Continui importul?")
//                        .setPositiveButton(duplicates > 0 ? "Rescrie dublurile" : "Importă", (d, w) -> {
//                            new Thread(() -> {
//                                ExportImportUtils.importJson(db, c, duplicates > 0);
//                                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                                        .setMessage("Import finalizat.").setPositiveButton("OK", null).show());
//                            }).start();
//                        })
//                        .setNegativeButton(duplicates > 0 ? "Importă doar noi" : "Anulează", (d, w) -> {
//                            if (duplicates > 0) {
//                                new Thread(() -> {
//                                    ExportImportUtils.importJson(db, c, false);
//                                    runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                                            .setMessage("Import finalizat (doar înregistrări noi).").setPositiveButton("OK", null).show());
//                                }).start();
//                            }
//                        })
//                        .show());
//            } catch (Exception e) {
//                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
//                        .setTitle("Eroare la import").setMessage(e.getMessage()).setPositiveButton("OK", null).show());
//            }
//        }).start();
//    }


}


