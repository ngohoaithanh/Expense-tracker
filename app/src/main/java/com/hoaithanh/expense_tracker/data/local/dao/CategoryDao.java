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

    @Query("SELECT * FROM categories WHERE type = :type")
    List<Category> getCategoriesByType(int type);

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    void deleteCategoryByName(String name, int type);

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName AND type = :type")
    void updateCategory(String oldName, String newName, int type);
}
