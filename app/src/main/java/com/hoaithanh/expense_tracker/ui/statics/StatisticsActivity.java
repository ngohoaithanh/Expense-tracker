package com.hoaithanh.expense_tracker.ui.statics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.model.CategorySum;
import com.hoaithanh.expense_tracker.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {
    private PieChart pieChart;
    private TextView tvTotalIncome, tvTotalExpense, tvMonthYear;
    private TabLayout tabLayout;
    private AppDatabase db;
    private long startOfMonth, now;
    private int currentType = 0; // 0: Chi tiêu, 1: Thu nhập

    private Calendar currentCalendar = Calendar.getInstance();
    private int filterType = 1; // 0: Tuần, 1: Tháng, 2: Năm
    private int currentTransactionType = 0; // 0: Chi tiêu, 1: Thu nhập

    // Ánh xạ thêm các nút điều hướng
    private ImageButton btnPrev, btnNext;
    private TextView tvNetBalance;
    private long totalIncome = 0;
    private long totalExpense = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Thống kê chi tiêu"); // Đặt tiêu đề cho trang
        }

        db = AppDatabase.getInstance(this);
        initViews();
        setupTimeRange();
        setupPieChart();
        setupTabLayout();

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(v -> {
            if (filterType == 0) currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            else if (filterType == 1) currentCalendar.add(Calendar.MONTH, -1);
            else currentCalendar.add(Calendar.YEAR, -1);

            updatePeriodData();
        });

        btnNext.setOnClickListener(v -> {
            if (filterType == 0) currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            else if (filterType == 1) currentCalendar.add(Calendar.MONTH, 1);
            else currentCalendar.add(Calendar.YEAR, 1);

            updatePeriodData();
        });


        updatePeriodData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.statistics_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_week) filterType = 0;
        else if (id == R.id.menu_month) filterType = 1;
        else if (id == R.id.menu_year) filterType = 2;
        else if (id == android.R.id.home) { finish(); return true; }

        updatePeriodData(); // Cập nhật lại giao diện ngay lập tức
        return super.onOptionsItemSelected(item);
    }

    private void updatePeriodData() {
        Calendar cal = (Calendar) currentCalendar.clone();
        long start, end;

        if (filterType == 0) { // TUẦN
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            start = cal.getTimeInMillis();

            cal.add(Calendar.DAY_OF_WEEK, 6);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            end = cal.getTimeInMillis();

            tvMonthYear.setText("Tuần " + cal.get(Calendar.WEEK_OF_YEAR) + " - " + cal.get(Calendar.YEAR));
        } else if (filterType == 1) { // THÁNG
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            start = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            end = cal.getTimeInMillis();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            tvMonthYear.setText(sdf.format(currentCalendar.getTime()));
        } else { // NĂM
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            start = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            end = cal.getTimeInMillis();

            tvMonthYear.setText("Năm " + cal.get(Calendar.YEAR));
        }

        // GỌI 2 HÀM NÀY ĐỂ CẬP NHẬT DỮ LIỆU MỚI
        refreshStatistics(start, end);
    }

    // Hàm trung gian để gọi cả Summary và Chart cùng lúc
    private void refreshStatistics(long start, long end) {
        loadSummaryData(start, end);
        loadChartData(currentType, start, end);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Đóng Activity
        // Hiệu ứng: Màn hình cũ mờ đi, màn hình Home trượt từ trái sang
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
        return true;
    }

    private void initViews() {
        pieChart = findViewById(R.id.pieChart);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tabLayout = findViewById(R.id.tabLayout);
        tvNetBalance = findViewById(R.id.tvNetBalance);
    }

    private void setupTimeRange() {
        Calendar calendar = Calendar.getInstance();
        // Set tiêu đề tháng hiện tại
        tvMonthYear.setText("Tháng " + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR));

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        startOfMonth = calendar.getTimeInMillis();
        now = System.currentTimeMillis();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                // Truyền lại startOfMonth và now đang lưu trữ
                loadChartData(currentType, startOfMonth, now);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupPieChart() {
        // Tắt description mặc định
        pieChart.getDescription().setEnabled(false);

        // Hiển thị %
        pieChart.setUsePercentValues(true);

        // Animation mượt
        pieChart.animateY(1200, Easing.EaseInOutQuad);

        // Donut chart
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(Color.WHITE);

        // Text ở giữa
        pieChart.setCenterText("Chi tiêu\nTháng này");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.BLACK);

        // Tương tác
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Label trên miếng bánh
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);

        // Offset cho thoáng
        pieChart.setExtraOffsets(10, 10, 10, 10);

        // Legend (chú thích)
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Listener khi click
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e == null) return;
                pieChart.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                PieEntry pe = (PieEntry) e;
                String category = pe.getLabel();
                double value = pe.getValue();

                pieChart.setCenterText(category + "\n" + CurrencyUtils.formatVND(value));
                pieChart.setCenterTextColor(Color.parseColor("#1976D2"));
            }

            @Override
            public void onNothingSelected() {
                pieChart.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                pieChart.setCenterText("Chi tiêu\nTháng này");
                pieChart.setCenterTextColor(Color.BLACK);
            }
        });
    }

