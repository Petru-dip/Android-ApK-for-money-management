package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

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

        // Swipe to delete with Undo
        ItemTouchHelper.SimpleCallback swipe = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder target) { return false; }
            @Override public void onSwiped(RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0) return;
                Income removed = adapter.removeAt(pos);
                com.google.android.material.snackbar.Snackbar.make(recycler, R.string.deleted, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> adapter.restoreAt(pos, removed))
                        .addCallback(new com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback<>() {
                            @Override public void onDismissed(com.google.android.material.snackbar.BaseTransientBottomBar<?> transientBottomBar, int event) {
                                if (event != com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION && removed != null) {
                                    new Thread(() -> AppDatabase.getInstance(getApplicationContext()).incomeDao().delete(removed)).start();
                                    MainActivity.shouldRefreshTotals = true;
                                }
                            }
                        })
                        .show();
            }
        };
        new ItemTouchHelper(swipe).attachToRecyclerView(recycler);

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
