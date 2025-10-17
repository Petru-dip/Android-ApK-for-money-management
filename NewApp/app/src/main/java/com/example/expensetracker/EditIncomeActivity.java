//package com.example.expensetracker;
//
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.ArrayAdapter;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AlertDialog;
//
//import com.google.android.material.textfield.MaterialAutoCompleteTextView;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//
//public class EditIncomeActivity extends BaseActivity {
//
//
//
//    private EditText amountInput, descriptionInput, dateInput;
//    private MaterialAutoCompleteTextView categoryInput;
//    private Spinner typeSpinner;
//    private Income currentIncome;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        ThemeUtils.applySavedTheme(this);
//        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_edit_income);
//        setupToolbar(R.string.title_incomes, true);
//
//        // view binding
//        amountInput      = findViewById(R.id.income_amount);
//        descriptionInput = findViewById(R.id.income_description);
//        dateInput        = findViewById(R.id.income_date);
//        categoryInput      = findViewById(R.id.income_category);
//        typeSpinner      = findViewById(R.id.income_type_spinner);
//
//        // spinner PERSONAL/FIRMA
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
//                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        typeSpinner.setAdapter(adapter);
//
//        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
//                this, R.array.expense_categories, android.R.layout.simple_list_item_1);
//        categoryInput.setAdapter(catAdapter);
//
//
//
//        DatePickerUtil.attach(this, dateInput);
//
//
//
//
//
//        // extras
//        int id = getIntent().getIntExtra("income_id", -1);
//        String uid = getIntent().getStringExtra("income_uid");
//
//        // încarcă modelul
//        new Thread(() -> {
//            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
//            Income loaded = null;
//            if (id > 0) {
//                loaded = db.incomeDao().getById(id);
//            } else if (uid != null && !uid.isEmpty()) {
//                loaded = db.incomeDao().getByUid(uid);
//            }
//            final Income result = loaded;
//            runOnUiThread(() -> {
//                if (result == null) {
//                    Toast.makeText(this, "Înregistrarea nu a fost găsită.", Toast.LENGTH_SHORT).show();
//                    finish();
//                } else {
//                    currentIncome = result;
//                    populateFormFromModel(result);
//                }
//            });
//        }).start();
//
//        Button save = findViewById(R.id.btn_save_income);
//        if (save != null) save.setOnClickListener(v -> onSave());
//
//        Button delete = findViewById(R.id.btn_delete_income);
//        if (delete != null) delete.setOnClickListener(v -> confirmDelete());
//    }
//
//    /** Populează UI din modelul încărcat. */
//    private void populateFormFromModel(Income e) {
//        amountInput.setText(String.valueOf(e.amount));
//        descriptionInput.setText(e.description == null ? "" : e.description);
//        dateInput.setText(formatDate(e.date));
//        categoryInput.setText(e.category == null ? "" : e.category, false);
//
//        String type = e.categoryType == null ? "PERSONAL" : e.categoryType;
//        int pos = "FIRMA".equalsIgnoreCase(type) ? 1 : 0;
//        typeSpinner.setSelection(pos);
//    }
//
//    private void onSave() {
//        if (currentIncome == null) return;
//
//        String amountStr = amountInput.getText().toString().trim().replace(',', '.');
//        double amount;
//        try {
//            amount = Double.parseDouble(amountStr);
//        } catch (Exception e) {
//            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String desc = descriptionInput.getText().toString().trim();
//        String dateStr = dateInput.getText().toString().trim();
//        long dateMillis = DatePickerUtil.parse(dateStr);
//        String category = categoryInput.getText() == null ? "" : categoryInput.getText().toString().trim();
//        String type = typeSpinner.getSelectedItem() != null
//                ? typeSpinner.getSelectedItem().toString()
//                : "PERSONAL";
//
//        currentIncome.amount = amount;
//        currentIncome.description = desc;
//        currentIncome.date = dateMillis;
//        currentIncome.category = category;
//        currentIncome.categoryType = type;
//        // la tine "tipul" sursei e stocat în sourceType
////        currentIncome.sourceType = (source.isEmpty() ? type : source);
//
//        new Thread(() -> {
//            AppDatabase.getInstance(getApplicationContext())
//                    .incomeDao().update(currentIncome);
//            runOnUiThread(() -> {
//                MainActivity.shouldRefreshTotals = true;
//                Toast.makeText(this, R.string.updated, Toast.LENGTH_SHORT).show();
//                finish();
//            });
//        }).start();
//    }
//
//    private void confirmDelete() {
//        if (currentIncome == null) return;
//        new AlertDialog.Builder(this)
//                .setTitle(R.string.delete)
//                .setMessage(R.string.confirm_delete_income)
//                .setPositiveButton(R.string.delete, (d, w) -> {
//                    new Thread(() -> {
//                        AppDatabase.getInstance(getApplicationContext())
//                                .incomeDao().delete(currentIncome);
//                        runOnUiThread(() -> {
//                            MainActivity.shouldRefreshTotals = true;
//                            Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
//                            finish();
//                        });
//                    }).start();
//                })
//                .setNegativeButton(R.string.cancel, null)
//                .show();
//    }
//    private String formatDate(long millis) {
//        if (millis <= 0) return DatePickerUtil.today(); // fallback
//        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
//        return sdf.format(new Date(millis));
//    }
//}
