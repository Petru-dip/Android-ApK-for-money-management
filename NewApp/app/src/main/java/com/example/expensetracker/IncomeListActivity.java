// java
package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.List;

public class IncomeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private IncomeAdapter adapter;
    private List<Income> fullData;
    private List<Income> currentData;
    private SortMode sortMode = SortMode.DATE_DESC;
    private long rangeFrom = 0L, rangeTo = Long.MAX_VALUE;
    private MaterialAutoCompleteTextView dropdownPeriod;
    private enum SortMode { DATE_DESC, CATEGORY, DATE_ASC }

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

        dropdownPeriod = findViewById(R.id.dropdown_income_period);
        setupPeriodDropdown();

        adapter.setListener(new IncomeAdapter.Listener() {
            @Override public void onRowClick(Income e) { openEdit(e); }
            @Override public void onEdit(Income e) { openEdit(e); }
            @Override public void onDelete(Income e) { confirmDelete(e); }
            @Override public void onRowLongClick(Income e) { confirmDelete(e); }
        });

        loadData();

        // filtre + raport
        initFilters();
        MaterialButton sortBtn = findViewById(R.id.btn_sort_income);
        if (sortBtn != null) {
            sortBtn.setOnClickListener(v -> {
                cycleSort();
                sortBtn.setText(getSortLabel());
                publishCurrentData();
            });
            sortBtn.setText(getSortLabel());
        }
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
        updateRangeFromDropdown();
        new Thread(() -> {
            List<Income> data = AppDatabase.getInstance(getApplicationContext())
                    .incomeDao().getByRangeAndCategory(rangeFrom, rangeTo, null, null);
            runOnUiThread(() -> {
                fullData = data;
                currentData = new java.util.ArrayList<>(data);
                publishCurrentData();
            });
        }).start();
    }

    private void initFilters() {
        View chipAll = findViewById(R.id.chip_income_all);
        View chipSal = findViewById(R.id.chip_income_salariu);
        View chipBonus = findViewById(R.id.chip_income_Bonus);
        View chipFamilie = findViewById(R.id.chip_income_familie);
        View chipPrimit = findViewById(R.id.chip_income_primit);
        View chipAlte = findViewById(R.id.chip_income_altele);

        View.OnClickListener l = v -> applyFilter(v.getId());
        chipAll.setOnClickListener(l);
        chipSal.setOnClickListener(l);
        chipBonus.setOnClickListener(l);
        chipFamilie.setOnClickListener(l);
        chipPrimit.setOnClickListener(l);
        chipAlte.setOnClickListener(l);

    }

    private void applyFilter(int id) {
        if (fullData == null) return;
        java.util.ArrayList<Income> filtered = new java.util.ArrayList<>();
        String key = null;
        if (id == R.id.chip_income_salariu) key = "salariu";
        else if (id == R.id.chip_income_Bonus) key = "bonus";
        else if (id == R.id.chip_income_familie) key = "familie";
        else if (id == R.id.chip_income_primit) key = "primit";
        else if (id == R.id.chip_income_altele) key = "altele";

        if (key == null) {
            currentData = new java.util.ArrayList<>(fullData);
            publishCurrentData();
            return;
        }
        for (Income e : fullData) {
            String c = normalize(e.category);
            if ("altele".equals(key)) {
                if (c.isEmpty()) filtered.add(e);
            } else if (c.contains(key)) {
                filtered.add(e);
            }
        }
        currentData = filtered;
        publishCurrentData();
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

    private void publishCurrentData() {
        if (currentData == null) return;
        java.util.List<Income> copy = new java.util.ArrayList<>(currentData);
        java.util.Collections.sort(copy, (a, b) -> {
            if (sortMode == SortMode.CATEGORY) {
                String ca = normalize(a.category);
                String cb = normalize(b.category);
                int cmp = ca.compareTo(cb);
                if (cmp != 0) return cmp;
            }
            if (sortMode == SortMode.DATE_ASC) {
                int cmpDate = Long.compare(a.date, b.date);
                if (cmpDate != 0) return cmpDate;
                return Integer.compare(a.id, b.id);
            } else {
                int cmpDate = Long.compare(b.date, a.date);
                if (cmpDate != 0) return cmpDate;
                return Integer.compare(b.id, a.id);
            }
        });
        adapter.submitList(copy);
    }

    private void cycleSort() {
        switch (sortMode) {
            case DATE_DESC -> sortMode = SortMode.CATEGORY;
            case CATEGORY -> sortMode = SortMode.DATE_ASC;
            case DATE_ASC -> sortMode = SortMode.DATE_DESC;
        }
    }

    private String getSortLabel() {
        return switch (sortMode) {
            case DATE_DESC -> "Ordine: dată (recent)";
            case DATE_ASC -> "Ordine: dată (vechi)";
            case CATEGORY -> "Ordine: categorie";
        };
    }

    private String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        String normalized = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void setupPeriodDropdown() {
        if (dropdownPeriod == null) return;
        String[] options = {
                "Tot", "Azi", "Săptămâna curentă", "Săptămâna trecută",
                "Luna curentă", "Luna trecută", "Anul curent", "Perioadă custom"
        };
        dropdownPeriod.setSimpleItems(options);
        dropdownPeriod.setText("Tot", false);
        dropdownPeriod.setOnItemClickListener((parent, view, position, id) -> {
            String sel = dropdownPeriod.getText().toString();
            if (sel.contains("custom")) {
                showRangePicker();
            } else {
                updateRangeFromDropdown();
                loadData();
            }
        });
    }

    private void showRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Alege perioada");
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(sel -> {
            rangeFrom = sel.first;
            rangeTo = sel.second;
            dropdownPeriod.setText("Perioadă custom", false);
            loadData();
        });
        picker.show(getSupportFragmentManager(), "range_income");
    }

    private void updateRangeFromDropdown() {
        String sel = dropdownPeriod != null ? dropdownPeriod.getText().toString() : "Tot";
        long[] r = resolveRange(sel);
        rangeFrom = r[0];
        rangeTo = r[1];
    }

    private long[] resolveRange(String label) {
        java.util.Calendar from = java.util.Calendar.getInstance();
        java.util.Calendar to = java.util.Calendar.getInstance();

        to.set(java.util.Calendar.HOUR_OF_DAY, 23);
        to.set(java.util.Calendar.MINUTE, 59);
        to.set(java.util.Calendar.SECOND, 59);

        switch (label) {
            case "Azi":
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);
                break;
            case "Săptămâna curentă":
                from.set(java.util.Calendar.DAY_OF_WEEK, from.getFirstDayOfWeek());
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);
                break;
            case "Săptămâna trecută":
                from.add(java.util.Calendar.WEEK_OF_YEAR, -1);
                from.set(java.util.Calendar.DAY_OF_WEEK, from.getFirstDayOfWeek());
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);

                to.add(java.util.Calendar.WEEK_OF_YEAR, -1);
                to.set(java.util.Calendar.DAY_OF_WEEK, to.getFirstDayOfWeek() + 6);
                to.set(java.util.Calendar.HOUR_OF_DAY, 23);
                to.set(java.util.Calendar.MINUTE, 59);
                to.set(java.util.Calendar.SECOND, 59);
                break;
            case "Luna curentă":
                from.set(java.util.Calendar.DAY_OF_MONTH, 1);
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);
                break;
            case "Luna trecută":
                from.add(java.util.Calendar.MONTH, -1);
                from.set(java.util.Calendar.DAY_OF_MONTH, 1);
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);

                to.add(java.util.Calendar.MONTH, -1);
                to.set(java.util.Calendar.DAY_OF_MONTH, to.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                to.set(java.util.Calendar.HOUR_OF_DAY, 23);
                to.set(java.util.Calendar.MINUTE, 59);
                to.set(java.util.Calendar.SECOND, 59);
                break;
            case "Anul curent":
                from.set(java.util.Calendar.DAY_OF_YEAR, 1);
                from.set(java.util.Calendar.HOUR_OF_DAY, 0);
                from.set(java.util.Calendar.MINUTE, 0);
                from.set(java.util.Calendar.SECOND, 0);
                break;
            case "Tot":
            default:
                return new long[]{0L, Long.MAX_VALUE};
        }
        return new long[]{from.getTimeInMillis(), to.getTimeInMillis()};
    }
}
