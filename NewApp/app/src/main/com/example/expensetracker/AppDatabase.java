package com.example.expensetracker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Expense.class, Income.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExpenseDao expenseDao();
    public abstract IncomeDao incomeDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    AppDatabase.class, "finance.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // placeholder dacă treci din v1 la v2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { /* no-op */ }
    };

    // v2 -> v3: adaugă coloanele uid + indexuri unice
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Expense ADD COLUMN uid TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_expense_uid ON Expense(uid)");
            db.execSQL("ALTER TABLE Income ADD COLUMN uid TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_income_uid ON Income(uid)");
        }
    };
}
