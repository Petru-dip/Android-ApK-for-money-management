// java
package com.example.expensetracker;

import android.content.Context;
import android.database.Cursor;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Expense.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract ExpenseDao expenseDao();
    public abstract IncomeDao incomeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expenses_db")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // corrected: migration should be from 1 to 2 (was 1->3)
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // add new column on Expense only if it doesn't already exist
            boolean hasCategoryType = false;
            Cursor cursor = null;
            try {
                cursor = db.query("PRAGMA table_info(Expense)");
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && "categoryType".equals(cursor.getString(nameIndex))) {
                        hasCategoryType = true;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // if any error occurs, fall back to attempting the alter (execSQL below will fail if column exists)
            } finally {
                if (cursor != null) cursor.close();
            }

            if (!hasCategoryType) {
                db.execSQL("ALTER TABLE Expense ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'PERSONAL'");
            }

            // create Income table (idempotent)
            db.execSQL("CREATE TABLE IF NOT EXISTS Income (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "description TEXT, " +
                    "amount REAL NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "sourceType TEXT NOT NULL)");
            // indices recommended (idempotent)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Expense_date ON Expense(date)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Income_date ON Income(date)");
        }
    };
}
