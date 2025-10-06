package com.example.expensetracker;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
public class ExpenseListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Spinner typeSpinner, periodSpinner;
    private ExpenseAdapter adapter;
    private final List<Expense> data = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);
        recyclerView = findViewById(R.id.recycler_expenses);
        progressBar = findViewById(R.id.progress);
        typeSpinner = findViewById(R.id.spinner_type);
        periodSpinner = findViewById(R.id.spinner_period);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(data);
        adapter.setOnExpenseClickListener(e -> {
            Intent i = new Intent(this, EditExpenseActivity.class);
            i.putExtra("id", e.id);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);
        ArrayAdapter<CharSequence> types = ArrayAdapter.createFromResource(
                this, R.array.type_all_personal_firma, android.R.layout.simple_spinner_item);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(types);
        ArrayAdapter<CharSequence> periods = ArrayAdapter.createFromResource(
                this, R.array.periods_common, android.R.layout.simple_spinner_item);
        periods.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periods);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { loadData(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        typeSpinner.setOnItemSelectedListener(listener);
        periodSpinner.setOnItemSelectedListener(listener);
        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder tgt) { return false; }
            @Override public void onSwiped(RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                if (pos >= 0 && pos < data.size()) {
                    Expense e = data.get(pos);
                    new AlertDialog.Builder(ExpenseListActivity.this)
                            .setTitle("Ștergi cheltuiala?")
                            .setMessage(e.description)
                            .setPositiveButton("Șterge", (d, w) -> {
                                new Thread(() -> {
                                    AppDatabase.getInstance(getApplicationContext()).expenseDao().deleteById(e.id);
                                    runOnUiThread(() -> loadData());
                                }).start();
                            })
                            .setNegativeButton("Anulează", (d, w) -> { adapter.notifyItemChanged(pos); })
                            .setOnCancelListener(c -> adapter.notifyItemChanged(pos))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerView);
        loadData();
    }
    @Override protected void onResume() { super.onResume(); loadData(); }
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            long[] range = computeRange(periodSpinner.getSelectedItemPosition());
            String type = typeSpinner.getSelectedItem() == null ? "Toate" : typeSpinner.getSelectedItem().toString();
            List<Expense> result;
            if ("Toate".equalsIgnoreCase(type)) {
                result = AppDatabase.getInstance(getApplicationContext()).expenseDao().getExpensesBetween(range[0], range[1]);
            } else {
                result = AppDatabase.getInstance(getApplicationContext()).expenseDao().getByTypeAndDate(type.toUpperCase(), range[0], range[1]);
            }
            runOnUiThread(() -> {
                data.clear();
                data.addAll(result);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }
    private long[] computeRange(int periodIndex) {
        Calendar c = Calendar.getInstance();
        Calendar from = Calendar.getInstance();
        switch (periodIndex) {
            case 0: from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0); break;
            case 1:
                from.set(Calendar.DAY_OF_WEEK, from.getFirstDayOfWeek());
                from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
                break;
            case 2:
                from.set(Calendar.DAY_OF_MONTH, 1);
                from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
                break;
            case 3: from.add(Calendar.DAY_OF_YEAR, -30); break;
            default: from.add(Calendar.YEAR, -10);
        }
        return new long[]{from.getTimeInMillis(), c.getTimeInMillis()};
    }
}
