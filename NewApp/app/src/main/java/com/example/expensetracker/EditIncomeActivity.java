package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Ecran de EDIT pentru o cheltuială existentă.
 * - Refolosește layoutul de "add" (activity_add_income)
 * - Încărcare după income_id sau income_uid
 * - UPDATE/DELETE pe thread separat
 * - Nu afectează flow-ul de inserare automată din notificări
 */
public class EditIncomeActivity extends BaseActivity {

    private EditText amountInput, descriptionInput, dateInput;
    private MaterialAutoCompleteTextView categoryInput;
    private MaterialAutoCompleteTextView typeSpinner;

    /** Modelul curent încărcat din DB și editat în UI. */
    private Income currentIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income); // refolosim layoutul de add
//        setupToolbar(R.string.title_incomes, true);

        // ---- Leagă view-urile din layout
        amountInput      = findViewById(R.id.income_amount);
        descriptionInput = findViewById(R.id.income_description);
        dateInput        = findViewById(R.id.income_date);
        categoryInput    = findViewById(R.id.income_category);
        typeSpinner      = findViewById(R.id.income_type_spinner);

        // Dropdown PERSONAL/FIRMA
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_list_item_1);
        typeSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                this, R.array.income_categories, android.R.layout.simple_list_item_1);
        categoryInput.setAdapter(catAdapter);

        // Date picker helper din proiect
        DatePickerUtil.attach(this, dateInput);

        // ---- Identificatori primiți prin Intent (fără redeclarări duble)
        final int incomeId = getIntent().getIntExtra("income_id", -1);
        final String incomeUid = getIntent().getStringExtra("income_uid");

        // ---- Încarcă modelul în background
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Income loaded = null;

            if (incomeId > 0) {
                loaded = db.incomeDao().getById(incomeId);
            } else if (incomeUid != null && !incomeUid.isEmpty()) {
                loaded = db.incomeDao().getByUid(incomeUid);
            }

            final Income result = loaded;
            runOnUiThread(() -> {
                if (result == null) {
                    Toast.makeText(this, "Înregistrarea nu a fost găsită.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    currentIncome = result;
                    populateFormFromModel(result);
                }
            });
        }).start();

        // ---- BUTON SALVARE (UPDATE)
        Button save = findViewById(R.id.btn_save_income);
        if (save != null) {
            save.setOnClickListener(v -> onSave());
        }

        // ---- BUTON ȘTERGERE (dacă există în layout)
        Button delete = findViewById(R.id.btn_delete_income);
        if (delete != null) {
            delete.setOnClickListener(v -> confirmDelete());
        }
    }

    /** Populează UI din modelul încărcat. */
    private void populateFormFromModel(Income e) {
        amountInput.setText(String.valueOf(e.amount));
        descriptionInput.setText(e.description == null ? "" : e.description);
        dateInput.setText(formatDate(e.date)); // folosim formatter local, nu DatePickerUtil.format(...)
        categoryInput.setText(e.category == null ? "" : e.category);

        String type = e.categoryType == null ? "PERSONAL" : e.categoryType;
        int pos = "FIRMA".equalsIgnoreCase(type) ? 1 : 0;
        String typedText = pos == 1 ? "FIRMA" : "PERSONAL";
        typeSpinner.setText(typedText, false);
    }

    /** Salvează modificările în DB (UPDATE). */
    private void onSave() {
        if (currentIncome == null) return;

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
        String category = categoryInput.getText() == null ? "" : categoryInput.getText().toString().trim();
        String type = typeSpinner.getText() != null && !typeSpinner.getText().toString().isEmpty()
                ? typeSpinner.getText().toString()
                : "PERSONAL";

        // Reflectă în model
        currentIncome.amount = amount;
        currentIncome.description = desc;
        currentIncome.date = dateMillis;
        currentIncome.category = category;
        currentIncome.categoryType = type;

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext())
                    .incomeDao().update(currentIncome);
            runOnUiThread(() -> {
                MainActivity.shouldRefreshTotals = true;
                Toast.makeText(this, "Actualizat", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    /** Confirmă și șterge înregistrarea. */
    private void confirmDelete() {
        if (currentIncome == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_income)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getApplicationContext())
                                .incomeDao().delete(currentIncome);
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
