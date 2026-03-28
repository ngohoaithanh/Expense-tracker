package com.hoaithanh.expense_tracker.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.model.ListItem;
import com.hoaithanh.expense_tracker.ui.add.AddExpenseActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvRecentExpenses;
    private HomeAdapter adapter;
    private TextView tvTotalToday, tvTotalMonth;
    private View layoutEmpty;
    private AppDatabase db;
    private ExtendedFloatingActionButton fabAdd;

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
        db = AppDatabase.getInstance(this);

        // 4. Quan sát dữ liệu (Observer Pattern)
        observeData();

        fabAdd.setOnClickListener(v -> {
            showAddOptionsSheet();
        });
//        fabAdd.setOnClickListener(v -> {
//            Intent intent = new Intent(HomeActivity.this, AddExpenseActivity.class);
//            // Gửi tín hiệu để AddExpenseActivity tự động bật Camera
//            intent.putExtra("AUTO_CAMERA", true);
//            startActivity(intent);
//        });
//
//        fabAdd.setOnLongClickListener(v -> {
//            Intent intent = new Intent(HomeActivity.this, AddExpenseActivity.class);
//            // Gửi tín hiệu FALSE để không tự bật Camera, cho phép chọn Gallery hoặc Skip
//            intent.putExtra("AUTO_CAMERA", false);
//            startActivity(intent);
//
//            // Rung nhẹ một cái để báo cho người dùng biết đã kích hoạt Long Click
//            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
//            return true;
//        });
    }

    private void showAddOptionsSheet() {
        // 1. Khởi tạo Dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // 2. Nạp Layout XML bạn vừa tạo vào Dialog
        View view = getLayoutInflater().inflate(R.layout.layout_add_options, null);
        bottomSheetDialog.setContentView(view);

        // 3. Ánh xạ các nút bên trong Bottom Sheet và xử lý Click

        // Nút Lựa chọn 1: Chụp ảnh
        view.findViewById(R.id.btnOptionCamera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss(); // Đóng bảng lại
            navigateToAddExpense(true);  // Mở màn hình Add với cờ hiệu Mở Camera
        });

        // Nút Lựa chọn 2: Nhập tay
        view.findViewById(R.id.btnOptionManual).setOnClickListener(v -> {
            bottomSheetDialog.dismiss(); // Đóng bảng lại
            navigateToAddExpense(false); // Mở màn hình Add với cờ hiệu NHẬP TAY
        });

        // 4. Hiển thị bảng lên
        bottomSheetDialog.show();
    }

    private void navigateToAddExpense(boolean isAutoCamera) {
        Intent intent = new Intent(HomeActivity.this, AddExpenseActivity.class);
        // Gửi tín hiệu để AddExpenseActivity biết nên bật Camera hay hiện Form nhập tay
        intent.putExtra("AUTO_CAMERA", isAutoCamera);
        startActivity(intent);
    }

    private void initViews() {
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses);
        tvTotalToday = findViewById(R.id.tvTotalToday);
        tvTotalMonth = findViewById(R.id.tvTotalMonth);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void observeData() {
        // Lấy tất cả chi tiêu và tự động cập nhật UI khi có thay đổi
        db.expenseDao().getAllExpenses().observe(this, expenses -> {
            if (expenses != null && !expenses.isEmpty()) {
                // --- BƯỚC QUAN TRỌNG: Chuyển đổi sang danh sách có nhóm ngày ---
                List<ListItem> displayList = groupExpensesByDate(expenses);

                // Cập nhật Adapter với danh sách ListItem mới
                adapter.setExpenses(displayList);

                layoutEmpty.setVisibility(View.GONE);
                rvRecentExpenses.setVisibility(View.VISIBLE);

                // Vẫn dùng danh sách gốc (expenses) để tính tổng tiền
                calculateTotals(expenses);
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvRecentExpenses.setVisibility(View.GONE);

                // Format tiền theo Locale VN cho đồng bộ
                tvTotalToday.setText("0 ₫");
            }
        });
    }

    private List<ListItem> groupExpensesByDate(List<Expense> expenseList) {
        List<ListItem> groupedList = new ArrayList<>();

        // 1. Tính tổng tiền cho từng ngày bằng Map
        Map<String, Long> dateTotals = new HashMap<>();
        SimpleDateFormat compareFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (Expense e : expenseList) {
            String key = compareFmt.format(new Date(e.timestamp));
            long current = dateTotals.getOrDefault(key, 0L);
            dateTotals.put(key, current + (long)e.amount); // Ép kiểu nếu e.amount là double
        }

        // 2. Tạo danh sách ListItem
        String lastDateKey = "";
        SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (Expense expense : expenseList) {
            String dateKey = compareFmt.format(new Date(expense.timestamp));

            if (!dateKey.equals(lastDateKey)) {
                String headerLabel = displayFmt.format(new Date(expense.timestamp));
                long totalForDay = dateTotals.get(dateKey);

                // Tạo Header với cả Ngày và Tổng tiền
                groupedList.add(new ListItem(headerLabel, totalForDay));
                lastDateKey = dateKey;
            }
            groupedList.add(new ListItem(expense));
        }
        return groupedList;
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