private void loadSummaryData(long start, long end) {
    // 1. Lấy tổng Thu
    db.expenseDao().getTotalByType(1, start, end).observe(this, income -> {
//        totalIncome = (income != null) ? income : 0;
        totalIncome = (income != null) ? income.longValue() : 0;
        tvTotalIncome.setText(CurrencyUtils.formatVND(totalIncome));
        calculateNetBalance(); // Tính lại số dư mỗi khi data thay đổi
    });

    // 2. Lấy tổng Chi
    db.expenseDao().getTotalByType(0, start, end).observe(this, expense -> {
//        totalExpense = (expense != null) ? expense : 0;
        totalExpense = (expense != null) ? expense.longValue() : 0;
        tvTotalExpense.setText(CurrencyUtils.formatVND(totalExpense));
        calculateNetBalance(); // Tính lại số dư mỗi khi data thay đổi
    });
}

    private void calculateNetBalance() {
        long net = totalIncome - totalExpense;
        tvNetBalance.setText(CurrencyUtils.formatVND(net));

        if (net > 0) {
            tvNetBalance.setTextColor(Color.parseColor("#2E7D32")); // Màu xanh (Dương)
            tvNetBalance.setText("+" + CurrencyUtils.formatVND(net));
        } else if (net < 0) {
            tvNetBalance.setTextColor(Color.parseColor("#C62828")); // Màu đỏ (Âm)
        } else {
            tvNetBalance.setTextColor(Color.GRAY); // Bằng 0
        }
    }

    private void loadChartData(int type, long start, long end) {
        // Lưu lại thời gian hiện tại để khi chuyển Tab (Thu/Chi) nó vẫn load đúng mốc thời gian này
        this.startOfMonth = start;
        this.now = end;

        pieChart.setCenterText(type == 0 ? "Chi tiêu" : "Thu nhập");

        db.expenseDao().getDataByCategory(type, start, end).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                pieChart.clear();
                pieChart.setNoDataText("Không có dữ liệu trong khoảng thời gian này");
                return;
            }

            ArrayList<PieEntry> entries = new ArrayList<>();
            for (CategorySum item : list) {
                entries.add(new PieEntry((float) item.total, item.category));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            ArrayList<Integer> colors = new ArrayList<>();
            if (type == 1) {
                for (int c : ColorTemplate.LIBERTY_COLORS) colors.add(c);
            } else {
                for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
            }
            dataSet.setColors(colors);

            PieData data = new PieData(dataSet);
            data.setValueTextSize(11f);
            data.setValueTextColor(Color.BLACK);
            data.setValueFormatter(new PercentFormatter(pieChart));

            pieChart.setData(data);
            pieChart.animateY(1000, Easing.EaseInOutQuad);
            pieChart.invalidate();
        });
    }
}
