package com.example.expensetracker;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
public class IncomeListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private IncomeAdapter adapter;
    private final List<Income> data = new ArrayList<>();
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);
        recyclerView = findViewById(R.id.recycler_incomes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomeAdapter(data);
        adapter.setOnIncomeClickListener(e -> {
            Intent i = new Intent(this, EditIncomeActivity.class);
            i.putExtra("id", e.id);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder tgt) { return false; }
            @Override public void onSwiped(RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                if (pos >= 0 && pos < data.size()) {
                    Income e = data.get(pos);
                    new AlertDialog.Builder(IncomeListActivity.this)
                            .setTitle("Ștergi venitul?")
                            .setMessage(e.description)
                            .setPositiveButton("Șterge", (d, w) -> {
                                MainActivity.shouldRefreshTotals = true;  // refresh la aplicatie pentru citire pret total
                                new Thread(() -> {
                                    AppDatabase.getInstance(getApplicationContext()).incomeDao().deleteById(e.id);
                                    runOnUiThread(() -> loadData());
                                }).start();
                            })
                            .setNegativeButton("Anulează", (d, w) -> { adapter.notifyItemChanged(pos); })
                            .setOnCancelListener(c -> adapter.notifyItemChanged(pos))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerView);
    }
    @Override protected void onResume() { super.onResume(); loadData(); }
    private void loadData() {
        new Thread(() -> {
            List<Income> result = AppDatabase.getInstance(getApplicationContext()).incomeDao().getAll();
            runOnUiThread(() -> { data.clear(); data.addAll(result); adapter.notifyDataSetChanged(); });
        }).start();
    }
}
