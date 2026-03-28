package com.hoaithanh.expense_tracker.ui.detail;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {
    private Expense currentExpense;
    private int expenseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 1. Lấy ID được gửi từ Adapter (mặc định là -1 nếu lỗi)
        expenseId = getIntent().getIntExtra("EXPENSE_ID", -1);

        // 2. Gọi hàm load dữ liệu (Hàm này chúng ta sẽ viết ngay dưới đây)
        loadData(expenseId);

        // Setup Toolbar để có nút Back
        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Memory Detail");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // ĐÂY LÀ HÀM BẠN ĐANG THIẾU:
    private void loadData(int id) {
        if (id == -1) {
            Toast.makeText(this, "Không tìm thấy chi tiêu này!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ các View từ XML
        ImageView ivFullPhoto = findViewById(R.id.ivFullPhoto);
        TextView tvAmount = findViewById(R.id.tvDetailAmount);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDate = findViewById(R.id.tvDetailDate);
        Chip chipCategory = findViewById(R.id.chipDetailCategory);

        // Truy vấn từ Database thông qua LiveData
        AppDatabase.getInstance(this).expenseDao().getExpenseById(id).observe(this, expense -> {
            if (expense != null) {
                currentExpense = expense; // Gán vào biến toàn cục để dùng khi Xóa

                // Hiển thị dữ liệu lên màn hình
                tvAmount.setText(String.format("$%.2f", expense.amount));
                tvTitle.setText(expense.title.isEmpty() ? "No Title" : expense.title);
                chipCategory.setText(expense.category);

                // Định dạng ngày tháng cho dễ đọc
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(expense.timestamp)));

                if (expense.imagePath != null && !expense.imagePath.isEmpty()) {

                    // Senior Tip: Kiểm tra nếu là ảnh mặc định (Skip Photo)
                    if (expense.imagePath.equals("default_placeholder")) {
                        ivFullPhoto.setImageResource(R.drawable.bg_placeholder_expense);
                        ivFullPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } else {
                        // Load ảnh từ đường dẫn vật lý (Internal Storage)
                        Glide.with(this)
                                .load(expense.imagePath) // Glide tự hiểu đường dẫn file vật lý
                                .placeholder(R.drawable.ic_camera) // Hiện màu gradient trong lúc chờ
                                .error(R.drawable.error_image) // Hiện ảnh lỗi nếu file bị xóa
                                .centerInside() // Giữ nguyên tỉ lệ ảnh để xem cho rõ
                                .into(ivFullPhoto);
                    }
                } else {
                    // Trường hợp không có ảnh
                    ivFullPhoto.setImageResource(R.drawable.bg_placeholder_expense);
                }
            }
        });
    }

    // Xử lý nút Xóa trên Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa kỷ niệm này?")
                .setMessage("Ảnh và dữ liệu sẽ bị xóa vĩnh viễn khỏi máy.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteRecord();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteRecord() {
        if (currentExpense == null) return;

        new Thread(() -> {
            // 1. Xóa file vật lý trước
            File file = new File(currentExpense.imagePath);
            if (file.exists()) file.delete();

            // 2. Xóa trong DB
            AppDatabase.getInstance(this).expenseDao().delete(currentExpense);

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                finish(); // Quay lại màn hình trước
            });
        }).start();
    }
}
