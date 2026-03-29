package com.hoaithanh.expense_tracker.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.ui.add.AddExpenseActivity;

import java.io.File;
import java.text.NumberFormat;
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
            getSupportActionBar().setTitle("Chi tiết");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        // Khi thoát ra cũng cho mờ dần
//        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//    }

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
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                String formattedAmount = formatter.format(expense.amount);

                tvAmount.setText(formattedAmount);
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
        if (item.getItemId() == R.id.action_edit) {
            // Gửi dữ liệu sang màn hình Add để sửa
            Intent intent = new Intent(this, AddExpenseActivity.class);
            intent.putExtra("IS_EDIT", true);
            intent.putExtra("EXPENSE_ID", currentExpense.id); // Quan trọng để biết sửa cái nào
            // Có thể gửi thêm các thông tin khác nếu không muốn load lại DB ở màn hình kia
            startActivity(intent);
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
