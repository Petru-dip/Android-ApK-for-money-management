package com.example.expensetracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String description;
    public double amount;
    public long date;
    public String category;
}
