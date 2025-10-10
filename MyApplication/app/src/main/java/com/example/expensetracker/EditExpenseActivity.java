package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Ecran de EDIT pentru o cheltuială existentă.
 * - Refolosește layoutul de "add" (activity_add_expense)
 * - Încărcare după expense_id sau expense_uid
 * - UPDATE/DELETE pe thread separat
 * - Nu afectează flow-ul de inserare automată din notificări
 */
public class EditExpenseActivity extends BaseActivity {

    private EditText amountInput, descriptionInput, dateInput, categoryInput;
    private Spinner typeSpinner;

    /** Modelul curent încărcat din DB și editat în UI. */
    private Expense currentExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense); // refolosim layoutul de add
        setupToolbar(R.string.title_expenses, true);

        // ---- Leagă view-urile din layout
        amountInput      = findViewById(R.id.expense_amount);
        descriptionInput = findViewById(R.id.expense_description);
        dateInput        = findViewById(R.id.expense_date);
        categoryInput    = findViewById(R.id.expense_category);
        typeSpinner      = findViewById(R.id.expense_type_spinner);

        // Spinner PERSONAL/FIRMA
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Date picker helper din proiect
        DatePickerUtil.attach(this, dateInput);

        // ---- Identificatori primiți prin Intent (fără redeclarări duble)
        final int expenseId = getIntent().getIntExtra("expense_id", -1);
        final String expenseUid = getIntent().getStringExtra("expense_uid");

        // ---- Încarcă modelul în background
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Expense loaded = null;

            if (expenseId > 0) {
                loaded = db.expenseDao().getById(expenseId);
            } else if (expenseUid != null && !expenseUid.isEmpty()) {
                loaded = db.expenseDao().getByUid(expenseUid);
            }

            final Expense result = loaded;
            runOnUiThread(() -> {
                if (result == null) {
                    Toast.makeText(this, "Înregistrarea nu a fost găsită.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    currentExpense = result;
                    populateFormFromModel(result);
                }
            });
        }).start();

        // ---- BUTON SALVARE (UPDATE)
        Button save = findViewById(R.id.btn_save_expense);
        if (save != null) {
            save.setOnClickListener(v -> onSave());
        }

        // ---- BUTON ȘTERGERE (dacă există în layout)
        Button delete = findViewById(R.id.btn_delete_expense);
        if (delete != null) {
            delete.setOnClickListener(v -> confirmDelete());
        }
    }

    /** Populează UI din modelul încărcat. */
    private void populateFormFromModel(Expense e) {
        amountInput.setText(String.valueOf(e.amount));
        descriptionInput.setText(e.description == null ? "" : e.description);
        dateInput.setText(formatDate(e.date)); // folosim formatter local, nu DatePickerUtil.format(...)
        categoryInput.setText(e.category == null ? "" : e.category);

        String type = e.categoryType == null ? "PERSONAL" : e.categoryType;
        int pos = "FIRMA".equalsIgnoreCase(type) ? 1 : 0;
        typeSpinner.setSelection(pos);
    }

    /** Salvează modificările în DB (UPDATE). */
    private void onSave() {
        if (currentExpense == null) return;

        // Validare minimală sumă
        String amountStr = amountInput.getText().toString().trim().replace(',', '.');
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception ex) {
            Toast.makeText(this, "Sumă invalidă", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = descriptionInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        long dateMillis = DatePickerUtil.parse(dateStr); // există deja în proiect
        String category = categoryInput.getText().toString().trim();
        String type = typeSpinner.getSelectedItem() != null
                ? typeSpinner.getSelectedItem().toString()
                : "PERSONAL";

        // Reflectă în model
        currentExpense.amount = amount;
        currentExpense.description = desc;
        currentExpense.date = dateMillis;
        currentExpense.category = category;
        currentExpense.categoryType = type;

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext())
                    .expenseDao().update(currentExpense);
            runOnUiThread(() -> {
                MainActivity.shouldRefreshTotals = true;
                Toast.makeText(this, "Actualizat", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    /** Confirmă și șterge înregistrarea. */
    private void confirmDelete() {
        if (currentExpense == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_expense)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getApplicationContext())
                                .expenseDao().delete(currentExpense);
                        runOnUiThread(() -> {
                            MainActivity.shouldRefreshTotals = true;
                            Toast.makeText(this, "Șters", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------- Helpers ----------------

    /** Format local pt. date (ca să nu depindem de DatePickerUtil.format(...)) */
    private String formatDate(long millis) {
        if (millis <= 0) return DatePickerUtil.today(); // fallback
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
