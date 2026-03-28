package com.hoaithanh.expense_tracker.ui.add;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Category;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {
    private String currentPhotoPath;
    private ImageView ivPreview;
    private EditText etAmount, etTitle;
    private ChipGroup chipGroup;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);
        database = AppDatabase.getInstance(this);
        ivPreview = findViewById(R.id.ivPreview);
        etAmount = findViewById(R.id.etAmount);
        etTitle = findViewById(R.id.etTitle);
        chipGroup = findViewById(R.id.chipGroupCategory);
        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            // Kiểm tra xem người dùng đã nhập dữ liệu gì chưa (Tư duy UX)
            if (hasUnsavedData()) {
                showExitConfirmation();
            } else {
                finish(); // Đóng Activity và quay lại màn hình trước
            }
        });

        setupCategoryChips();

        // Nhận tín hiệu từ Home: Có ép mở Camera không?
        boolean autoCamera = getIntent().getBooleanExtra("AUTO_CAMERA", true);
        if (autoCamera && savedInstanceState == null && currentPhotoPath == null) {
            checkCameraPermission(); // Hàm này bên trong sẽ gọi dispatchTakePictureIntent()
        } else {
            // Nếu là nhập tay, hiện ảnh placeholder để UI không bị trống
            ivPreview.setImageResource(R.drawable.bg_placeholder_expense2);
        }

        if (autoCamera && savedInstanceState == null && currentPhotoPath == null) {
            checkCameraPermission();
        } else {
            // Nếu Long Click (autoCamera = false), hiện ảnh mặc định
            ivPreview.setImageResource(R.drawable.bg_placeholder_expense);
        }
        // Nút chọn từ Gallery (Thêm vào XML ở bước sau)
        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
        // Nút bỏ qua ảnh
        findViewById(R.id.btnSkipPhoto).setOnClickListener(v -> {
            currentPhotoPath = "default_placeholder"; // Đánh dấu dùng ảnh mặc định
            ivPreview.setImageResource(R.drawable.bg_placeholder_expense2); // Một ảnh gradient đẹp
            Toast.makeText(this, "Đã bỏ qua chụp ảnh", Toast.LENGTH_SHORT).show();
        });
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

    private String saveImageToInternalStorage(Uri uri) {
        try {
            // Tạo tên file duy nhất
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return file.getAbsolutePath(); // Trả về đường dẫn vật lý: /storage/emulated/0/...
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Kiểm tra xem có dữ liệu chưa lưu không
    private boolean hasUnsavedData() {
        String amount = etAmount.getText().toString().trim();
        // Nếu đã nhập tiền HOẶC đã có ảnh path thì coi là có dữ liệu
        return !amount.isEmpty() || currentPhotoPath != null;
    }

    // Hiển thị thông báo xác nhận thoát
    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy bỏ thay đổi?")
                .setMessage("Dữ liệu bạn vừa nhập sẽ không được lưu. Bạn có chắc muốn quay lại?")
                .setPositiveButton("Thoát", (dialog, which) -> finish())
                .setNegativeButton("Ở lại", null)
                .show();
    }

    private void setupCategoryChips() {
        chipGroup.removeAllViews();

        // Chạy trong Background Thread (vì Room không cho chạy Main Thread)
        new Thread(() -> {
            // 1. Kiểm tra Database có Category chưa
            List<Category> list = database.categoryDao().getAllCategories();

            if (list.isEmpty()) {
                // Nếu trống, nạp mặc định từ strings.xml vào DB
                String[] defaults = getResources().getStringArray(R.array.default_categories);
                for (String s : defaults) {
                    database.categoryDao().insert(new Category(s));
                }
                list = database.categoryDao().getAllCategories();
            }

            // 2. Quay lại UI Thread để vẽ Chip
            List<Category> finalList = list;
            runOnUiThread(() -> {
                for (Category cat : finalList) {
                    createChip(cat.name, false);
                }
                createAddButtonChip(); // Luôn có nút + ở cuối
            });
        }).start();
    }

    private void createChip(String text, boolean isSelected) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);

        // Style Material 3: Bo tròn và màu sắc thân thiện
        chip.setChipCornerRadius(20f);
        chip.setChecked(isSelected);

        // Thêm vào ChipGroup
        chipGroup.addView(chip);
    }

    private void createAddButtonChip() {
        Chip addChip = new Chip(this);
        addChip.setText("Thêm");
        addChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_add)); // Nếu bạn có icon add
        addChip.setChipBackgroundColorResource(R.color.secondary_container);

        addChip.setOnClickListener(v -> {
            // Kiểm tra giới hạn 20 chip trước khi cho phép thêm
            if (chipGroup.getChildCount() > 20) {
                Toast.makeText(this, "Tối đa 20 danh mục thôi nhé!", Toast.LENGTH_SHORT).show();
            } else {
                showAddCategoryDialog();
            }
        });

        chipGroup.addView(addChip);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm danh mục mới");

        // Tạo một EditText để người dùng nhập tên
        final EditText input = new EditText(this);
        input.setHint("Ví dụ: 🐱 Thú cưng");
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newCatName = input.getText().toString().trim();
            if (!newCatName.isEmpty()) {
                new Thread(() -> {
                    // 1. Lưu vào Database vật lý
                    database.categoryDao().insert(new Category(newCatName));

                    // 2. Cập nhật lại giao diện
                    runOnUiThread(() -> {
                        // Chèn vào trước nút "+"
                        createChipAt(newCatName, chipGroup.getChildCount() - 1);
                    });
                }).start();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Hàm hỗ trợ chèn chip vào vị trí cụ thể
    private void createChipAt(String text, int index) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(true); // Tự động chọn luôn danh mục vừa tạo
        chipGroup.addView(chip, index);
    }

    // 1. Thêm Launcher để chọn ảnh từ Gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Lưu Uri vào biến đường dẫn
//                    currentPhotoPath = uri.toString();

                    String internalPath = saveImageToInternalStorage(uri);
                    if (internalPath != null) {
                        // 2. Cập nhật biến đường dẫn để tí nữa lưu Database
                        currentPhotoPath = internalPath;

                        // 3. Hiển thị ảnh lên giao diện cho người dùng xem
                        Glide.with(this)
                                .load(internalPath)
                                .centerCrop()
                                .into(ivPreview);
                        Toast.makeText(this, "Đã chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                    }

                    // Hiển thị lên giao diện ngay lập tức
//                    Glide.with(this)
//                            .load(uri)
//                            .centerCrop()
//                            .into(ivPreview);
//
//                    Toast.makeText(this, "Đã chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                }
            });

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
        String category = "Khác"; // Giá trị mặc định

        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = findViewById(checkedChipId);
            category = selectedChip.getText().toString();
        }else {
            // Nếu bạn muốn ép người dùng phải chọn danh mục
            Toast.makeText(this, "Hãy chọn một danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense expense = new Expense(title, amount, category,
                System.currentTimeMillis(), currentPhotoPath);

        // Lưu vào Database (chạy trên thread riêng)
        new Thread(() -> {
            AppDatabase.getInstance(this).expenseDao().insert(expense);
            runOnUiThread(() -> {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                // Senior Tip: Tạo hiệu ứng đóng màn hình mượt mà
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
            });
        }).start();

    }
}
