package com.example.expensetracker;

import androidx.room.*;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert void insert(Expense e);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(List<Expense> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReplace(List<Expense> list);

    @Update void update(Expense e);
    @Delete void delete(Expense e);

    @Query("DELETE FROM Expense WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM Expense WHERE id = :id LIMIT 1")
    Expense getById(int id);

    @Query("SELECT * FROM Expense WHERE uid = :uid LIMIT 1")
    Expense getByUid(String uid);

    @Query("SELECT * FROM Expense WHERE uid IS NULL OR uid = ''")
    List<Expense> getMissingUid();

    @Query("SELECT * FROM Expense ORDER BY date DESC")
    List<Expense> getAll();

    // ✅ Noua metodă care lipsea:
    @Query("SELECT * FROM Expense WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<Expense> getExpensesBetween(long from, long to);

    @Query("SELECT * FROM Expense WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<Expense> getByDateRange(long from, long to);

    @Query("SELECT * FROM Expense WHERE categoryType = :categoryType AND date BETWEEN :from AND :to ORDER BY date DESC")
    List<Expense> getByTypeAndDate(String categoryType, long from, long to);

    @Query("SELECT SUM(amount) FROM Expense WHERE categoryType = :categoryType AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String categoryType, long from, long to);

    @Query("SELECT SUM(amount) FROM Expense WHERE date BETWEEN :from AND :to")
    Double getTotalByDate(long from, long to);

    @Query("SELECT IFNULL(SUM(amount), 0) FROM Expense")
    double getTotalAmount();

    @Query("SELECT SUM(amount) FROM Expense WHERE categoryType = :categoryType")
    Double getTotalByCategoryType(String categoryType);

    @Query("SELECT * FROM Expense WHERE (:type IS NULL OR categoryType = :type) ORDER BY date DESC")
    List<Expense> getAllFiltered(String type);


}
