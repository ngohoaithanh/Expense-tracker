package com.hoaithanh.expense_tracker.ui.add;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {
    private String currentPhotoPath;
    private ImageView ivPreview;
    private EditText etAmount, etTitle;
    private ChipGroup chipGroup;
    private AppDatabase database;
    private MaterialButtonToggleGroup toggleTransactionType; // KHAI BÁO BIẾN MỚI
    private int currentType = 0;

    // Danh sách danh mục cho Chi tiêu
    private String[] expenseCategories;
    private String[] incomeCategories;
    private List<String> expenseList = new ArrayList<>();
    private List<String> incomeList = new ArrayList<>();

    private String selectedCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);
        database = AppDatabase.getInstance(this);
        ivPreview = findViewById(R.id.ivPreview);
        etAmount = findViewById(R.id.etAmount);
        toggleTransactionType = findViewById(R.id.toggleTransactionType);
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

        expenseCategories = getResources().getStringArray(R.array.expense_categories);
        incomeCategories = getResources().getStringArray(R.array.income_categories);
        loadCategoriesFromDb();

        // 2. Lắng nghe đổi Thu/Chi
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTypeIncome) {
                    currentType = 1;
                    // DÙNG LIST THAY VÌ ARRAY TĨNH
                    populateChips(incomeList.toArray(new String[0]), 1);
                    updateUIForIncome();
                } else {
                    currentType = 0;
                    // DÙNG LIST THAY VÌ ARRAY TĨNH
                    populateChips(expenseList.toArray(new String[0]), 0);
                    updateUIForExpense();
                }
            }
        });
        populateChips(expenseList.toArray(new String[0]), 0);


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
            ivPreview.setImageResource(R.drawable.bg_placeholder_expense2);
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

    private void loadCategoriesFromDb() {
        new Thread(() -> {
            // 1. Lấy danh mục Chi tiêu từ DB
            List<Category> dbExpenses = database.categoryDao().getCategoriesByType(0);
            if (dbExpenses.isEmpty()) {
                // Nếu DB trống, nạp mặc định từ strings.xml vào DB và List
                String[] defaults = getResources().getStringArray(R.array.expense_categories);
                for (String s : defaults) {
                    database.categoryDao().insert(new Category(s, 0));
                    expenseList.add(s);
                }
            } else {
                for (Category c : dbExpenses) expenseList.add(c.name);
            }

            // 2. Lấy danh mục Thu nhập từ DB
            List<Category> dbIncomes = database.categoryDao().getCategoriesByType(1);
            if (dbIncomes.isEmpty()) {
                String[] defaults = getResources().getStringArray(R.array.income_categories);
                for (String s : defaults) {
                    database.categoryDao().insert(new Category(s, 1));
                    incomeList.add(s);
                }
            } else {
                for (Category c : dbIncomes) incomeList.add(c.name);
            }

            // 3. Sau khi nạp xong vào List, hiển thị bộ Chip mặc định (Chi tiêu)
            runOnUiThread(() -> populateChips(expenseList.toArray(new String[0]), 0));
        }).start();
    }

    private void populateChips(String[] categories, int type) {
        chipGroup.removeAllViews(); // Xóa sạch để không bị lẫn Thu/Chi

        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Thiết lập màu sắc dựa trên Type
            if (type == 1) { // Thu nhập
                chip.setChipBackgroundColorResource(R.color.income_chip_bg);
                chip.setTextColor(getResources().getColor(R.color.income_text));
            } else { // Chi tiêu
                chip.setChipBackgroundColorResource(R.color.expense_chip_bg);
                chip.setTextColor(getResources().getColor(R.color.expense_text));
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategory = cat;
                }
            });
            chipGroup.addView(chip);
        }

        // Tự động chọn Chip đầu tiên để tránh lỗi chưa chọn danh mục
        if (chipGroup.getChildCount() > 0) {
            ((Chip) chipGroup.getChildAt(0)).setChecked(true);
            selectedCategory = categories[0];
        }

        createAddButtonChip(type);
    }

    private void addPlusButton(int type) {
        Chip addChip = new Chip(this);
        addChip.setText("+");
        addChip.setChipBackgroundColorResource(R.color.secondary_container);
        addChip.setOnClickListener(v -> {
            // Gọi Dialog thêm danh mục ở đây nếu cần
            Toast.makeText(this, "Tính năng thêm danh mục đang phát triển", Toast.LENGTH_SHORT).show();
        });
        chipGroup.addView(addChip);
    }

    // --- CÁC HÀM TINH CHỈNH UI (UX Bonus) ---
    private void updateUIForIncome() {
        // Ví dụ: Đổi màu số tiền thành xanh lá cho tiền vào
        etAmount.setTextColor(Color.GREEN);
        // Sau này chúng ta sẽ đổi danh sách Chip tại đây
    }

    private void updateUIForExpense() {
        // Đổi màu số tiền thành đỏ cho tiền ra
        etAmount.setTextColor(Color.RED);
        // Sau này chúng ta sẽ đổi danh sách Chip tại đây
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

    private void createAddButtonChip(int type) {
        Chip addChip = new Chip(this);
        addChip.setText("Thêm");
        addChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_add));
        addChip.setChipBackgroundColorResource(R.color.secondary_container);

        addChip.setOnClickListener(v -> {
            // Truyền currentType vào đây
            showAddCategoryDialog(type);
        });

        chipGroup.addView(addChip);
    }

    private void showAddCategoryDialog(int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm danh mục mới");
        final EditText input = new EditText(this);
        input.setHint("Ví dụ: 🐱 Thú cưng");
        builder.setView(input);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newCatName = input.getText().toString().trim();
            if (!newCatName.isEmpty()) {
                if (type == 1) incomeList.add(newCatName);
                else expenseList.add(newCatName);

                new Thread(() -> {
                    // LƯU KÈM TYPE VÀO DATABASE
                    database.categoryDao().insert(new Category(newCatName, type));
                }).start();

                // Vẽ lại Chip
                if (type == 1) populateChips(incomeList.toArray(new String[0]), 1);
                else populateChips(expenseList.toArray(new String[0]), 0);
            }
        });
        builder.setNegativeButton("Hủy", null);
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
        if (amountStr.isEmpty()) {
            etAmount.setError("Vui lòng nhập số tiền!");
            return;
        }

        // Xóa tất cả ký tự không phải là số (Regex: [^0-9])
        String cleanAmount = amountStr.replaceAll("[^0-9]", "");
        double amount = Double.parseDouble(cleanAmount);
        String title = etTitle.getText().toString();
        int transactionType = currentType;

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

        Expense newRecord = new Expense(
                title,
                amount,
                System.currentTimeMillis(),
                category,
                currentPhotoPath,
                transactionType // LƯU THU/CHI VÀO DATABASE
        );

        // Lưu vào Database (chạy trên thread riêng)
        new Thread(() -> {
            AppDatabase.getInstance(this).expenseDao().insert(newRecord);
            runOnUiThread(() -> {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                // Senior Tip: Tạo hiệu ứng đóng màn hình mượt mà
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
            });
        }).start();

    }
}
