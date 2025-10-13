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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<Income> data = db.incomeDao().getAll(); // sau getAllOrderByDateDesc()
            runOnUiThread(() -> setData(data));
        }).start();
    }

    private void setData(List<Income> items) {
        adapter.setItems(items);
        boolean isEmpty = (items == null || items.isEmpty());
        if (emptyView != null) emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
