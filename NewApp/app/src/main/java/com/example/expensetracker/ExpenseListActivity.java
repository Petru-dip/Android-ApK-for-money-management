package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseListActivity extends BaseActivity {

    private RecyclerView recycler;
    private ExpenseAdapter adapter;
    private java.util.List<Expense> fullData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);
        setupToolbar(R.string.title_expenses, true);

        recycler = findViewById(R.id.expense_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter();
        recycler.setAdapter(adapter);

        adapter.setListener(new ExpenseAdapter.Listener() {
            @Override public void onRowClick(Expense e) { openEdit(e); }
            @Override public void onEdit(Expense e) { openEdit(e); }
            @Override public void onDelete(Expense e) { confirmDelete(e); }
            @Override public void onRowLongClick(Expense e) { confirmDelete(e); }
        });

        loadData();

        // filtre + raport
        initFilters();
        findViewById(R.id.btn_expense_report).setOnClickListener(v -> {
            android.content.Intent it = new android.content.Intent(this, ReportActivity.class);
            it.putExtra(ReportActivity.EXTRA_TYPE, "expense");
            startActivity(it);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        loadData();
    }

    private void openEdit(Expense e) {
        Intent i = new Intent(this, EditExpenseActivity.class);
        i.putExtra("expense_id", e.id);
        i.putExtra("expense_uid", e.uid);
        startActivity(i);
    }

    private void confirmDelete(Expense e) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete_expense)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getApplicationContext())
                                .expenseDao().delete(e);
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
            List<Expense> data = AppDatabase.getInstance(getApplicationContext())
                    .expenseDao().getAll();
            runOnUiThread(() -> {
                fullData = data;
                adapter.submitList(data);
            });
        }).start();
    }

    private void initFilters() {
        View chipAll = findViewById(R.id.chip_expense_all);
        View chipFood = findViewById(R.id.chip_expense_food);
        View chipHealth = findViewById(R.id.chip_expense_health);
        View chipTransport = findViewById(R.id.chip_expense_transport);
        View chipHome = findViewById(R.id.chip_expense_home);

        View.OnClickListener l = v -> applyFilter(v.getId());
        chipAll.setOnClickListener(l);
        chipFood.setOnClickListener(l);
        chipHealth.setOnClickListener(l);
        chipTransport.setOnClickListener(l);
        chipHome.setOnClickListener(l);
    }

    private void applyFilter(int id) {
        if (fullData == null) return;
        java.util.ArrayList<Expense> filtered = new java.util.ArrayList<>();
        String key = null;
        if (id == R.id.chip_expense_food) key = "mancare";
        else if (id == R.id.chip_expense_health) key = "sanatate";
        else if (id == R.id.chip_expense_transport) key = "transport";
        else if (id == R.id.chip_expense_home) key = "casa";

        if (key == null) {
            adapter.submitList(fullData);
            return;
        }
        for (Expense e : fullData) {
            String c = e.category == null ? "" : e.category.toLowerCase();
            if (c.contains(key)) filtered.add(e);
        }
        adapter.submitList(filtered);
    }

    private void showReport() {
        if (fullData == null || fullData.isEmpty()) return;
        java.util.Map<String, Double> sums = new java.util.HashMap<>();
        for (Expense e : fullData) {
            String key = e.category == null || e.category.isEmpty() ? "Altele" : e.category;
            sums.put(key, sums.getOrDefault(key, 0.0) + e.amount);
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Double> it : sums.entrySet()) {
            sb.append(it.getKey()).append(": ")
              .append(String.format(java.util.Locale.getDefault(), "%.2f RON", it.getValue()))
              .append("\n");
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Raport cheltuieli pe categorie")
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
