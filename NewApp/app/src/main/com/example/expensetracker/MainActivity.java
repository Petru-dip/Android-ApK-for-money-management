package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppDatabase db;
    private Calendar selectedDate;

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
        Button btnSave = findViewById(R.id.btnSave);
        Button btnReport = findViewById(R.id.btnReport);
        TextView tvResult = findViewById(R.id.tvResult);

        btnDate.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        btnDate.setText(dayOfMonth + "/" + (month+1) + "/" + year);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnSave.setOnClickListener(v -> {
            Expense e = new Expense();
            e.description = etDesc.getText().toString();
            e.amount = Double.parseDouble(etAmount.getText().toString());
            e.date = selectedDate.getTimeInMillis();
            db.expenseDao().insert(e);
            Toast.makeText(this, "Cheltuială salvată!", Toast.LENGTH_SHORT).show();
        });

        btnReport.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            long weekAgo = now - 7L*24*60*60*1000;

            List<Expense> thisWeek = db.expenseDao().getExpensesBetween(weekAgo, now);
            double totalWeek = 0;
            for (Expense e : thisWeek) totalWeek += e.amount;

            MostExpensiveDay med = db.expenseDao().getMostExpensiveDay();

            String result = "Cheltuieli în ultima săptămână: " + totalWeek + " RON\n" +
                    "Cea mai scumpă zi: " + (med != null ? med.day + " (" + med.total + " RON)" : "-");
            tvResult.setText(result);
        });
    }
}
