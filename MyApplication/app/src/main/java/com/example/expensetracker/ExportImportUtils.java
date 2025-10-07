package com.example.expensetracker;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportImportUtils {

    // =============================
    // === EXPORT TO EXCEL (.xlsx)
    // =============================
    public static void exportToExcel(Context ctx, AppDatabase db, Uri outUri) throws Exception {
        List<Expense> expenses = db.expenseDao().getAll();
        List<Income> incomes = db.incomeDao().getAll();

        if (expenses.isEmpty() && incomes.isEmpty()) {
            throw new Exception("Nu există date pentru export.");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            // === Sheet 1: Cheltuieli ===
            Sheet expenseSheet = workbook.createSheet("Cheltuieli");
            Row header = expenseSheet.createRow(0);
            header.createCell(0).setCellValue("Categorie");
            header.createCell(1).setCellValue("Tip (PERSONAL/FIRMA)");
            header.createCell(2).setCellValue("Sumă");
            header.createCell(3).setCellValue("Descriere");
            header.createCell(4).setCellValue("Dată");

            int rowIdx = 1;
            for (Expense e : expenses) {
                Row r = expenseSheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(e.category != null ? e.category : "");
                r.createCell(1).setCellValue(e.categoryType != null ? e.categoryType : "");
                r.createCell(2).setCellValue(e.amount);
                r.createCell(3).setCellValue(e.description != null ? e.description : "");
                r.createCell(4).setCellValue(sdf.format(new Date(e.date)));
            }

            // === Sheet 2: Venituri ===
            Sheet incomeSheet = workbook.createSheet("Venituri");
            Row header2 = incomeSheet.createRow(0);
            header2.createCell(0).setCellValue("Tip sursă");
            header2.createCell(1).setCellValue("Sumă");
            header2.createCell(2).setCellValue("Descriere");
            header2.createCell(3).setCellValue("Dată");

            int rowIdx2 = 1;
            for (Income i : incomes) {
                Row r = incomeSheet.createRow(rowIdx2++);
                r.createCell(0).setCellValue(i.sourceType != null ? i.sourceType : "");
                r.createCell(1).setCellValue(i.amount);
                r.createCell(2).setCellValue(i.description != null ? i.description : "");
                r.createCell(3).setCellValue(sdf.format(new Date(i.date)));
            }

            try (OutputStream os = ctx.getContentResolver().openOutputStream(outUri)) {
                workbook.write(os);
                os.flush();
            }
        }
    }

    // ==================================
    // === IMPORT FROM EXCEL (.xlsx)
    // ==================================
    public static ExcelImportResult importFromExcel(Context ctx, AppDatabase db, Uri inUri) throws Exception {
        int expensesImported = 0;
        int incomesImported = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        try (InputStream is = ctx.getContentResolver().openInputStream(inUri);
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            // === Sheet 1: Cheltuieli ===
            Sheet expenseSheet = workbook.getSheet("Cheltuieli");
            if (expenseSheet != null) {
                for (int i = 1; i <= expenseSheet.getLastRowNum(); i++) {
                    Row r = expenseSheet.getRow(i);
                    if (r == null) continue;

                    Expense e = new Expense();
                    e.category = getStringCell(r.getCell(0));
                    e.categoryType = getStringCell(r.getCell(1));
                    e.amount = getNumericCell(r.getCell(2));
                    e.description = getStringCell(r.getCell(3));

                    String dateStr = getStringCell(r.getCell(4));
                    if (!dateStr.isEmpty()) {
                        e.date = sdf.parse(dateStr).getTime();
                    } else {
                        e.date = System.currentTimeMillis();
                    }

                    e.uid = java.util.UUID.randomUUID().toString();
                    db.expenseDao().insert(e);
                    expensesImported++;
                }
            }

            // === Sheet 2: Venituri ===
            Sheet incomeSheet = workbook.getSheet("Venituri");
            if (incomeSheet != null) {
                for (int i = 1; i <= incomeSheet.getLastRowNum(); i++) {
                    Row r = incomeSheet.getRow(i);
                    if (r == null) continue;

                    Income inc = new Income();
                    inc.sourceType = getStringCell(r.getCell(0));
                    inc.amount = getNumericCell(r.getCell(1));
                    inc.description = getStringCell(r.getCell(2));

                    String dateStr = getStringCell(r.getCell(3));
                    if (!dateStr.isEmpty()) {
                        inc.date = sdf.parse(dateStr).getTime();
                    } else {
                        inc.date = System.currentTimeMillis();
                    }

                    inc.uid = java.util.UUID.randomUUID().toString();
                    db.incomeDao().insert(inc);
                    incomesImported++;
                }
            }
        }

        return new ExcelImportResult(expensesImported, incomesImported);
    }

    // === Helper pentru citire sigură de celule ===
    private static String getStringCell(Cell cell) {
        if (cell == null) return "";
        try {
            return cell.getStringCellValue();
        } catch (Exception e) {
            try {
                return String.valueOf(cell.getNumericCellValue());
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static double getNumericCell(Cell cell) {
        if (cell == null) return 0;
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    // === REZULTAT IMPORT ===
    public static class ExcelImportResult {
        public int expensesCount;
        public int incomesCount;

        public ExcelImportResult(int e, int i) {
            this.expensesCount = e;
            this.incomesCount = i;
            MainActivity.shouldRefreshTotals = true;  // refresh la aplicatie pentru citire pret total
        }

        @Override
        public String toString() {
            return "Cheltuieli importate: " + expensesCount + "\nVenituri importate: " + incomesCount;
        }
    }

    // === EXPORT JSON pentru backup intern ===
    public static void exportJsonToStream(Context ctx, AppDatabase db, OutputStream os) throws Exception {
        org.json.JSONObject root = new org.json.JSONObject();

        java.util.List<Expense> expenses = db.expenseDao().getAll();
        org.json.JSONArray expArray = new org.json.JSONArray();
        for (Expense e : expenses) {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("uid", e.uid);
            o.put("amount", e.amount);
            o.put("description", e.description);
            o.put("date", e.date);
            o.put("category", e.category);
            o.put("categoryType", e.categoryType);
            expArray.put(o);
        }
        root.put("expenses", expArray);

        java.util.List<Income> incomes = db.incomeDao().getAll();
        org.json.JSONArray incArray = new org.json.JSONArray();
        for (Income i : incomes) {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("uid", i.uid);
            o.put("amount", i.amount);
            o.put("description", i.description);
            o.put("date", i.date);
            o.put("sourceType", i.sourceType);
            incArray.put(o);
        }
        root.put("incomes", incArray);

        try (OutputStreamWriter writer = new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(root.toString(2));
            writer.flush();
        }
    }
}
