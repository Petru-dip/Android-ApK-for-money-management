package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ecran raport: pie + bar, pe perioadă selectabilă. */
public class ReportActivity extends BaseActivity {

    public static final String EXTRA_TYPE = "report_type"; // "income" | "expense"

    private PieChart pieChart;
    private BarChart barChart;
    private MaterialAutoCompleteTextView periodDropdown;
    private String type; // income / expense

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        boolean isIncome = "income".equalsIgnoreCase(type);
//        setupToolbar(isIncome ? R.string.title_incomes : R.string.title_expenses, true);

//        pieChart = findViewById(R.id.pieChart);
//        barChart = findViewById(R.id.barChart);
//        periodDropdown = findViewById(R.id.dropdown_period);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.periods_common, android.R.layout.simple_list_item_1);
        periodDropdown.setAdapter(adapter);

        // Restaurează ultima perioadă (pref)
        String lastPeriod = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("period_last", null);
        if (lastPeriod != null) {
            periodDropdown.setText(lastPeriod, false);
        } else {
            periodDropdown.setText(adapter.getItem(2)); // Luna curentă implicit
        }

        periodDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String sel = periodDropdown.getText().toString();
            getSharedPreferences("prefs", MODE_PRIVATE).edit()
                    .putString("period_last", sel)
                    .apply();
            loadAndRender();
        });

        loadAndRender();
    }

    private void loadAndRender() {
        // Încarcă pe thread separat, agregă datele pe categorie/tip
        new Thread(() -> {
            long now = System.currentTimeMillis();
            long from = Periods.resolveFromMillis(periodDropdown.getText() == null ? "" : periodDropdown.getText().toString(), now);

            Map<String, Double> sums = new HashMap<>();
            if ("income".equalsIgnoreCase(type)) {
//                List<Income> data = AppDatabase.getInstance(getApplicationContext()).incomeDao().getAll();
//                for (Income i : data) {
//                    if (i.date < from) continue;
//                    String key = i.sourceType == null || i.sourceType.isEmpty() ? "Altele" : i.sourceType;
//                    sums.put(key, sums.getOrDefault(key, 0.0) + i.amount);
//                }
            } else {
                List<Expense> data = AppDatabase.getInstance(getApplicationContext()).expenseDao().getAll();
                for (Expense e : data) {
                    if (e.date < from) continue;
                    String key = e.category == null || e.category.isEmpty() ? "Altele" : e.category;
                    sums.put(key, sums.getOrDefault(key, 0.0) + e.amount);
                }
            }

            runOnUiThread(() -> renderCharts(sums));
        }).start();
    }

    private void renderCharts(Map<String, Double> sums) {
        if (sums == null || sums.isEmpty()) {
            Toast.makeText(this, R.string.no_incomes_yet, Toast.LENGTH_SHORT).show();
            pieChart.clear();
            barChart.clear();
            return;
        }

        List<PieEntry> pie = new ArrayList<>();
        List<BarEntry> bars = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, Double> e : sums.entrySet()) {
            pie.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
            bars.add(new BarEntry(idx, e.getValue().floatValue()));
            colors.add(CategoryMeta.pickColor(this, e.getKey()));
            idx++;
        }

        PieDataSet pieSet = new PieDataSet(pie, "");
        pieSet.setColors(colors);
        pieSet.setValueTextColor(0xFF000000);
        PieData pieData = new PieData(pieSet);
        pieData.setValueTextSize(12f);
        pieChart.setData(pieData);
        Description pd = new Description(); pd.setText(""); pieChart.setDescription(pd);
        pieChart.invalidate();

        BarDataSet barSet = new BarDataSet(bars, "");
        barSet.setColors(colors);
        BarData barData = new BarData(barSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        Description bd = new Description(); bd.setText(""); barChart.setDescription(bd);
        barChart.getXAxis().setGranularity(1f);
        barChart.invalidate();
    }

    // Util perioade
    static class Periods {
        static long resolveFromMillis(String periodLabel, long now) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(now);
            if (periodLabel == null) periodLabel = "";
            String p = periodLabel.toLowerCase(Locale.getDefault());
            if (p.contains("azi")) {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            } else if (p.contains("săptămâna")) {
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            } else if (p.contains("luna")) {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            } else if (p.contains("30")) {
                return now - 30L * 24 * 60 * 60 * 1000;
            }
            return now - 30L * 24 * 60 * 60 * 1000;
        }
    }
}


