package com.example.expensetracker;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Locale;

public class AddIncomeActivity extends AppCompatActivity {

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

    private static final String TAG = "AddIncomeExtras";

    private EditText amountInput, descriptionInput, dateInput;
    private MaterialAutoCompleteTextView categoryInput;
    private MaterialAutoCompleteTextView typeSpinner;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Întâi încărcăm layoutul
        setContentView(R.layout.activity_add_income);

        // 2) Apoi setăm toolbar-ul (altfel risti NPE în setupToolbar)
        setupToolbar(R.string.title_add_incomes, true);

        amountInput = findViewById(R.id.income_amount);
        descriptionInput = findViewById(R.id.income_description);
        dateInput = findViewById(R.id.income_date);
        categoryInput = findViewById(R.id.income_category);
        typeSpinner = findViewById(R.id.income_type_spinner);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_list_item_1
        );
        typeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                this, R.array.income_categories, android.R.layout.simple_list_item_1
        );
        categoryInput.setAdapter(catAdapter);


        dateInput.setText(DatePickerUtil.today());
        DatePickerUtil.attach(this, dateInput);

        // ---- Pre-fill din intent (notificare/ADB) ----
        double amount = getFlexibleAmount();
        String merchant = getFlexibleString(
                PaymentNotificationService.EXTRA_MERCHANT, "extra_merchant");
        String currency = getFlexibleString(
                PaymentNotificationService.EXTRA_CURRENCY, "extra_currency");
        String category = getFlexibleString(
                PaymentNotificationService.EXTRA_CATEGORY, "extra_category");

        Bundle b = getIntent().getExtras();
        if (b != null) {
            for (String k : b.keySet()) {
                Object v = b.get(k);
                Log.d(TAG, k + " = " + v + " (" + (v != null ? v.getClass().getSimpleName() : "null") + ")");
            }
        }

        if (!Double.isNaN(amount) && amount > 0) {
            if (Math.abs(amount - Math.round(amount)) < 0.0001) {
                amountInput.setText(String.valueOf((long) Math.round(amount)));
            } else {
                amountInput.setText(String.valueOf(amount));
            }
        }

        if (merchant != null && !merchant.isEmpty()) {
            descriptionInput.setText(merchant);
            if ((category == null || category.isEmpty()) && looksLikeGroceries(merchant)) {
                category = "Cumpărături";
            }
        }

        if (category != null && !category.isEmpty()) {
            categoryInput.setText(category);
        }
        // currency: momentan nefolosit în UI

        Button save = findViewById(R.id.btn_save_income);
        if (save != null) {
            save.setOnClickListener(v -> onSaveStrict());
        }

        // Butonul "auto save" a fost eliminat din layout pentru simplitate vizuală

    }

    /** Varianta strictă (comportamentul de până acum): cere sumă validă. */
    private void onSaveStrict() {
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Introdu o sumă", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(',', '.'));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Sumă invalidă", Toast.LENGTH_SHORT).show();
            return;
        }
        saveIncome(amount);
    }

    /** Varianta nouă: dacă nu avem sumă, folosește 01.01 (1.01). */
    private void onAutoSave() {
        double amount;
        String amountStr = amountInput.getText().toString().trim();

        if (amountStr.isEmpty()) {
            amount = 1.01d;
            amountInput.setText("1.01");
        } else {
            try {
                amount = Double.parseDouble(amountStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                amount = 1.01d;
                amountInput.setText("1.01");
            }
        }

        String catNow = categoryInput.getText().toString().trim();
        String descNow = descriptionInput.getText().toString().trim();
        if (catNow.isEmpty() && looksLikeGroceries(descNow)) {
            categoryInput.setText("Cumpărături");
        }
        if (descNow.isEmpty()) {
            descriptionInput.setText("Necunoscut");
        }

        saveIncome(amount);
    }

    private void saveIncome(double amount) {
        String desc = descriptionInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        long dateMillis = DatePickerUtil.parse(dateStr);
        if (dateMillis == 0L) {
            dateMillis = System.currentTimeMillis(); // fallback sigur
        }
        String category = categoryInput.getText() == null ? "" : categoryInput.getText().toString().trim();
        String type = typeSpinner.getText() != null && !typeSpinner.getText().toString().isEmpty()
                ? typeSpinner.getText().toString()
                : "PERSONAL"; // PERSONAL / FIRMA

        Income ex = new Income();
        ex.amount = amount;
        ex.description = desc;
        ex.date = dateMillis;
        ex.category = category;
        ex.categoryType = type;
        ex.uid = java.util.UUID.randomUUID().toString();

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).incomeDao().insert(ex);
            runOnUiThread(() -> {
                MainActivity.shouldRefreshTotals = true;
                Toast.makeText(this, "Cheltuială salvată", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    // --- Helpers ---

    /** Citește suma din EXTRA_AMOUNT ca double, iar dacă nu există, încearcă ca string (inclusiv fallback 'extra_amount' pentru ADB). */
    private double getFlexibleAmount() {
        double amount = getIntent().getDoubleExtra(PaymentNotificationService.EXTRA_AMOUNT, Double.NaN);
        if (!Double.isNaN(amount)) return amount;

        String amountStr = getFlexibleString(PaymentNotificationService.EXTRA_AMOUNT, "extra_amount");
        if (amountStr != null) {
            try {
                return Double.parseDouble(amountStr.replace(',', '.'));
            } catch (Exception ignored) {}
        }
        return Double.NaN;
    }

    /** Returnează primul extra string non-gol dintre cheia oficială și fallback-ul pentru ADB. */
    private String getFlexibleString(String officialKey, String adbFallbackKey) {
        String a = getIntent().getStringExtra(officialKey);
        if (a != null && !a.isEmpty()) return a;
        String b = getIntent().getStringExtra(adbFallbackKey);
        if (b != null && !b.isEmpty()) return b;
        return null;
    }

    /** Heuristic mic pentru magazine alimentare. Extinde după nevoie. */
    private boolean looksLikeGroceries(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("mega image") || t.contains("mega")
                || t.contains("kaufland")  || t.contains("lidl")
                || t.contains("carrefour") || t.contains("auchan")
                || t.contains("profi")     || t.contains("penny");
    }

    // metodă existentă de setup toolbar:

}
