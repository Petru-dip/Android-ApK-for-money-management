package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;

// NU mai importa AppCompatActivity aici; clasa extinde BaseActivity
// import androidx.appcompat.app.AppCompatActivity;

public class IncomeActivity extends BaseActivity {


    protected void setupToolbar(@StringRes int titleRes, boolean showBack) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_include);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(titleRes);
                ab.setDisplayHomeAsUpEnabled(showBack);
            }

            if (showBack) {
                toolbar.setNavigationOnClickListener(v -> finish());
            }
        }
    }
    private EditText amountInput, descriptionInput, dateInput;
    private MaterialAutoCompleteTextView sourceTypeSpinner, typeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) întâi încărcăm layout-ul
        setContentView(R.layout.activity_income);

        // 2) apoi configurăm toolbar-ul comun (titlu & back)
        setupToolbar(R.string.title_incomes, true);

        amountInput = findViewById(R.id.income_amount);
        descriptionInput = findViewById(R.id.income_description);
        dateInput = findViewById(R.id.income_date);
        sourceTypeSpinner = findViewById(R.id.income_source);
        typeSpinner = findViewById(R.id.income_source_type);

        // dropdown categorii venit (Salariu/Bonus/Familie/Primit)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.income_categories, android.R.layout.simple_list_item_1
        );
        sourceTypeSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_list_item_1
        );
        typeSpinner.setAdapter(typeAdapter);

        dateInput.setText(DatePickerUtil.today());
        DatePickerUtil.attach(this, dateInput);

        Button saveBtn = findViewById(R.id.btn_save_income);
        saveBtn.setOnClickListener(v -> onSave());
    }

    private void onSave() {
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Introdu o sumă", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(',', '.'));
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Sumă invalidă", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = descriptionInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        long dateMillis = DatePickerUtil.parse(dateStr);

        String sourceType = sourceTypeSpinner.getText() == null
                ? "PERSONAL"
                : sourceTypeSpinner.getText().toString(); // "PERSONAL"/"FIRMA"

        Income income = new Income();
        income.amount = amount;
        income.description = desc;
        income.date = dateMillis;
        income.sourceType = sourceType;
        income.uid = java.util.UUID.randomUUID().toString();

        MainActivity.shouldRefreshTotals = true; // pentru actualizarea totalurilor în UI

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).incomeDao().insert(income);
            runOnUiThread(() -> {
                Toast.makeText(this, "Venit salvat", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
