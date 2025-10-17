//java
package com.example.expensetracker;

import android.content.Context;
import android.database.Cursor;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Expense.class, Income.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract ExpenseDao expenseDao();
    public abstract IncomeDao incomeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app-db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            // .fallbackToDestructiveMigration() // dev only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // existing migration logic (unchanged)
            boolean hasCategoryType = false;
            Cursor cursor = null;
            String expenseTableCandidate = null;
            try {
                cursor = db.query("PRAGMA table_info('Expense')");
                if (cursor != null && cursor.getCount() > 0) expenseTableCandidate = "Expense";
                if (cursor != null) cursor.close();

                if (expenseTableCandidate == null) {
                    cursor = db.query("PRAGMA table_info('expense')");
                    if (cursor != null && cursor.getCount() > 0) expenseTableCandidate = "expense";
                }

                if (expenseTableCandidate != null) {
                    if (cursor == null) cursor = db.query("PRAGMA table_info('" + expenseTableCandidate + "')");
                    int nameIndex = cursor != null ? cursor.getColumnIndex("name") : -1;
                    while (cursor != null && cursor.moveToNext()) {
                        if (nameIndex != -1 && "categoryType".equals(cursor.getString(nameIndex))) {
                            hasCategoryType = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) cursor.close();
            }

            if (!hasCategoryType && expenseTableCandidate != null) {
                db.execSQL("ALTER TABLE `" + expenseTableCandidate + "` ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'PERSONAL'");
            }

            // ensure some form of income table exists for older versions
            db.execSQL("CREATE TABLE IF NOT EXISTS `income` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uid` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`description` TEXT, " +
                    "`date` INTEGER NOT NULL, " +
                    "`category` TEXT, " +
                    "`categoryType` TEXT)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Cursor cursor = null;
            String sourceTable = null;
            try {
                // Check for existing table named exactly 'Income'
                cursor = db.query("PRAGMA table_info('Income')");
                if (cursor != null && cursor.getCount() > 0) {
                    sourceTable = "Income";
                }
                if (cursor != null) cursor.close();

                // If not found, check lowercase 'income'
                if (sourceTable == null) {
                    cursor = db.query("PRAGMA table_info('income')");
                    if (cursor != null && cursor.getCount() > 0) {
                        sourceTable = "income";
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            // Create the exact table Room expects as a temporary table
            db.execSQL("CREATE TABLE IF NOT EXISTS `Income_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uid` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`description` TEXT, " +
                    "`date` INTEGER NOT NULL, " +
                    "`category` TEXT, " +
                    "`categoryType` TEXT)");

            // Copy existing data if any source table exists
            if (sourceTable != null) {
                // only copy columns that exist in the old table; assume common names
                db.execSQL("INSERT INTO `Income_new` (id, uid, amount, description, date, category, categoryType) " +
                        "SELECT id, uid, amount, description, date, category, categoryType FROM `" + sourceTable + "`");
            }

            // Drop old table if its name differs from the desired 'Income'
            if (sourceTable != null && !sourceTable.equals("Income")) {
                db.execSQL("DROP TABLE IF EXISTS `" + sourceTable + "`");
            }

            // Remove any previous 'Income' table and rename the new one to the exact expected name
            db.execSQL("DROP TABLE IF EXISTS `Income`");
            db.execSQL("ALTER TABLE `Income_new` RENAME TO `Income`");

            // Create the unique index Room expects (exact name and uniqueness)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Income_uid` ON `Income` (`uid`)");

            // Note: do NOT create additional indexes here that Room does not expect.
        }
    };
}
