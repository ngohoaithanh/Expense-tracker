package com.hoaithanh.expense_tracker.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public double amount;
    public String category;
    public long timestamp;
    public String imagePath; // Lưu path: /storage/emulated/0/.../img_123.jpg

    public Expense(String title, double amount, String category, long timestamp, String imagePath) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
    }
}
