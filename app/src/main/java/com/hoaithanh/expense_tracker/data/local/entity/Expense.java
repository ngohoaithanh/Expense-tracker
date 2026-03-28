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
    public int type; // 0: Expense, 1: Income

    public Expense(String title, double amount, long timestamp, String category, String imagePath, int type) {
        this.title = title;
        this.amount = amount;
        this.timestamp = timestamp;
        this.category = category;
        this.imagePath = imagePath;
        this.type = type;
    }
}
