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


    @Query("SELECT * FROM income ORDER BY date DESC")
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

    // folosite în MainActivity
    @Query("SELECT SUM(amount) FROM income")
    Double getTotalAmount();

    @Query("SELECT SUM(amount) FROM income WHERE categoryType = :type")
    Double getTotalByCategoryType(String type);

    // folosite de IncomeReportActivity
    @Query("SELECT SUM(amount) FROM income WHERE categoryType = :type AND date BETWEEN :from AND :to")
    Double getTotalByTypeAndDate(String type, long from, long to);


    // folosit de UidBackfill
    @Query("SELECT * FROM income WHERE uid IS NULL OR uid = ''")
    List<Income> getMissingUid();




}
