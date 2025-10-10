package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseListActivity extends BaseActivity {

    private RecyclerView recycler;
    private ExpenseAdapter adapter;

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
            runOnUiThread(() -> adapter.submitList(data));
        }).start();
    }
}
