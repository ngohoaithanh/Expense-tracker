package com.hoaithanh.expense_tracker.ui.add;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {
    private String currentPhotoPath;
    private ImageView ivPreview;
    private EditText etAmount, etTitle;
    private ChipGroup chipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        ivPreview = findViewById(R.id.ivPreview);
        etAmount = findViewById(R.id.etAmount);
        etTitle = findViewById(R.id.etTitle);
        chipGroup = findViewById(R.id.chipGroupCategory);

        // 1. Mở camera ngay khi vào màn hình
        dispatchTakePictureIntent();
        if (savedInstanceState == null && currentPhotoPath == null) {
            checkCameraPermission();
        }
        findViewById(R.id.btnSave).setOnClickListener(v -> saveExpense());

        // 2. Format money
        etAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final Locale vietnam = new Locale("vi", "VN");

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    // Xóa bỏ listener tạm thời để tránh vòng lặp vô tận
                    etAmount.removeTextChangedListener(this);

                    // Làm sạch chuỗi (chỉ giữ lại số)
                    String cleanString = s.toString().replaceAll("[^0-9]", "");

                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);

                        NumberFormat formatter = NumberFormat.getCurrencyInstance(vietnam);
                        String formatted = formatter.format(parsed);

                        current = formatted;
                        etAmount.setText(formatted);
                        etAmount.setSelection(formatted.length());
                    }

                    etAmount.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Nếu chưa có quyền, hệ thống sẽ hiện hộp thoại xin quyền
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            // Nếu đã có quyền rồi, mở camera luôn
            dispatchTakePictureIntent();
        }
    }

    // Hàm này sẽ chạy khi người dùng bấm "Cho phép" hoặc "Từ chối" trên hộp thoại
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Người dùng đã cho phép!
                dispatchTakePictureIntent();
            } else {
                // Người dùng từ chối
                Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp ảnh chi tiêu!", Toast.LENGTH_LONG).show();
                finish(); // Thoát màn hình vì không có ảnh thì không làm gì được
            }
        }
    }

    // Đăng ký nhận kết quả từ Camera
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    // CHỤP THÀNH CÔNG: Hiển thị ảnh lên ivPreview
                    Glide.with(this).load(currentPhotoPath).into(ivPreview);

                    // Senior Tip: Ép hệ thống quét file ảnh mới để tránh bị mất ảnh trong Gallery máy
                    MediaScannerConnection.scanFile(this, new String[]{currentPhotoPath}, null, null);
                } else {
                    // CHỤP THẤT BẠI HOẶC BẤM BACK: Thoát luôn để không bị lặp
                    Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = ImageUtils.createImageFile(this);
            currentPhotoPath = photoFile.getAbsolutePath();
            Uri photoURI = ImageUtils.getUriForFile(this, photoFile);
            takePictureLauncher.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) return;

        // Xóa tất cả ký tự không phải là số (Regex: [^0-9])
        String cleanAmount = amountStr.replaceAll("[^0-9]", "");
        double amount = Double.parseDouble(cleanAmount);
        String title = etTitle.getText().toString();

        // Cách lấy text của Chip đang chọn cực gọn:
        int checkedChipId = chipGroup.getCheckedChipId();
        String category = "Others"; // Giá trị mặc định

        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = findViewById(checkedChipId);
            category = selectedChip.getText().toString();
        }

        Expense expense = new Expense(title, amount, category,
                System.currentTimeMillis(), currentPhotoPath);

        // Lưu vào Database (chạy trên thread riêng)
        new Thread(() -> {
            AppDatabase.getDatabase(this).expenseDao().insert(expense);
            runOnUiThread(() -> {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                // Senior Tip: Tạo hiệu ứng đóng màn hình mượt mà
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
            });
        }).start();

    }
}
