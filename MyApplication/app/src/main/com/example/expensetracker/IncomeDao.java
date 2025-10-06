package com.example.expensetracker;

import androidx.room.*;
import java.util.List;

@Dao
public interface IncomeDao {
    @Insert void insert(Income e);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(List<Income> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReplace(List<Income> list);

    @Update void update(Income e);
    @Delete void delete(Income e);

    @Query("DELETE FROM Income WHERE id = :id") void deleteById(int id);
    @Query("SELECT * FROM Income WHERE id = :id LIMIT 1") Income getById(int id);
    @Query("SELECT * FROM Income WHERE uid = :uid LIMIT 1") Income getByUid(String uid);
    @Query("SELECT * FROM Income WHERE uid IS NULL OR uid = ''") List<Income> getMissingUid();

    @Query("SELECT * FROM Income ORDER BY date DESC") List<Income> getAll();
    @Query("SELECT * FROM Income WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<Income> getByDateRange(long from, long to);
    @Query("SELECT * FROM Income WHERE sourceType = :sourceType AND date BETWEEN :from AND :to ORDER BY date DESC")
    List<Income> getByTypeAndDate(String sourceType, long from, long to);
    @Query("SELECT SUM(amount) FROM Income WHERE sourceType = :sourceType AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String sourceType, long from, long to);
    @Query("SELECT SUM(amount) FROM Income WHERE date BETWEEN :from AND :to")
    Double getTotalByDate(long from, long to);
}
