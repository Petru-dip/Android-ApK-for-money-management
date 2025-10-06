package com.example.expensetracker;

import java.util.List;
import java.util.UUID;

public class UidBackfill {
    public static void ensureUids(AppDatabase db) {
        List<Expense> exp = db.expenseDao().getMissingUid();
        for (Expense e : exp) { e.uid = UUID.randomUUID().toString(); db.expenseDao().update(e); }
        List<Income> inc = db.incomeDao().getMissingUid();
        for (Income i : inc) { i.uid = UUID.randomUUID().toString(); db.incomeDao().update(i); }
    }
}
