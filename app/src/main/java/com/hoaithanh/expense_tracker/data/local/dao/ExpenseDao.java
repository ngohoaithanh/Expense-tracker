package com.hoaithanh.expense_tracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.hoaithanh.expense_tracker.data.local.entity.Expense;

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
}
