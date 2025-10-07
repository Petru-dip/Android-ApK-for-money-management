package com.example.expensetracker;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class AddExpenseActivity extends AppCompatActivity {
    private EditText amountInput, descriptionInput, dateInput, categoryInput;
    private Spinner typeSpinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);
        amountInput = findViewById(R.id.expense_amount);
        descriptionInput = findViewById(R.id.expense_description);
        dateInput = findViewById(R.id.expense_date);
        categoryInput = findViewById(R.id.expense_category);
        typeSpinner = findViewById(R.id.expense_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        dateInput.setText(DatePickerUtil.today());
        DatePickerUtil.attach(this, dateInput);
        Button save = findViewById(R.id.btn_save_expense);
        save.setOnClickListener(v -> onSave());
    }
    private void onSave() {
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) { Toast.makeText(this, "Introdu o sumă", Toast.LENGTH_SHORT).show(); return; }
        double amount = Double.parseDouble(amountStr);
        String desc = descriptionInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        long dateMillis = DatePickerUtil.parse(dateStr);
        String category = categoryInput.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString(); // PERSONAL / FIRMA
        Expense ex = new Expense();
        ex.amount = amount;
        ex.description = desc;
        ex.date = dateMillis;
        ex.category = category;
        ex.categoryType = type;
        MainActivity.shouldRefreshTotals = true;
        ex.uid = java.util.UUID.randomUUID().toString();
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).expenseDao().insert(ex);
            runOnUiThread(() -> { Toast.makeText(this, "Cheltuială salvată", Toast.LENGTH_SHORT).show(); finish(); });
        }).start();
    }
}
