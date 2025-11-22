package com.example.expensetracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface IncomeDao {

    @Query("SELECT SUM(amount) FROM `Income` WHERE categoryType = :type")
    Double getTotalBySourceType(String type);

    @Query("SELECT * FROM income ORDER BY date DESC, id DESC")
    List<Income> getAll();

    @Query("SELECT * FROM income WHERE id = :id LIMIT 1")
    Income getById(int id);

    @Query("SELECT * FROM income WHERE uid = :uid LIMIT 1")
    Income getByUid(String uid);

    @Insert
    void insert(Income e);

    @Update
    void update(Income e);

    @Delete
    void delete(Income e);

    @Query("SELECT SUM(amount) FROM income")
    Double getTotalAmount();

    @Query("SELECT SUM(amount) FROM income WHERE date BETWEEN :from AND :to")
    Double getTotalAmountByDate(long from, long to);

    @Query("SELECT SUM(amount) FROM income WHERE categoryType = :type")
    Double getTotalByCategoryType(String type);

    @Query("SELECT SUM(amount) FROM income WHERE categoryType = :type AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String type, long from, long to);

    @Query("SELECT * FROM income WHERE uid IS NULL OR uid = ''")
    List<Income> getMissingUid();

    @Query("SELECT * FROM income WHERE date BETWEEN :from AND :to " +
            "AND (:category IS NULL OR LENGTH(:category)=0 OR LOWER(category) LIKE '%' || LOWER(:category) || '%') " +
            "AND (:categoryType IS NULL OR LENGTH(:categoryType)=0 OR categoryType = :categoryType) " +
            "ORDER BY date DESC, id DESC")
    List<Income> getByRangeAndCategory(long from, long to, String category, String categoryType);

    @Query("DELETE FROM income WHERE date BETWEEN :from AND :to " +
            "AND (:category IS NULL OR LENGTH(:category)=0 OR LOWER(category) LIKE '%' || LOWER(:category) || '%') " +
            "AND (:categoryType IS NULL OR LENGTH(:categoryType)=0 OR categoryType = :categoryType)")
    int deleteByRangeAndCategory(long from, long to, String category, String categoryType);
}
