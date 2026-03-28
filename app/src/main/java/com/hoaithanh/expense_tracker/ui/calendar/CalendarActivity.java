package com.hoaithanh.expense_tracker.ui.calendar;

import android.os.Bundle;
import android.widget.CalendarView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.model.ListItem;
import com.hoaithanh.expense_tracker.ui.home.HomeAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private HomeAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = AppDatabase.getInstance(this);
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
        db.expenseDao().getAllExpenses().observe(this, allExpenses -> {
            List<Expense> filtered = new ArrayList<>();
            long dayTotal = 0; // Biến để cộng dồn tổng tiền của ngày được chọn

            Calendar target = Calendar.getInstance();
            target.set(year, month, day);

            for (Expense e : allExpenses) {
                Calendar current = Calendar.getInstance();
                current.setTimeInMillis(e.timestamp);

                // Kiểm tra xem chi tiêu có thuộc ngày/tháng/năm đang chọn không
                if (current.get(Calendar.YEAR) == year &&
                        current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
                    filtered.add(e);
                    dayTotal += (long) e.amount; // Cộng dồn vào tổng chi tiêu trong ngày
                }
            }

            // Chuyển đổi sang List<ListItem> để khớp với HomeAdapter mới
            List<ListItem> displayList = new ArrayList<>();

            if (!filtered.isEmpty()) {
                // 1. Định dạng ngày tháng (28/03/2026)
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String headerLabel = sdf.format(target.getTime());

                // 2. Thêm Header chứa cả Ngày và Tổng tiền của ngày đó
                // (Sử dụng Constructor mới: ListItem(String label, long total))
                displayList.add(new ListItem(headerLabel, dayTotal));

                // 3. Thêm danh sách các chi tiêu của ngày đó
                for (Expense e : filtered) {
                    displayList.add(new ListItem(e));
                }
            }

            // Đẩy dữ liệu đã xử lý vào adapter
            adapter.setExpenses(displayList);
        });
    }
}