package com.example.expensetracker;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
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

            // === Sheet 1: Cheltuieli ===
            Sheet expenseSheet = workbook.createSheet("Cheltuieli");
            Row header = expenseSheet.createRow(0);
            header.createCell(0).setCellValue("Categorie");
            header.createCell(1).setCellValue("Sumă");
            header.createCell(2).setCellValue("Descriere");
            header.createCell(3).setCellValue("Dată");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            int rowIdx = 1;
            for (Expense e : expenses) {
                Row r = expenseSheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(e.category != null ? e.category : "");
                r.createCell(1).setCellValue(e.amount);
                r.createCell(2).setCellValue(e.description != null ? e.description : "");
                r.createCell(3).setCellValue(sdf.format(new Date(e.date)));
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

        try (InputStream is = ctx.getContentResolver().openInputStream(inUri);
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            // === Sheet 1: Cheltuieli ===
            Sheet expenseSheet = workbook.getSheet("Cheltuieli");
            if (expenseSheet != null) {
                for (int i = 1; i <= expenseSheet.getLastRowNum(); i++) {
                    Row r = expenseSheet.getRow(i);
                    if (r == null) continue;
                    Expense e = new Expense();
                    e.category = r.getCell(0).getStringCellValue();
                    e.amount = r.getCell(1).getNumericCellValue();
                    e.description = r.getCell(2).getStringCellValue();
                    e.date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse(r.getCell(3).getStringCellValue()).getTime();
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
                    inc.sourceType = r.getCell(0).getStringCellValue();
                    inc.amount = r.getCell(1).getNumericCellValue();
                    inc.description = r.getCell(2).getStringCellValue();
                    inc.date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse(r.getCell(3).getStringCellValue()).getTime();
                    inc.uid = java.util.UUID.randomUUID().toString();
                    db.incomeDao().insert(inc);
                    incomesImported++;
                }
            }
        }

        return new ExcelImportResult(expensesImported, incomesImported);
    }

    // ==================================
    // === REZULTAT IMPORT
    // ==================================
    public static class ExcelImportResult {
        public int expensesCount;
        public int incomesCount;

        public ExcelImportResult(int e, int i) {
            this.expensesCount = e;
            this.incomesCount = i;
        }

        @Override
        public String toString() {
            return "Cheltuieli importate: " + expensesCount + "\nVenituri importate: " + incomesCount;
        }
    }
}
