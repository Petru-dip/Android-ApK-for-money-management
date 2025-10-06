package com.example.expensetracker;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class EditExpenseActivity extends AppCompatActivity {
    private EditText amountInput, descriptionInput, dateInput, categoryInput;
    private Spinner typeSpinner;
    private Expense current;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.expensetracker.R.layout.activity_edit_expense);
        amountInput = findViewById(R.id.expense_amount);
        descriptionInput = findViewById(R.id.expense_description);
        dateInput = findViewById(R.id.expense_date);
        categoryInput = findViewById(R.id.expense_category);
        typeSpinner = findViewById(R.id.expense_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        DatePickerUtil.attach(this, dateInput);
        int id = getIntent().getIntExtra("id", -1);
        if (id == -1) { finish(); return; }
        new Thread(() -> {
            current = AppDatabase.getInstance(getApplicationContext()).expenseDao().getById(id);
            runOnUiThread(() -> bind(current));
        }).start();
        Button save = findViewById(R.id.btn_save_expense);
        Button delete = findViewById(R.id.btn_delete_expense);
        save.setOnClickListener(v -> onSave());
        delete.setOnClickListener(v -> onDelete());
    }
    private void bind(Expense e) {
        if (e == null) { finish(); return; }
        amountInput.setText(String.valueOf(e.amount));
        descriptionInput.setText(e.description);
        dateInput.setText(new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(e.date)));
        categoryInput.setText(e.category);
        typeSpinner.setSelection("FIRMA".equalsIgnoreCase(e.categoryType) ? 1 : 0);
    }
    private void onSave() {
        if (current == null) return;
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) { Toast.makeText(this, "Introdu o sumă", Toast.LENGTH_SHORT).show(); return; }
        current.amount = Double.parseDouble(amountStr);
        current.description = descriptionInput.getText().toString().trim();
        current.date = DatePickerUtil.parse(dateInput.getText().toString().trim());
        current.category = categoryInput.getText().toString().trim();
        current.categoryType = typeSpinner.getSelectedItem().toString();
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).expenseDao().update(current);
            runOnUiThread(() -> { Toast.makeText(this, "Actualizat", Toast.LENGTH_SHORT).show(); finish(); });
        }).start();
    }
    private void onDelete() {
        if (current == null) return;
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).expenseDao().deleteById(current.id);
            runOnUiThread(() -> { Toast.makeText(this, "Șters", Toast.LENGTH_SHORT).show(); finish(); });
        }).start();
    }
}
