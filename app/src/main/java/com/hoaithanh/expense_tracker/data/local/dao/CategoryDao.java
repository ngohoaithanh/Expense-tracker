package com.hoaithanh.expense_tracker.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hoaithanh.expense_tracker.data.local.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories")
    List<Category> getAllCategories();

    @Insert
    void insert(Category category);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();
}
