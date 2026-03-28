package com.hoaithanh.expense_tracker.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hoaithanh.expense_tracker.data.local.dao.CategoryDao;
import com.hoaithanh.expense_tracker.data.local.dao.ExpenseDao;
import com.hoaithanh.expense_tracker.data.local.entity.Category;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;

// 1. Tăng version lên 2 vì ta thêm Table mới
// 2. Thêm class Category vào mảng entities
@Database(entities = {Expense.class, Category.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // 3. Khai báo hàm trừu tượng để lấy Dao
    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "expense_db")
                    .fallbackToDestructiveMigration() // Tự động xóa data cũ để nâng cấp cấu trúc
                    .build();
        }
        return instance;
    }
}
