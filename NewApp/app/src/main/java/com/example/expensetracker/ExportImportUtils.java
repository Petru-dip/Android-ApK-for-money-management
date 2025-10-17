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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExportImportUtils {

    // =========================================================
    // ===============   Tipuri pentru import   ================
    // =========================================================

    /** Rezolvarea aleasă de utilizator pentru import. */
    public enum Resolution {
        EXCLUDE_DUPLICATES,  // Omite duplicatele (importă doar înregistrările noi)
        REPLACE_DUPLICATES,  // Rescrie în DB înregistrările care se repetă
        CANCEL               // Anulează importul
    }

    /** Pachetul de date citite din Excel (încă nescrise în DB) + număr duplicate. */
    public static class PendingExcelImport {
        public final List<Expense> expenses;
//        public final List<Income> incomes;
        public final int duplicateExpenses;
//        public final int duplicateIncomes;

        public PendingExcelImport(List<Expense> expenses,
                                  int duplicateExpenses
                                  /*,int duplicateIncomes*/) {
            this.expenses = expenses;
            this.duplicateExpenses = duplicateExpenses;
            /*this.duplicateIncomes = duplicateIncomes;*/
        }

        public boolean hasDuplicates() {
            return duplicateExpenses > 0 /*|| duplicateIncomes > 0*/;
        }
    }

    /** Rezultatul final al importului (după commit) + sumar pentru UI. */
    public static class ExcelImportResult {
        public int expensesInserted;
//        public int incomesInserted;
        public int expensesUpdated;
//        public int incomesUpdated;
        public int expensesSkipped;
//        public int incomesSkipped;
        public Resolution resolution;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Cheltuieli – inserate: ").append(expensesInserted)
                    .append(", actualizate: ").append(expensesUpdated)
                    .append(", omise: ").append(expensesSkipped).append("\n");

            return sb.toString();
        }
    }

    // =========================================================
    // ================   Export la Excel (.xlsx)   ============
    // =========================================================

    public static void exportToExcel(Context ctx, AppDatabase db, Uri outUri) throws Exception {
        List<Expense> expenses = db.expenseDao().getAll();
//        List<Income> incomes = db.incomeDao().getAll();

        if (expenses.isEmpty() /*&& incomes.isEmpty()*/) {
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
//            for (Income i : incomes) {
//                Row r = incomeSheet.createRow(rowIdx2++);
//                r.createCell(0).setCellValue(i.sourceType != null ? i.sourceType : "");
//                r.createCell(1).setCellValue(i.amount);
//                r.createCell(2).setCellValue(i.description != null ? i.description : "");
//                r.createCell(3).setCellValue(sdf.format(new Date(i.date)));
//            }

            try (OutputStream os = ctx.getContentResolver().openOutputStream(outUri)) {
                workbook.write(os);
                os.flush();
            }
        }
    }

    // =========================================================
    // ==================   Import – logică   ==================
    // =========================================================

    /** Normalizări + chei de potrivire pentru duplicate. */
    private static String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String normAmount(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // Dacă vrei „egalitate la zi”, normalizează data aici (comentariu mai jos).
    private static long normDate(long millis) {
        return millis;
        // La nivel de zi:
        // java.util.Calendar c = java.util.Calendar.getInstance();
        // c.setTimeInMillis(millis);
        // c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        // c.set(java.util.Calendar.MINUTE, 0);
        // c.set(java.util.Calendar.SECOND, 0);
        // c.set(java.util.Calendar.MILLISECOND, 0);
        // return c.getTimeInMillis();
    }

    private static String expenseKey(Expense e) {
        return safe(e.categoryType) + "|" + safe(e.category) + "|" +
                normAmount(e.amount) + "|" + safe(e.description) + "|" + normDate(e.date);
    }

//    private static String incomeKey(Income i) {
//        return safe(i.sourceType) + "|" + normAmount(i.amount) + "|" +
//                safe(i.description) + "|" + normDate(i.date);
//    }

    /**
     * Citește Excel-ul și pregătește importul (fără să scrie în DB). Returnează
     * lista elementelor și numărul de duplicate față de conținutul actual din DB.
     */
    public static PendingExcelImport prepareImportFromExcel(Context ctx, AppDatabase db, Uri inUri) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        List<Expense> importedExpenses = new ArrayList<>();
//        List<Income> importedIncomes = new ArrayList<>();

        try (InputStream is = ctx.getContentResolver().openInputStream(inUri);
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            // --- Sheet Cheltuieli ---
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
                    e.date = dateStr.isEmpty() ? System.currentTimeMillis() : sdf.parse(dateStr).getTime();
                    e.uid = UUID.randomUUID().toString();

                    importedExpenses.add(e);
                }
            }

            // --- Sheet Venituri ---
            Sheet incomeSheet = workbook.getSheet("Venituri");
            if (incomeSheet != null) {
                for (int i = 1; i <= incomeSheet.getLastRowNum(); i++) {
                    Row r = incomeSheet.getRow(i);
                    if (r == null) continue;

//                    Income inc = new Income();
//                    inc.sourceType = getStringCell(r.getCell(0));
//                    inc.amount = getNumericCell(r.getCell(1));
//                    inc.description = getStringCell(r.getCell(2));
//                    String dateStr = getStringCell(r.getCell(3));
//                    inc.date = dateStr.isEmpty() ? System.currentTimeMillis() : sdf.parse(dateStr).getTime();
//                    inc.uid = UUID.randomUUID().toString();
//
//                    importedIncomes.add(inc);
                }
            }
        }

        // Construiește seturile/indecșii de chei existente în DB
        Set<String> existingExpenseKeys = new HashSet<>();
        for (Expense e : db.expenseDao().getAll()) {
            existingExpenseKeys.add(expenseKey(e));
        }

        Set<String> existingIncomeKeys = new HashSet<>();
