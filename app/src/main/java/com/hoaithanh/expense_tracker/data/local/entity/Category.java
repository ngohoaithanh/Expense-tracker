package com.hoaithanh.expense_tracker.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;


    @ColumnInfo(name = "name")
    public String name;

    public int type;
    public Category(String name, int type) {
        this.name = name;
        this.type = type;
    }
}
