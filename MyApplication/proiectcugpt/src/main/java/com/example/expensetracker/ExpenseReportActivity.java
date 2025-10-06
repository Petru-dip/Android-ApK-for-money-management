package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseReportActivity extends AppCompatActivity {

    private AppDatabase db;
    private Calendar start = Calendar.getInstance();
    private Calendar end = Calendar.getInstance();
    private TextView tvSummary;
    private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_report);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "expenses-db")
                .allowMainThreadQueries()
                .build();

        Button btnStart = findViewById(R.id.btnStartDate);
        Button btnEnd = findViewById(R.id.btnEndDate);
        Button btnCalc = findViewById(R.id.btnCalc);
        Button btnPdf = findViewById(R.id.btnPdf);
        tvSummary = findViewById(R.id.tvSummary);

        // default: această săptămână
        long[] week = DateUtils.getThisWeekRange();
        start.setTimeInMillis(week[0]);
        end.setTimeInMillis(week[1]);

        btnStart.setText(df.format(start.getTime()));
        btnEnd.setText(df.format(end.getTime()));

        btnStart.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(ExpenseReportActivity.this,
                    (view, y, m, d) -> { start.set(y, m, d); btnStart.setText(df.format(start.getTime())); },
                    start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });
        btnEnd.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(ExpenseReportActivity.this,
                    (view, y, m, d) -> { end.set(y, m, d); btnEnd.setText(df.format(end.getTime())); },
                    end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnCalc.setOnClickListener(v -> calcSummary());
        btnPdf.setOnClickListener(v -> exportPdf());
        calcSummary();
    }

    private void calcSummary() {
        long startMs = DateUtils.startOfDay(start.getTimeInMillis());
        long endMs = DateUtils.endOfDay(end.getTimeInMillis());
        List<Expense> data = db.expenseDao().getExpensesBetween(startMs, endMs);

        double total = 0;
        for (Expense e : data) total += e.amount;

        List<MostExpensiveDay> topDays = db.expenseDao().getTopDays(startMs, endMs, 5);

        StringBuilder sb = new StringBuilder();
        sb.append("Perioadă: ").append(df.format(start.getTime())).append(" - ").append(df.format(end.getTime())).append("\n");
        sb.append("Total cheltuieli: ").append(total).append(" RON\n\n");
        sb.append("Top zile costisitoare:\n");
        for (MostExpensiveDay d : topDays) {
            sb.append(" - ").append(d.day).append(": ").append(d.total).append(" RON\n");
        }
        tvSummary.setText(sb.toString());
    }

    private void exportPdf() {
        long startMs = DateUtils.startOfDay(start.getTimeInMillis());
        long endMs = DateUtils.endOfDay(end.getTimeInMillis());
        List<Expense> data = db.expenseDao().getExpensesBetween(startMs, endMs);

        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595; // A4 at 72dpi
        int pageHeight = 842;
        int y = 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas c = page.getCanvas();

        paint.setTextSize(16);
        c.drawText("Raport Cheltuieli", 40, y, paint); y += 24;
        paint.setTextSize(12);
        c.drawText("Perioadă: " + new java.text.SimpleDateFormat("dd/MM/yyyy").format(start.getTime())
                + " - " + new java.text.SimpleDateFormat("dd/MM/yyyy").format(end.getTime()), 40, y, paint); y += 20;

        double total = 0;
        for (Expense e : data) total += e.amount;
        c.drawText("Total: " + total + " RON", 40, y, paint); y += 20;
        y += 10;
        c.drawText("Listă cheltuieli:", 40, y, paint); y += 18;

        for (Expense e : data) {
            String line = new java.text.SimpleDateFormat("dd/MM").format(new java.util.Date(e.date))
                    + "  |  " + e.category + "  |  " + e.description + "  |  " + e.amount + " RON";
            if (y > pageHeight - 40) {
                doc.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, doc.getPages().size()+1).create();
                page = doc.startPage(pageInfo);
                c = page.getCanvas();
                y = 40;
            }
            c.drawText(line, 40, y, paint);
            y += 16;
        }

        doc.finishPage(page);

        // Save to app-specific external files (no runtime permission needed)
        File outDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outDir != null && !outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "Raport_Cheltuieli_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            doc.writeTo(fos);
            Toast.makeText(this, "PDF salvat: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Eroare PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            doc.close();
        }
    }
}