//        for (Income i : db.incomeDao().getAll()) {
//            existingIncomeKeys.add(incomeKey(i));
//        }

        int dupExpenses = 0;
        for (Expense e : importedExpenses) {
            if (existingExpenseKeys.contains(expenseKey(e))) dupExpenses++;
        }

        int dupIncomes = 0;
//        for (Income i : importedIncomes) {
//            if (existingIncomeKeys.contains(incomeKey(i))) dupIncomes++;
//        }

        return new PendingExcelImport(importedExpenses,/*importedIncomes,*/ dupExpenses/*, dupIncomes*/);
    }

    /**
     * Aplică scrierea în DB în funcție de rezoluția aleasă de utilizator.
     * - EXCLUDE_DUPLICATES: inserează doar înregistrările care NU există deja
     * - REPLACE_DUPLICATES: actualizează înregistrările existente și inserează ce e nou
     * - CANCEL: nu scrie nimic
     */
    public static ExcelImportResult commitImport(AppDatabase db, PendingExcelImport pending, Resolution resolution) {
        ExcelImportResult out = new ExcelImportResult();
        out.resolution = resolution;

        if (resolution == Resolution.CANCEL) {
            return out; // nimic de făcut
        }

        // Indexează existentele din DB după cheie
        Map<String, Expense> expByKey = new HashMap<>();
        for (Expense e : db.expenseDao().getAll()) {
            expByKey.put(expenseKey(e), e);
        }
//        Map<String, Income> incByKey = new HashMap<>();
//        for (Income i : db.incomeDao().getAll()) {
//            incByKey.put(incomeKey(i), i);
//        }

        // --- Cheltuieli ---
        for (Expense imp : pending.expenses) {
            String k = expenseKey(imp);
            Expense exist = expByKey.get(k);
            if (exist == null) {
                // nou
                db.expenseDao().insert(imp);
                out.expensesInserted++;
            } else if (resolution == Resolution.REPLACE_DUPLICATES) {
                // rescrie duplicatele (păstrează id/uid existente)
                exist.amount = imp.amount;
                exist.description = imp.description;
                exist.category = imp.category;
                exist.categoryType = imp.categoryType;
                exist.date = imp.date;
                db.expenseDao().update(exist);
                out.expensesUpdated++;
            } else {
                // EXCLUDE_DUPLICATES => se omite
                out.expensesSkipped++;
            }
        }

        // --- Venituri ---
//        for (Income imp : pending.incomes) {
//            String k = incomeKey(imp);
//            Income exist = incByKey.get(k);
//            if (exist == null) {
//                db.incomeDao().insert(imp);
//                out.incomesInserted++;
//            } else if (resolution == Resolution.REPLACE_DUPLICATES) {
//                exist.amount = imp.amount;
//                exist.description = imp.description;
//                exist.sourceType = imp.sourceType;
//                exist.date = imp.date;
//                db.incomeDao().update(exist);
//                out.incomesUpdated++;
//            } else {
//                out.incomesSkipped++;
//            }
//        }

        // notifică UI (dacă folosești acest flag în MainActivity)
        MainActivity.shouldRefreshTotals = true;
        return out;
    }

    /**
     * Wrapper compatibil cu apelul vechi: importă și omite duplicatele.
     * (Dacă vrei alt comportament implicit, schimbă rezoluția de mai jos.)
     */
    public static ExcelImportResult importFromExcel(Context ctx, AppDatabase db, Uri inUri) throws Exception {
        PendingExcelImport pending = prepareImportFromExcel(ctx, db, inUri);
        return commitImport(db, pending, Resolution.EXCLUDE_DUPLICATES);
    }

    // =========================================================
    // ==================  Helperi celule Excel  ===============
    // =========================================================

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

    // =========================================================
    // ================  Export JSON (backup)  =================
    // =========================================================

    public static void exportJsonToStream(Context ctx, AppDatabase db, OutputStream os) throws Exception {
        org.json.JSONObject root = new org.json.JSONObject();

        List<Expense> expenses = db.expenseDao().getAll();
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
//
//        List<Income> incomes = db.incomeDao().getAll();
//        org.json.JSONArray incArray = new org.json.JSONArray();
//        for (Income i : incomes) {
//            org.json.JSONObject o = new org.json.JSONObject();
//            o.put("uid", i.uid);
//            o.put("amount", i.amount);
//            o.put("description", i.description);
//            o.put("date", i.date);
//            o.put("sourceType", i.sourceType);
//            incArray.put(o);
//        }
//        root.put("incomes", incArray);
//
//        try (OutputStreamWriter writer = new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8)) {
//            writer.write(root.toString(2));
//            writer.flush();
//        }
    }
}
