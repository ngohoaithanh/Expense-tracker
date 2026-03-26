package com.hoaithanh.expense_tracker.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.ui.add.AddExpenseActivity;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvRecentExpenses;
    private HomeAdapter adapter;
    private TextView tvTotalToday, tvTotalMonth;
    private View layoutEmpty;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ View
        initViews();

        // 2. Cấu hình RecyclerView
        adapter = new HomeAdapter();
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvRecentExpenses.setAdapter(adapter);

        // 3. Khởi tạo Database
        db = AppDatabase.getDatabase(this);

        // 4. Quan sát dữ liệu (Observer Pattern)
        observeData();

        // 5. Sự kiện bấm nút Chụp ảnh
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses);
        tvTotalToday = findViewById(R.id.tvTotalToday);
        tvTotalMonth = findViewById(R.id.tvTotalMonth);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void observeData() {
        // Lấy tất cả chi tiêu và tự động cập nhật UI khi có thay đổi
        db.expenseDao().getAllExpenses().observe(this, expenses -> {
            if (expenses != null && !expenses.isEmpty()) {
                adapter.setExpenses(expenses);
                layoutEmpty.setVisibility(View.GONE);
                rvRecentExpenses.setVisibility(View.VISIBLE);
                calculateTotals(expenses);
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvRecentExpenses.setVisibility(View.GONE);
                tvTotalToday.setText("$0.00");
            }
        });
    }

    private void calculateTotals(List<Expense> expenses) {
        double todaySum = 0;
        double monthSum = 0;

        long now = System.currentTimeMillis();
        // Senior Tip: Sử dụng Calendar để check ngày/tháng chính xác
        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);
        int currentMonth = cal.get(Calendar.MONTH);

        for (Expense e : expenses) {
            cal.setTimeInMillis(e.timestamp);
            if (cal.get(Calendar.DAY_OF_YEAR) == currentDay) {
                todaySum += e.amount;
            }
            if (cal.get(Calendar.MONTH) == currentMonth) {
                monthSum += e.amount;
            }
        }

        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalToday.setText(vnFormat.format(todaySum));
        tvTotalMonth.setText(vnFormat.format(monthSum));

    }
}