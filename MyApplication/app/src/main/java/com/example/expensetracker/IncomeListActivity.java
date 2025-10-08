package com.example.expensetracker;

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

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private Spinner spinnerViewMode;
    private ChipGroup chipGroupType;
    private Chip chipAll, chipPersonal, chipFirma;

    private final String MODE_TX = "Tranzacții";
    private final String MODE_DAY = "Pe zile";
    private final String MODE_SRC = "Pe tip sursă";

    private IncomeAdapter incomeAdapter;
    private KeyValueAdapter keyValueAdapter;

    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);
        setupToolbar(R.string.title_incomes, true);

        db = AppDatabase.getInstance(this);

        recycler = findViewById(R.id.recycler);
        spinnerViewMode = findViewById(R.id.spinnerViewMode);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipAll = findViewById(R.id.chipAll);
        chipPersonal = findViewById(R.id.chipPersonal);
        chipFirma = findViewById(R.id.chipFirma);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        incomeAdapter = new IncomeAdapter(new ArrayList<Income>());
        keyValueAdapter = new KeyValueAdapter();

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{MODE_TX, MODE_DAY, MODE_SRC});
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
            List<Income> all = db.incomeDao().getAll();
            List<Income> filtered = filterByType(all, typeFilter);

            if (MODE_TX.equals(selectedMode)) {
                runOnUiThread(() -> {
                    recycler.setAdapter(incomeAdapter);
                    incomeAdapter.submit(filtered);
                });
            } else if (MODE_DAY.equals(selectedMode)) {
                Map<String, Double> byDay = groupByDay(filtered);
                runOnUiThread(() -> {
                    recycler.setAdapter(keyValueAdapter);
                    keyValueAdapter.submit(fromMap(byDay));
                });
            } else { // Pe tip sursă
                Map<String, Double> bySource = groupBySource(filtered);
                runOnUiThread(() -> {
                    recycler.setAdapter(keyValueAdapter);
                    keyValueAdapter.submit(fromMap(bySource));
                });
            }
        }).start();
    }

    private List<Income> filterByType(List<Income> src, String type) {
        if (type == null) return src;
        List<Income> out = new ArrayList<>();
        for (Income i : src) {
            if (type.equalsIgnoreCase(i.sourceType)) out.add(i);
        }
        return out;
    }

    private Map<String, Double> groupByDay(List<Income> list) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Map<String, Double> map = new LinkedHashMap<>();
        for (Income i : list) {
            String day = sdf.format(new java.util.Date(i.date));
            map.put(day, map.getOrDefault(day, 0.0) + i.amount);
        }
        return map;
    }

    private Map<String, Double> groupBySource(List<Income> list) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Income i : list) {
            String src = (i.sourceType == null || i.sourceType.isEmpty()) ? "(necunoscut)" : i.sourceType;
            map.put(src, map.getOrDefault(src, 0.0) + i.amount);
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
