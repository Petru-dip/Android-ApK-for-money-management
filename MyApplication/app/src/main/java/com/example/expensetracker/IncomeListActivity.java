package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private IncomeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);   // asigură-te că acesta e layoutul listei
        setupToolbar(R.string.title_incomes, true);

        recycler = findViewById(R.id.recycler_incomes); // ID-ul din XML (vezi layoutul de mai jos)
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomeAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnIncomeClickListener(new IncomeAdapter.OnIncomeClickListener() {
            @Override
            public void onEdit(Income income) {
                Intent it = new Intent(IncomeListActivity.this, EditIncomeActivity.class);
                it.putExtra("income_id", income.id);
                it.putExtra("income_uid", income.uid);
                startActivity(it);
            }

            @Override
            public void onDelete(Income income) {
                new AlertDialog.Builder(IncomeListActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.confirm_delete_income)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            new Thread(() -> {
                                AppDatabase.getInstance(getApplicationContext())
                                        .incomeDao().delete(income);
                                runOnUiThread(() -> {
                                    Toast.makeText(IncomeListActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                                    loadData(); // reîncarcă lista
                                    MainActivity.shouldRefreshTotals = true;
                                });
                            }).start();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // după ce revii din EditIncomeActivity, reîncarcă (în caz de update)
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            // Folosește metoda ta reală din IncomeDao:
            // List<Income> data = db.incomeDao().getAllOrderByDateDesc();
            List<Income> data = db.incomeDao().getAll(); // dacă asta aveai deja
            runOnUiThread(() -> adapter.submitList(data));
        }).start();
    }
}
