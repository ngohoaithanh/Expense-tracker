package com.hoaithanh.expense_tracker.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hoaithanh.expense_tracker.data.local.dao.ExpenseDao;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;

@Database(entities = {Expense.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ExpenseDao expenseDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_db")
                            .fallbackToDestructiveMigration() // Xóa data cũ nếu đổi version DB (chỉ dùng khi dev)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
