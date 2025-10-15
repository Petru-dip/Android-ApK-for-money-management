package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private TextView emptyView;
    private IncomeAdapter adapter;
    private List<Income> fullData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);
        setupToolbar(R.string.title_incomes, true);

        recycler  = findViewById(R.id.recycler_incomes);
        emptyView = findViewById(R.id.empty_view_incomes);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomeAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnIncomeClickListener(new IncomeAdapter.OnIncomeClickListener() {
            @Override
            public void onEdit(Income income) {
                Intent it = new Intent(IncomeListActivity.this, EditIncomeActivity.class);
                it.putExtra("income_id", income.id);
                it.putExtra("income_uid", income.uid); // păstrăm uid-ul pentru corelare cu notificările
                startActivity(it);
            }

            @Override
            public void onDelete(Income income) {
                new AlertDialog.Builder(IncomeListActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.confirm_delete_income)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            new Thread(() -> {
                                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                                db.incomeDao().delete(income);
                                List<Income> fresh = db.incomeDao().getAll();
                                runOnUiThread(() -> {
                                    setData(fresh);
                                    Toast.makeText(IncomeListActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                                    MainActivity.shouldRefreshTotals = true;
                                });
                            }).start();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        loadData();

        // filtre chips + raport
        initFilters();
        findViewById(R.id.btn_income_report).setOnClickListener(v -> {
            android.content.Intent it = new android.content.Intent(this, ReportActivity.class);
            it.putExtra(ReportActivity.EXTRA_TYPE, "income");
            startActivity(it);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<Income> data = db.incomeDao().getAll();
            runOnUiThread(() -> {
                fullData = data;
                setData(data);
            });
        }).start();
    }

    private void setData(List<Income> items) {
        adapter.setItems(items);
        boolean isEmpty = (items == null || items.isEmpty());
        if (emptyView != null) emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void initFilters() {
        View chipAll = findViewById(R.id.chip_income_all);
        View chipSalary = findViewById(R.id.chip_income_salary);
        View chipBonus = findViewById(R.id.chip_income_bonus);
        View chipFamily = findViewById(R.id.chip_income_family);
        View chipReceived = findViewById(R.id.chip_income_received);

        View.OnClickListener l = v -> applyFilter(v.getId());
        chipAll.setOnClickListener(l);
        chipSalary.setOnClickListener(l);
        chipBonus.setOnClickListener(l);
        chipFamily.setOnClickListener(l);
        chipReceived.setOnClickListener(l);
    }

    private void applyFilter(int id) {
        if (fullData == null) return;
        List<Income> src = fullData;
        java.util.ArrayList<Income> filtered = new java.util.ArrayList<>();
        String key = null;
        if (id == R.id.chip_income_salary) key = "salariu";
        else if (id == R.id.chip_income_bonus) key = "bonus";
        else if (id == R.id.chip_income_family) key = "familie";
        else if (id == R.id.chip_income_received) key = "primit";

        if (key == null) {
            setData(src);
            return;
        }
        for (Income i : src) {
            String s1 = i.sourceType == null ? "" : i.sourceType.toLowerCase();
            String s2 = i.description == null ? "" : i.description.toLowerCase();
            if (s1.contains(key) || s2.contains(key)) filtered.add(i);
        }
        setData(filtered);
    }

    private void showReport() {
        if (fullData == null || fullData.isEmpty()) {
            Toast.makeText(this, R.string.no_incomes_yet, Toast.LENGTH_SHORT).show();
            return;
        }
        java.util.Map<String, Double> sums = new java.util.HashMap<>();
        for (Income i : fullData) {
            String key = i.sourceType == null ? "Altele" : i.sourceType;
            sums.put(key, (sums.getOrDefault(key, 0.0)) + i.amount);
        }
        StringBuilder msg = new StringBuilder();
        for (java.util.Map.Entry<String, Double> e : sums.entrySet()) {
            msg.append(e.getKey()).append(": ")
               .append(String.format(java.util.Locale.getDefault(), "%.2f RON", e.getValue()))
               .append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Raport venituri pe tip")
                .setMessage(msg.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
