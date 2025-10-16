package com.example.expensetracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface IncomeDao {

    @Query("SELECT * FROM Income")
    List<Income> getAll();

    @Query("SELECT * FROM Income ORDER BY date DESC")
    List<Income> getAllOrderByDateDesc();

    @Query("SELECT * FROM Income WHERE id = :id LIMIT 1")
    Income getById(int id);

    @Query("SELECT * FROM Income WHERE uid = :uid LIMIT 1")
    Income getByUid(String uid);

    @Insert
    long insert(Income income);

    @Update
    int update(Income income);

    @Delete
    int delete(Income income);

    // cerute de MainActivity/rapoarte:
    @Query("SELECT SUM(amount) FROM Income")
    Double getTotalAmount();

    @Query("SELECT SUM(amount) FROM Income WHERE categoryType = :type")
    Double getTotalBySourceType(String type); // ex. "FIRMA" sau "PERSONAL"

    @Query("SELECT SUM(amount) FROM Income WHERE categoryType = :type AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String type, long from, long to);

    // pentru UidBackfill
    @Query("SELECT * FROM Income WHERE (uid IS NULL OR uid = '')")
    List<Income> getMissingUid();
}
