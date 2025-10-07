package com.example.expensetracker;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class EditIncomeActivity extends AppCompatActivity {
    private EditText amountInput, descriptionInput, dateInput;
    private Spinner sourceSpinner;
    private Income current;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_income);
        amountInput = findViewById(R.id.income_amount);
        descriptionInput = findViewById(R.id.income_description);
        dateInput = findViewById(R.id.income_date);
        sourceSpinner = findViewById(R.id.income_source_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.type_personal_firma, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(adapter);
        DatePickerUtil.attach(this, dateInput);
        int id = getIntent().getIntExtra("id", -1);
        if (id == -1) { finish(); return; }
        new Thread(() -> {
            current = AppDatabase.getInstance(getApplicationContext()).incomeDao().getById(id);
            runOnUiThread(() -> bind(current));
        }).start();
        Button save = findViewById(R.id.btn_save_income);
        Button delete = findViewById(R.id.btn_delete_income);
        save.setOnClickListener(v -> onSave());
        delete.setOnClickListener(v -> onDelete());
    }
    private void bind(Income e) {
        if (e == null) { finish(); return; }
        amountInput.setText(String.valueOf(e.amount));
        descriptionInput.setText(e.description);
        dateInput.setText(new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(e.date)));
        sourceSpinner.setSelection("FIRMA".equalsIgnoreCase(e.sourceType) ? 1 : 0);
    }
    private void onSave() {
        if (current == null) return;
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) { Toast.makeText(this, "Introdu o sumă", Toast.LENGTH_SHORT).show(); return; }
        current.amount = Double.parseDouble(amountStr);
        current.description = descriptionInput.getText().toString().trim();
        current.date = DatePickerUtil.parse(dateInput.getText().toString().trim());
        current.sourceType = sourceSpinner.getSelectedItem().toString();
        MainActivity.shouldRefreshTotals = true;
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).incomeDao().update(current);
            runOnUiThread(() -> { Toast.makeText(this, "Actualizat", Toast.LENGTH_SHORT).show(); finish(); });
        }).start();
    }
    private void onDelete() {
        if (current == null) return;
        MainActivity.shouldRefreshTotals = true;
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).incomeDao().deleteById(current.id);
            runOnUiThread(() -> { Toast.makeText(this, "Șters", Toast.LENGTH_SHORT).show(); finish(); });
        }).start();
    }
}
