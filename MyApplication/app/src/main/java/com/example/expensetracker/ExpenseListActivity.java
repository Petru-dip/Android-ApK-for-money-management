package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseListActivity extends BaseActivity {

    private RecyclerView recycler;
    private Spinner spinnerViewMode;
    private ChipGroup chipGroupType;
    private Chip chipAll, chipPersonal, chipFirma;

    private final String MODE_TX = "Tranzacții";
    private final String MODE_DAY = "Pe zile";
    private final String MODE_CAT = "Pe categorii";

    private ExpenseAdapter expenseAdapter;
    private KeyValueAdapter keyValueAdapter;

    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);
        setupToolbar(R.string.title_expenses, true);

        db = AppDatabase.getInstance(this);

        recycler = findViewById(R.id.recycler);
        spinnerViewMode = findViewById(R.id.spinnerViewMode);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipAll = findViewById(R.id.chipAll);
        chipPersonal = findViewById(R.id.chipPersonal);
        chipFirma = findViewById(R.id.chipFirma);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        expenseAdapter = new ExpenseAdapter(new ArrayList<Expense>());
        expenseAdapter.setOnExpenseClickListener(e -> {
            Intent intent = new Intent(this, EditExpenseActivity.class);
            intent.putExtra("expense_id", e.id);
            startActivity(intent);
        });

        keyValueAdapter = new KeyValueAdapter();

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{MODE_TX, MODE_DAY, MODE_CAT});
        spinnerViewMode.setAdapter(modeAdapter);

        spinnerViewMode.setOnItemSelectedListener(new SimpleItemSelectedListener(this::reload));
        chipGroupType.setOnCheckedStateChangeListener((group, ids) -> reload());

        reload();
    }

    private void reload() {
        String selectedMode = (String) spinnerViewMode.getSelectedItem();

        final String typeFilter;
        if (chipPersonal.isChecked()) typeFilter = "PERSONAL";
        else if (chipFirma.isChecked()) typeFilter = "FIRMA";
        else typeFilter = null; // toate

        new Thread(() -> {
            List<Expense> all = db.expenseDao().getAll();
            List<Expense> filtered = filterByType(all, typeFilter);

            if (MODE_TX.equals(selectedMode)) {
                runOnUiThread(() -> {
                    recycler.setAdapter(expenseAdapter);
                    expenseAdapter.submit(filtered);
                });
            } else if (MODE_DAY.equals(selectedMode)) {
                Map<String, Double> byDay = groupByDay(filtered);
                runOnUiThread(() -> {
                    recycler.setAdapter(keyValueAdapter);
                    keyValueAdapter.submit(fromMap(byDay));
                });
            } else { // Pe categorii
                Map<String, Double> byCat = groupByCategory(filtered);
                runOnUiThread(() -> {
                    recycler.setAdapter(keyValueAdapter);
                    keyValueAdapter.submit(fromMap(byCat));
                });
            }
        }).start();
    }

    private List<Expense> filterByType(List<Expense> src, String type) {
        if (type == null) return src;
        List<Expense> out = new ArrayList<>();
        for (Expense e : src) {
            if (type.equalsIgnoreCase(e.categoryType)) out.add(e);
        }
        return out;
    }

    private Map<String, Double> groupByDay(List<Expense> list) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Map<String, Double> map = new LinkedHashMap<>();
        for (Expense e : list) {
            String day = sdf.format(new java.util.Date(e.date));
            map.put(day, map.getOrDefault(day, 0.0) + e.amount);
        }
        return map;
    }

    private Map<String, Double> groupByCategory(List<Expense> list) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Expense e : list) {
            String cat = (e.category == null || e.category.isEmpty()) ? "(fără categorie)" : e.category;
            map.put(cat, map.getOrDefault(cat, 0.0) + e.amount);
        }
        return map;
    }

    private List<KeyValue> fromMap(Map<String, Double> map) {
        List<KeyValue> out = new ArrayList<>();
        for (Map.Entry<String, Double> en : map.entrySet()) {
            out.add(new KeyValue(en.getKey(), en.getValue()));
        }
        return out;
    }
}
