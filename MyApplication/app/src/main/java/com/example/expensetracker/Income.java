package com.example.expensetracker;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Income", indices = {@Index(value = {"uid"}, unique = true)})
public class Income {
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** UID stabil pentru import/export (UUID). */
    @ColumnInfo(name = "uid")
    public String uid;

    public double amount;
    public String description;
    /** Unix millis */
    public long date;
    /** "PERSONAL" sau "FIRMA" */
    public String sourceType;
}
