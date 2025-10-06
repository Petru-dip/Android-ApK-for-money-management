package com.example.expensetracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseListActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExpenseAdapter adapter;
    private TextView tvTotal;
    private RadioGroup rgFilter;
    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "expenses-db")
                .allowMainThreadQueries()
                .build();

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        tvTotal = findViewById(R.id.tvTotal);
        rgFilter = findViewById(R.id.rgFilter);

        rgFilter.setOnCheckedChangeListener((group, checkedId) -> reload());
        reload();
    }

    private void reload() {
        long[] range;
        int checkedId = rgFilter.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDay) range = DateUtils.getTodayRange();
        else if (checkedId == R.id.rbWeek) range = DateUtils.getThisWeekRange();
        else if (checkedId == R.id.rbMonth) range = DateUtils.getThisMonthRange();
        else range = new long[]{0, Long.MAX_VALUE};

        List<Expense> data = db.expenseDao().getExpensesBetween(range[0], range[1]);
        adapter.setData(data);

        double total = 0;
        for (Expense e : data) total += e.amount;
        tvTotal.setText("Total: " + total + " RON");
    }
}
