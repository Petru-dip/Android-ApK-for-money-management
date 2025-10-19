// java
package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private IncomeAdapter adapter;
    private List<Income> fullData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);
        setupToolbar(R.string.title_add_incomes, true);

        recycler = findViewById(R.id.income_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomeAdapter();
        recycler.setAdapter(adapter);

        adapter.setListener(new IncomeAdapter.Listener() {
            @Override public void onRowClick(Income e) { openEdit(e); }
            @Override public void onEdit(Income e) { openEdit(e); }
            @Override public void onDelete(Income e) { confirmDelete(e); }
            @Override public void onRowLongClick(Income e) { confirmDelete(e); }
        });

        loadData();

        // filtre + raport
        initFilters();
        findViewById(R.id.btn_income_report).setOnClickListener(v -> {
            Intent it = new Intent(this, ReportActivity.class);
            it.putExtra(ReportActivity.EXTRA_TYPE, "income");
            startActivity(it);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        loadData();
    }

    private void openEdit(Income e) {
        Intent i = new Intent(this, EditIncomeActivity.class);
        i.putExtra("income_id", e.id);
        i.putExtra("income_uid", e.uid);
        startActivity(i);
    }

    private void confirmDelete(Income e) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_income)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getApplicationContext())
                                .incomeDao().delete(e);
                        runOnUiThread(() -> {
                            MainActivity.shouldRefreshTotals = true;
                            loadData();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadData() {
        new Thread(() -> {
            List<Income> data = AppDatabase.getInstance(getApplicationContext())
                    .incomeDao().getAll();
            runOnUiThread(() -> {
                fullData = data;
                adapter.submitList(data);
            });
        }).start();
    }

    private void initFilters() {
        View chipAll = findViewById(R.id.chip_income_all);
//        View chipFood = findViewById(R.id.chip_income_food);
//        View chipHealth = findViewById(R.id.chip_income_health);
//        View chipTransport = findViewById(R.id.chip_income_transport);
//        View chipHome = findViewById(R.id.chip_income_home);

        View.OnClickListener l = v -> applyFilter(v.getId());
        chipAll.setOnClickListener(l);
//        chipFood.setOnClickListener(l);
//        chipHealth.setOnClickListener(l);
//        chipTransport.setOnClickListener(l);
//        chipHome.setOnClickListener(l);
    }

    private void applyFilter(int id) {
        if (fullData == null) return;
        java.util.ArrayList<Income> filtered = new java.util.ArrayList<>();
        String key = null;
//        if (id == R.id.chip_income_food) key = "mancare";
//        else if (id == R.id.chip_income_health) key = "sanatate";
//        else if (id == R.id.chip_income_transport) key = "transport";
//        else if (id == R.id.chip_income_home) key = "casa";

        if (key == null) {
            adapter.submitList(fullData);
            return;
        }
        for (Income e : fullData) {
            String c = e.category == null ? "" : e.category.toLowerCase();
            if (c.contains(key)) filtered.add(e);
        }
        adapter.submitList(filtered);
    }

    private void showReport() {
        if (fullData == null || fullData.isEmpty()) return;
        java.util.Map<String, Double> sums = new java.util.HashMap<>();
        for (Income e : fullData) {
            String key = e.category == null || e.category.isEmpty() ? "Altele" : e.category;
            sums.put(key, sums.getOrDefault(key, 0.0) + e.amount);
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Double> it : sums.entrySet()) {
            sb.append(it.getKey()).append(": ")
                    .append(String.format(java.util.Locale.getDefault(), "%.2f RON", it.getValue()))
                    .append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Raport cheltuieli pe categorie")
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
