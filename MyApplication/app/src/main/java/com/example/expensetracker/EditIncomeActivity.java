package com.example.expensetracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class EditIncomeActivity extends BaseActivity {

    private EditText amountInput, descriptionInput, dateInput, sourceInput;
    private Spinner typeSpinner;

    private Income currentIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_income);
        setupToolbar(R.string.title_incomes, true);

        // view binding
        amountInput      = findViewById(R.id.income_amount);
        descriptionInput = findViewById(R.id.income_description);
        dateInput        = findViewById(R.id.income_date);
        sourceInput      = findViewById(R.id.income_source);
        typeSpinner      = findViewById(R.id.income_type_spinner);

        // spinner PERSONAL/FIRMA
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        DatePickerUtil.attach(this, dateInput);

        // extras
        int id = getIntent().getIntExtra("income_id", -1);
        String uid = getIntent().getStringExtra("income_uid");

        // încarcă modelul
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Income loaded = null;
            if (id > 0) {
                loaded = db.incomeDao().getById(id);
            } else if (uid != null && !uid.isEmpty()) {
                loaded = db.incomeDao().getByUid(uid);
            }
            Income finalLoaded = loaded;
            runOnUiThread(() -> {
                if (finalLoaded == null) {
                    finish();
                } else {
                    currentIncome = finalLoaded;
                    populateFormFromModel();
                }
            });
        }).start();

        Button save = findViewById(R.id.btn_save_income);
        if (save != null) save.setOnClickListener(v -> onSave());

        Button delete = findViewById(R.id.btn_delete_income);
        if (delete != null) delete.setOnClickListener(v -> confirmDelete());
    }

    private void populateFormFromModel() {
        amountInput.setText(String.valueOf(currentIncome.amount));
        descriptionInput.setText(currentIncome.description == null ? "" : currentIncome.description);
        dateInput.setText(DatePickerUtil.format(currentIncome.date));
        sourceInput.setText(currentIncome.sourceType == null ? "" : currentIncome.sourceType);

        String type = currentIncome.sourceType == null ? "PERSONAL" : currentIncome.sourceType;
        int pos = "FIRMA".equalsIgnoreCase(type) ? 1 : 0;
        typeSpinner.setSelection(pos);
    }

    private void onSave() {
        if (currentIncome == null) return;

        String amountStr = amountInput.getText().toString().trim().replace(',', '.');
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = descriptionInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        long dateMillis = DatePickerUtil.parse(dateStr);
        String source = sourceInput.getText().toString().trim();
        String type = typeSpinner.getSelectedItem() == null ? "PERSONAL"
                : typeSpinner.getSelectedItem().toString();

        currentIncome.amount = amount;
        currentIncome.description = desc;
        currentIncome.date = dateMillis;
        // la tine "tipul" sursei e stocat în sourceType
        currentIncome.sourceType = (source.isEmpty() ? type : source);

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext())
                    .incomeDao().update(currentIncome);
            runOnUiThread(() -> {
                MainActivity.shouldRefreshTotals = true;
                Toast.makeText(this, R.string.updated, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

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
                            Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
