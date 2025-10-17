package com.example.expensetracker;

import java.util.List;

public class BackupModels {
    public static class ExportContainer {
        public int schemaVersion;
        public long exportedAt;
        public List<Expense> expenses;
//        public List<Income> incomes;
    }
}
