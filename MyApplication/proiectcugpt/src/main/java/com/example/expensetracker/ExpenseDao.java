package com.example.expensetracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Query("SELECT * FROM Expense WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    List<Expense> getExpensesBetween(long start, long end);

    @Query("SELECT * FROM Expense ORDER BY date DESC")
    List<Expense> getAll();

    @Query("SELECT strftime('%Y-%m-%d', date/1000, 'unixepoch') as day, SUM(amount) as total " +
            "FROM Expense WHERE date BETWEEN :start AND :end GROUP BY day ORDER BY total DESC LIMIT :limit")
    List<MostExpensiveDay> getTopDays(long start, long end, int limit);
}
