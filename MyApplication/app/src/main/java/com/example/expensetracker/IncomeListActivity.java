package com.example.expensetracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private TextView emptyView;
    private IncomeAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layoutul din ZIP are RecyclerView cu id @id/recycler_incomes
        setContentView(R.layout.activity_income_list);
        // Dacă toolbar-ul e prezent în layout-ul comun, va seta titlul; dacă nu, metoda iese fără efect
        setupToolbar(R.string.title_incomes, true);

        recycler = findViewById(R.id.recycler_incomes);
        // emptyView e opțional – îl afișăm dacă îl adaugi în layout; dacă nu există, rămâne null
        emptyView = findViewById(
                getResources().getIdentifier("empty_view_incomes", "id", getPackageName())
        );

        if (recycler == null) {
            throw new IllegalStateException(
                    "RecyclerView null: verifică să existe @+id/recycler_incomes în activity_income_list.xml");
        }

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomeAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<Income> data = AppDatabase.getInstance(getApplicationContext())
                    .incomeDao()
                    .getAll(); // DAO-ul din ZIP are getAll() (ORDER BY date DESC)

            runOnUiThread(() -> {
                if (data == null || data.isEmpty()) {
                    if (emptyView != null) {
                        recycler.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    adapter.submit(data); // face notifyDataSetChanged() în interior
                }
            });
        }).start();
    }
}
