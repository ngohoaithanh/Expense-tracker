package com.hoaithanh.expense_tracker.ui.home;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.model.ListItem;
import com.hoaithanh.expense_tracker.ui.add.AddExpenseActivity;
import com.hoaithanh.expense_tracker.ui.statics.StatisticsActivity;
import com.hoaithanh.expense_tracker.utils.DateUtils;

import java.text.DecimalFormat;
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
    private TextView tvBalance, tvTotalIncome, tvTotalExpense;
    NestedScrollView nestedScrollView;

    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbar;
    private ShimmerFrameLayout shimmerHome;
    private boolean isDataLoaded = false;
    private TextView tvCurrentMonthName, tvMonthSummary;
    private MaterialButton btnPreviousMonth, btnNextMonth;
    private Calendar currentViewCalendar = Calendar.getInstance(); // Biến theo dõi tháng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ View
        initViews();
        shimmerHome.startShimmer();

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // Kiểm tra hướng cuộn
                if (scrollY > oldScrollY + 12 && fabAdd.isExtended()) {
                    // Đang cuộn xuống -> Thu nhỏ nút (chỉ còn icon)
                    fabAdd.shrink();
                } else if (scrollY < oldScrollY - 12 && !fabAdd.isExtended()) {
                    // Đang cuộn lên -> Mở rộng nút (hiện chữ "Thêm mới")
                    fabAdd.extend();
                }

                // Nếu quay về đầu trang -> Luôn mở rộng nút
                if (scrollY == 0) {
                    fabAdd.extend();
                }

                float alpha = Math.min(1.0f, (float) scrollY / 200.0f);

                // Đổi màu nền của AppBarLayout (ví dụ sang màu trắng hoặc màu Surface)
                int color = getResources().getColor(R.color.surface); // Hoặc màu trắng
                appBarLayout.setBackgroundColor(interpolateColor(Color.TRANSPARENT, color, alpha));

                // Thêm bóng đổ (Elevation) khi cuộn xuống
                if (scrollY > 0) {
                    appBarLayout.setElevation(4f);
                } else {
                    appBarLayout.setElevation(0f);
                }
            }
        });

        // Nút Tháng sau (Next)
        btnNextMonth.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            currentViewCalendar.add(Calendar.MONTH, 1);
            runSlideAnimation(true); // Tham số true để trượt sang trái
        });

        // Nút Tháng trước (Previous)
        btnPreviousMonth.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            currentViewCalendar.add(Calendar.MONTH, -1);
            runSlideAnimation(false); // Tham số false để trượt sang phải
        });

        tvCurrentMonthName.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showMonthYearPicker();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentViewCalendar = Calendar.getInstance();
        observeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_statistics) {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            // Mở màn hình thống kê (Activity mới)
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);

            // Hiệu ứng chuyển cảnh mượt mà
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int interpolateColor(int colorStart, int colorEnd, float fraction) {
        float[] startHsv = new float[3];
        float[] endHsv = new float[3];
        Color.colorToHSV(colorStart, startHsv);
        Color.colorToHSV(colorEnd, endHsv);
        for (int i = 0; i < 3; i++) {
            endHsv[i] = startHsv[i] + (endHsv[i] - startHsv[i]) * fraction;
        }
        return Color.HSVToColor((int)(fraction * 255), endHsv);
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

        tvBalance = findViewById(R.id.tvBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);

        nestedScrollView = findViewById(R.id.main_scrollview);
        appBarLayout = findViewById(R.id.appBarLayout);
        toolbar = findViewById(R.id.toolbar);
        shimmerHome = findViewById(R.id.shimmerHome);

        tvCurrentMonthName = findViewById(R.id.tvCurrentMonthName);
        // Nếu bạn có thêm dòng text nhỏ "X giao dịch" thì ánh xạ luôn:
        tvMonthSummary = findViewById(R.id.tvMonthSummary);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
    }

    private void runSlideAnimation(boolean isNext) {
        // 1. Khai báo hiệu ứng biến mất và xuất hiện
        Animation outAnim = AnimationUtils.loadAnimation(this, isNext ? R.anim.slide_out_left : R.anim.slide_out_right);
        Animation inAnim = AnimationUtils.loadAnimation(this, isNext ? R.anim.slide_in_right : R.anim.slide_in_left);
        observeData();
        // 2. Chạy hiệu ứng biến mất cho dữ liệu cũ
        nestedScrollView.startAnimation(outAnim);

        outAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Khi cái cũ đã biến mất -> Tải dữ liệu tháng mới

                // Chạy hiệu ứng xuất hiện cho dữ liệu mới
                nestedScrollView.startAnimation(inAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void observeData() {
        long start = DateUtils.getStartOfMonth(currentViewCalendar);
        long end = DateUtils.getEndOfMonth(currentViewCalendar);

        // 1. Hiển thị tên tháng
        SimpleDateFormat sdf = new SimpleDateFormat("'Tháng 'MM, yyyy", new Locale("vi", "VN"));
        tvCurrentMonthName.setText(sdf.format(currentViewCalendar.getTime()));

        // 2. Gỡ bỏ observer cũ - Lưu ý: dùng đúng instance LiveData cũ nếu có thể,
        // hoặc đơn giản nhất là removeObservers trên chính câu query đó.
        db.expenseDao().getExpensesByMonth(start, end).removeObservers(this);

        db.expenseDao().getExpensesByMonth(start, end).observe(this, expenses -> {
            // LUÔN LUÔN tính toán lại tổng tiền, kể cả khi danh sách trống (để reset về 0)
            if (expenses != null) {
                int count = expenses.size();
                if (count == 0) {
                    tvMonthSummary.setText("Chưa có giao dịch nào");
                } else {
                    tvMonthSummary.setText(count + " giao dịch trong tháng");
                }
            }
            calculateTotals(expenses != null ? expenses : new ArrayList<>());

            if (expenses != null && !expenses.isEmpty()) {
                List<ListItem> displayList = groupExpensesByDate(expenses);
                adapter.setExpenses(displayList);

                rvRecentExpenses.scheduleLayoutAnimation();
                layoutEmpty.setVisibility(View.GONE);
                rvRecentExpenses.setVisibility(View.VISIBLE);
            } else {
                // Nếu tháng này chưa có gì, xóa sạch danh sách cũ trên Adapter
                adapter.setExpenses(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                rvRecentExpenses.setVisibility(View.GONE);
            }

            if (!isDataLoaded) {
                hideShimmerEffect();
                isDataLoaded = true;
            }
        });
    }

    private void showMonthYearPicker() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_month_year_picker);

        // Làm cho nền Dialog trong suốt để thấy được bo góc của CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Thêm hiệu ứng hiện lên từ từ
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        NumberPicker monthPicker = dialog.findViewById(R.id.monthPicker);
        NumberPicker yearPicker = dialog.findViewById(R.id.yearPicker);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Cấu hình Tháng: Hiện 01, 02 thay vì 1, 2
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setFormatter(i -> String.format("%02d", i));
        monthPicker.setValue(currentViewCalendar.get(Calendar.MONTH) + 1);

        // Cấu hình Năm
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(currentViewCalendar.get(Calendar.YEAR));

        btnConfirm.setOnClickListener(v -> {
            currentViewCalendar.set(Calendar.MONTH, monthPicker.getValue() - 1);
            currentViewCalendar.set(Calendar.YEAR, yearPicker.getValue());

            // Chuyển cảnh mượt mà
            nestedScrollView.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(150).withEndAction(() -> {
                observeData();
                nestedScrollView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();
            }).start();

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void hideShimmerEffect() {
        // Tắt animation và ẩn View Shimmer
        shimmerHome.stopShimmer();
        shimmerHome.setVisibility(View.GONE);

        // Hiện layout chính và dùng hiệu ứng Fade-in cho mượt
        nestedScrollView.setVisibility(View.VISIBLE);
        nestedScrollView.setAlpha(0f);
        nestedScrollView.animate()
                .alpha(1f)
                .setDuration(400) // 0.4 giây để hiện dần lên
                .start();
    }

    private List<ListItem> groupExpensesByDate(List<Expense> expenseList) {
        List<ListItem> groupedList = new ArrayList<>();

        // 1. Tính tổng tiền cho từng ngày bằng Map
        Map<String, Long> dateTotals = new HashMap<>();
        SimpleDateFormat compareFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (Expense e : expenseList) {
            String key = compareFmt.format(new Date(e.timestamp));
            long currentNet = dateTotals.getOrDefault(key, 0L);

            if (e.type == 1) currentNet += (long)e.amount; // Thu thì cộng
            else currentNet -= (long)e.amount; // Chi thì trừ

            dateTotals.put(key, currentNet);
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
        double totalIncome = 0;
        double totalExpense = 0;
        double todayNet = 0; // Để hiện ở dòng "Hôm nay: ..."
        double monthNet = 0; // Để hiện ở dòng "Tháng này: ..."

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        for (Expense e : expenses) {
            cal.setTimeInMillis(e.timestamp);

            // 1. Tính Tổng Thu và Tổng Chi (Toàn bộ thời gian)
            if (e.type == 1) {
                totalIncome += e.amount;
            } else {
                totalExpense += e.amount;
            }

            // 2. Tính toán cho Hôm nay và Tháng này (Tính số dư Net)
            if (cal.get(Calendar.YEAR) == currentYear) {
                if (cal.get(Calendar.DAY_OF_YEAR) == currentDay) {
                    if (e.type == 1) todayNet += e.amount; else todayNet -= e.amount;
                }
                if (cal.get(Calendar.MONTH) == currentMonth) {
                    if (e.type == 1) monthNet += e.amount; else monthNet -= e.amount;
                }
            }
        }

        // 3. Tính số dư tổng (Balance)
        double balance = totalIncome - totalExpense;

        // 4. Định dạng tiền tệ VND
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Đổ dữ liệu lên Card Summary
        tvBalance.setText(vnFormat.format(balance));
        tvTotalIncome.setText(vnFormat.format(totalIncome));
        tvTotalExpense.setText(vnFormat.format(totalExpense));

        // Cập nhật 2 dòng nhỏ bên dưới
        tvTotalToday.setText("Hôm nay: " + vnFormat.format(todayNet));
        tvTotalMonth.setText("Tháng này: " + vnFormat.format(monthNet));

        // --- SENIOR UX: Đổi màu theo trạng thái tài chính ---

        // Số dư tổng: Đỏ nếu âm, Xanh Primary nếu dương
        tvBalance.setTextColor(balance >= 0 ?
                getResources().getColor(R.color.primary) : Color.RED);

        // Dòng Today/Month: Đỏ nếu chi nhiều hơn thu
        tvTotalToday.setTextColor(todayNet >= 0 ? Color.GRAY : Color.RED);
        tvTotalMonth.setTextColor(monthNet >= 0 ? Color.GRAY : Color.RED);
    }
}