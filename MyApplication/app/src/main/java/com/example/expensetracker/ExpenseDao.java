package com.example.expensetracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Query("SELECT * FROM expense ORDER BY date DESC")
    List<Expense> getAll();

    @Query("SELECT * FROM expense WHERE id = :id LIMIT 1")
    Expense getById(int id);

    @Query("SELECT * FROM expense WHERE uid = :uid LIMIT 1")
    Expense getByUid(String uid);

    @Insert
    void insert(Expense e);

    @Update
    void update(Expense e);

    @Delete
    void delete(Expense e);

    // folosite în MainActivity
    @Query("SELECT SUM(amount) FROM expense")
    Double getTotalAmount();

    @Query("SELECT SUM(amount) FROM expense WHERE categoryType = :type")
    Double getTotalByCategoryType(String type);

    // folosite de ExpenseReportActivity (logurile tale arătau apeluri aici)
    @Query("SELECT SUM(amount) FROM expense WHERE categoryType = :type AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String type, long from, long to);

    // folosit de UidBackfill (ai avut eroare anterior)
    @Query("SELECT * FROM expense WHERE uid IS NULL OR uid = ''")
    List<Expense> getMissingUid();
}
