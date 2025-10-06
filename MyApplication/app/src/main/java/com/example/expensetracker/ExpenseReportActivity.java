package com.example.expensetracker;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
public class ExpenseReportActivity extends AppCompatActivity {
    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvTotals;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_report);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        tvTotals = findViewById(R.id.tv_totals);
        loadDataAndRender();
    }
    private void loadDataAndRender() {
        new Thread(() -> {
            long[] range = currentMonth();
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Double expensesPersonal = nvl(db.expenseDao().getTotalByTypeAndDate("PERSONAL", range[0], range[1]));
            Double expensesFirma = nvl(db.expenseDao().getTotalByTypeAndDate("FIRMA", range[0], range[1]));
            Double incomePersonal = nvl(db.incomeDao().getTotalByTypeAndDate("PERSONAL", range[0], range[1]));
            Double incomeFirma = nvl(db.incomeDao().getTotalByTypeAndDate("FIRMA", range[0], range[1]));
            double totalExpenses = expensesPersonal + expensesFirma;
            double totalIncome = incomePersonal + incomeFirma;
            double balance = totalIncome - totalExpenses;
            runOnUiThread(() -> {
                List<PieEntry> pieEntries = new ArrayList<>();
                if (expensesPersonal > 0) pieEntries.add(new PieEntry(expensesPersonal.floatValue(), "Chelt. personale"));
                if (expensesFirma > 0) pieEntries.add(new PieEntry(expensesFirma.floatValue(), "Chelt. firmă"));
                PieDataSet pieDataSet = new PieDataSet(pieEntries, "Distribuție cheltuieli (luna curentă)");
                PieData pieData = new PieData(pieDataSet);
                pieChart.setData(pieData);
                Description d1 = new Description(); d1.setText(""); pieChart.setDescription(d1); pieChart.invalidate();
                List<BarEntry> barEntries = new ArrayList<>();
                barEntries.add(new BarEntry(0f, new float[]{incomePersonal.floatValue(), -expensesPersonal.floatValue()}));
                barEntries.add(new BarEntry(1f, new float[]{incomeFirma.floatValue(), -expensesFirma.floatValue()}));
                BarDataSet barDataSet = new BarDataSet(barEntries, "Venit (+) / Cheltuială (-)");
                barDataSet.setStackLabels(new String[]{"Venit", "Cheltuială"});
                BarData barData = new BarData(barDataSet);
                barChart.setData(barData);
                Description d2 = new Description(); d2.setText(""); barChart.setDescription(d2);
                barChart.getXAxis().setGranularity(1f);
                barChart.invalidate();
                tvTotals.setText(String.format("Venit total: %.2f | Cheltuieli totale: %.2f | Sold: %.2f", totalIncome, totalExpenses, balance));
            });
        }).start();
    }
    private static Double nvl(Double d) { return d == null ? 0.0 : d; }
    private long[] currentMonth() {
        Calendar to = Calendar.getInstance();
        Calendar from = Calendar.getInstance();
        from.set(Calendar.DAY_OF_MONTH, 1);
        from.set(Calendar.HOUR_OF_DAY, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);
        return new long[]{from.getTimeInMillis(), to.getTimeInMillis()};
    }
}
