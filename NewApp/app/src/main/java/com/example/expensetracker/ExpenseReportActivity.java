package com.example.expensetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ExpenseReportActivity extends BaseActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvTotals;
    private AutoCompleteTextView dropdownPeriod, dropdownCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_report);

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        tvTotals = findViewById(R.id.tv_totals);
        dropdownPeriod = findViewById(R.id.dropdown_period);
        dropdownCategory = findViewById(R.id.dropdown_category);

        setupFilters();
        loadDataAndRender();
    }

    private void setupFilters() {
        String[] periods = {"Luna curentă", "Luna trecută", "Anul curent"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, periods);
        dropdownPeriod.setAdapter(periodAdapter);
        dropdownPeriod.setText(periods[0], false);

        String[] categories = {"Toate", "PERSONAL", "FIRMA"}; // Adaugă și alte categorii dacă e necesar
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        dropdownCategory.setAdapter(categoryAdapter);
        dropdownCategory.setText(categories[0], false);

        dropdownPeriod.setOnItemClickListener((parent, view, position, id) -> loadDataAndRender());
        dropdownCategory.setOnItemClickListener((parent, view, position, id) -> loadDataAndRender());
    }

    private void loadDataAndRender() {
        new Thread(() -> {
            String selectedPeriod = dropdownPeriod.getText().toString();
            String selectedCategory = dropdownCategory.getText().toString();

            long[] range = getDateRange(selectedPeriod);
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            Double totalExpenses, totalIncome;
            List<PieEntry> pieEntries = new ArrayList<>();
            List<BarEntry> barEntries = new ArrayList<>();

            if (selectedCategory.equals("Toate")) {
                Double expensesPersonal = nvl(db.expenseDao().getTotalByTypeAndDate("PERSONAL", range[0], range[1]));
                Double expensesFirma = nvl(db.expenseDao().getTotalByTypeAndDate("FIRMA", range[0], range[1]));
                Double incomePersonal = nvl(db.incomeDao().getTotalByTypeAndDate("PERSONAL", range[0], range[1]));
                Double incomeFirma = nvl(db.incomeDao().getTotalByTypeAndDate("FIRMA", range[0], range[1]));

                totalExpenses = expensesPersonal + expensesFirma;
                totalIncome = incomePersonal + incomeFirma;

                if (expensesPersonal > 0) pieEntries.add(new PieEntry(expensesPersonal.floatValue(), "Chelt. personale"));
                if (expensesFirma > 0) pieEntries.add(new PieEntry(expensesFirma.floatValue(), "Chelt. firmă"));

                barEntries.add(new BarEntry(0f, new float[]{incomePersonal.floatValue(), -expensesPersonal.floatValue()}));
                barEntries.add(new BarEntry(1f, new float[]{incomeFirma.floatValue(), -expensesFirma.floatValue()}));
            } else {
                totalExpenses = nvl(db.expenseDao().getTotalByTypeAndDate(selectedCategory, range[0], range[1]));
                totalIncome = nvl(db.incomeDao().getTotalByTypeAndDate(selectedCategory, range[0], range[1]));

                if (totalExpenses > 0) pieEntries.add(new PieEntry(totalExpenses.floatValue(), "Cheltuieli " + selectedCategory));
                barEntries.add(new BarEntry(0f, new float[]{totalIncome.floatValue(), -totalExpenses.floatValue()}));
            }
            double balance = totalIncome - totalExpenses;

            runOnUiThread(() -> {
                renderPieChart(pieEntries, selectedPeriod);
                renderBarChart(barEntries, selectedCategory);
                tvTotals.setText(String.format(getString(R.string.totals_template), totalIncome, totalExpenses, balance));
            });
        }).start();
    }

    private void renderPieChart(List<PieEntry> entries, String period) {
        PieDataSet pieDataSet = new PieDataSet(entries, "Distribuție cheltuieli (" + period.toLowerCase() + ")");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(12f);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Cheltuieli");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.invalidate();
    }

    private void renderBarChart(List<BarEntry> entries, String category) {
        BarDataSet barDataSet = new BarDataSet(entries, "Venit (+) / Cheltuială (-)");
        barDataSet.setColors(new int[]{Color.GREEN, Color.RED});
        barDataSet.setStackLabels(new String[]{"Venit", "Cheltuială"});

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);

        List<String> labels = new ArrayList<>();
        if (category.equals("Toate")) {
            labels.addAll(Arrays.asList("Personal", "Firma"));
        } else {
            labels.add(category);
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }

    private long[] getDateRange(String period) {
        Calendar to = Calendar.getInstance();
        Calendar from = Calendar.getInstance();

        switch (period) {
            case "Luna trecută":
                from.add(Calendar.MONTH, -1);
                from.set(Calendar.DAY_OF_MONTH, 1);
                to.add(Calendar.MONTH, -1);
                to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case "Anul curent":
                from.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Luna curentă":
            default:
                from.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }
        from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
        to.set(Calendar.HOUR_OF_DAY, 23); to.set(Calendar.MINUTE, 59); to.set(Calendar.SECOND, 59);

        return new long[]{from.getTimeInMillis(), to.getTimeInMillis()};
    }

    private static Double nvl(Double d) {
        return d == null ? 0.0 : d;
    }
}
