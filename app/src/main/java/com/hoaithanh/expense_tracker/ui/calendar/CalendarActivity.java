package com.hoaithanh.expense_tracker.ui.calendar;

import android.os.Bundle;
import android.widget.CalendarView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.ui.home.HomeAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {
    private HomeAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = AppDatabase.getDatabase(this);
        adapter = new HomeAdapter();

        RecyclerView rv = findViewById(R.id.rvDailyExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Convert ngày được chọn sang timestamp để query
            loadExpensesForDate(year, month, dayOfMonth);
        });

        // Load dữ liệu ngày hiện tại khi vừa vào màn hình
        Calendar c = Calendar.getInstance();
        loadExpensesForDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    private void loadExpensesForDate(int year, int month, int day) {
        // Logic lọc dữ liệu từ List tổng (Hoặc dùng DAO query theo range thời gian)
        db.expenseDao().getAllExpenses().observe(this, allExpenses -> {
            List<Expense> filtered = new ArrayList<>();
            Calendar target = Calendar.getInstance();
            target.set(year, month, day);

            for (Expense e : allExpenses) {
                Calendar current = Calendar.getInstance();
                current.setTimeInMillis(e.timestamp);
                if (current.get(Calendar.YEAR) == year &&
                        current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
                    filtered.add(e);
                }
            }
            adapter.setExpenses(filtered);
        });
    }
}