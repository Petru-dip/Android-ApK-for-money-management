package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private Calendar selectedDate;
    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "expenses-db")
                .allowMainThreadQueries()
                .build();

        selectedDate = Calendar.getInstance();

        Button btnDate = findViewById(R.id.btnDate);
        EditText etDesc = findViewById(R.id.etDescription);
        EditText etAmount = findViewById(R.id.etAmount);
        Spinner spCategory = findViewById(R.id.spCategory);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnToList = findViewById(R.id.btnToList);
        Button btnToReport = findViewById(R.id.btnToReport);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        btnDate.setText(df.format(selectedDate.getTime()));
        btnDate.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        btnDate.setText(df.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnSave.setOnClickListener(v -> {
            String desc = etDesc.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String cat = spCategory.getSelectedItem().toString();

            if (desc.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Completează descrierea și suma.", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Suma invalidă.", Toast.LENGTH_SHORT).show();
                return;
            }

            Expense e = new Expense();
            e.description = desc;
            e.amount = amount;
            e.category = cat;
            e.date = selectedDate.getTimeInMillis();

            db.expenseDao().insert(e);
            Toast.makeText(this, "Cheltuială salvată!", Toast.LENGTH_SHORT).show();
            etDesc.setText("");
            etAmount.setText("");
        });

        btnToList.setOnClickListener(v -> startActivity(new Intent(this, ExpenseListActivity.class)));
        btnToReport.setOnClickListener(v -> startActivity(new Intent(this, ExpenseReportActivity.class)));
    }
}
