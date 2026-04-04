package com.hoaithanh.expense_tracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.model.CategorySum;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startOfDay")
    LiveData<Double> getTotalToday(long startOfDay);

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    LiveData<Expense> getExpenseById(int id);

    @Delete
    void delete(Expense expense);

//    @Query("UPDATE expenses SET category = :newName WHERE TRIM(category) = TRIM(:oldName)")
//    void updateExpenseCategory(String oldName, String newName);

    @Query("UPDATE expenses SET category = :newName WHERE TRIM(category) = TRIM(:oldName)")
    int updateExpenseCategory(String oldName, String newName);

    @Update
    void update(Expense expense);

    // 1. Tính tổng số tiền theo loại (0: Chi, 1: Thu) trong một khoảng thời gian
    @Query("SELECT SUM(amount) FROM expenses WHERE type = :type AND timestamp BETWEEN :start AND :end")
    LiveData<Double> getTotalByType(int type, long start, long end);

    // 2. Thống kê chi tiết: Tên danh mục và Tổng tiền của danh mục đó
    @Query("SELECT category, SUM(amount) as total FROM expenses " +
            "WHERE type = 0 AND timestamp BETWEEN :start AND :end " +
            "GROUP BY category ORDER BY total DESC")
    LiveData<List<CategorySum>> getExpenseByCategory(long start, long end);

    @Query("SELECT category, SUM(amount) as total FROM expenses " +
            "WHERE type = :transactionType AND timestamp BETWEEN :start AND :end " +
            "GROUP BY category ORDER BY total DESC")
    LiveData<List<CategorySum>> getDataByCategory(int transactionType, long start, long end);

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    LiveData<List<Expense>> getExpensesByMonth(long start, long end);

}
